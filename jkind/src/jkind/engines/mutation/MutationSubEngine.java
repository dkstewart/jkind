package jkind.engines.mutation;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jkind.JKindSettings;
import jkind.engines.Director;
import jkind.engines.SolverBasedEngine;
import jkind.engines.messages.BaseStepMessage;
import jkind.engines.messages.InductiveCounterexampleMessage;
import jkind.engines.messages.InvalidMessage;
import jkind.engines.messages.InvariantMessage;
import jkind.engines.messages.MutationMessage;
import jkind.engines.messages.NodeInputMutationMessage;
import jkind.engines.messages.UnknownMessage;
import jkind.engines.messages.ValidMessage;
import jkind.engines.mutation.Mutation.Verdict;
import jkind.lustre.Ast;
import jkind.lustre.Expr;
import jkind.lustre.Location;
import jkind.lustre.NamedType;
import jkind.lustre.VarDecl;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.visitors.Evaluator;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Model;
import jkind.solvers.ModelEvaluator;
import jkind.solvers.Result;
import jkind.solvers.UnsatResult;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Relation;
import jkind.translation.Specification;
import jkind.util.SexpUtil;
import jkind.util.StreamIndex;

public class MutationSubEngine extends SolverBasedEngine {

	private final Map<String, Integer> validProperty_k;
	private final Map<Expr, Integer> invariant_k;

	private final Map<Ast, Symbol> labeling_orig;
	private final Map<Expr, Symbol> labeling_inv;
	private Relation transition_orig, transition_mut;

	ConcurrentLinkedQueue<List<Mutation>> mutations_tasks;
	Map<Location, List<Mutation>> task_computed_mutations;

	public MutationSubEngine(String name, Specification spec, JKindSettings settings, Director director,
			Map<String, Integer> validProperty_k, Map<Expr, Integer> invariant_k, ConcurrentLinkedQueue<List<Mutation>> mutations_tasks) {
		super(name, spec, settings, director);
		this.validProperty_k = Collections.unmodifiableMap(validProperty_k);
		this.invariant_k = Collections.unmodifiableMap(invariant_k);

		List<Ast> all_eqs_asserts = new ArrayList<>();
		all_eqs_asserts.addAll(this.spec.node.equations);
		all_eqs_asserts.addAll(this.spec.node.assertions);
		this.labeling_orig = MutationEngine.createMutationMap("orig", all_eqs_asserts);

		this.labeling_inv = MutationEngine.createMutationMap("inv", invariant_k.keySet());

		this.mutations_tasks = mutations_tasks;
		this.task_computed_mutations = new HashMap<>();
	}

	@Override
	protected void initializeSolver() {
		solver = getSolver();
		solver.initialize();

		solver.declare(spec.functions);

		solver.define(new VarDecl(INIT.str, NamedType.BOOL));

		for (Symbol e : labeling_orig.values()) {
			solver.define(new VarDecl(e.str, NamedType.BOOL));
		}
		transition_orig = spec.getMutationTransitionRelation(Relation.T, labeling_orig);
		solver.define(transition_orig);
	}

	@Override
	public void main() {
		processMutations();
	}

	private void processMutations() {

		int k_max_prop = Collections.max(validProperty_k.values()).intValue();
		int k_max_inv = -1;
		if(!invariant_k.isEmpty()) {
			k_max_inv = Collections.max(invariant_k.values()).intValue();
		}

		for (Symbol e : labeling_inv.values()) {
			solver.define(new VarDecl(e.str, NamedType.BOOL));
		}

		while(true) {

			List<Mutation> mutations = mutations_tasks.poll();
			if (mutations == null) {
				break;
			}
			Location loc = mutations.get(0).location;
			task_computed_mutations.put(loc, mutations);

			comment("Mutations at position " + loc);
			solver.push();

			// send non mutated lines
			List<Symbol> non_mutated = new ArrayList<>(labeling_orig.values());
			for (Ast e : mutations.get(0).mutationMap.keySet()) {
				non_mutated.remove(labeling_orig.get(e));
			}
			solver.assertSexp(SexpUtil.conjoin(non_mutated));

			for (Mutation mut : mutations) {
				comment("Mutation : " + mut.description);
				solver.push();

				Map<Ast, Symbol> labeling_mut = MutationEngine.createMutationMap("mut", mut.mutationMap.values());
				for (Symbol e : labeling_mut.values()) {
					solver.define(new VarDecl(e.str, NamedType.BOOL));
				}
				transition_mut = spec.getMutationTransitionRelation("T_mut", labeling_mut);
				solver.define(transition_mut);
				solver.assertSexp(SexpUtil.conjoin(new ArrayList<>(labeling_mut.values())));

				// only non trivial invariants
				Collection<Expr> inv_candidates = new HashSet<>();
				for (Expr e : labeling_inv.keySet()) {
					if (invariant_k.get(e) >= 1) {
						inv_candidates.add(e);
					}
				}

				solver.push();

				int k_inv = k_max_inv;
				createVariables(-1);
				for (int k = 0; k <= k_max_prop + settings.kIndWidening ; k++) { // more steps than the original induction
					comment("BMC on mutation, K = " + k);
					createVariables(k);
					assertBaseTransition(k);
					if (!baseCheckProperties(k, mut)) {
						comment("BMC K = " + k + ", mutation: " + mut.verdict);
						break;
					}
					assertProperties(k);
					if (k_inv < k) {
						continue;
					}
					if (!baseReduceInvariants(inv_candidates, k)) {
						k_inv = k;
					}
					comment("BMC K = " + k + ", surviving invariants: " +  Arrays.toString(inv_candidates.stream().map(e -> labeling_inv.get(e).str).toArray()));
					assertInvariants(inv_candidates, k);
				}

				solver.pop();

				if(mut.verdict == Verdict.KILLED || mut.verdict == Verdict.UNKNOWN) {
					solver.pop();
					continue;
				}

				solver.push();

				createVariables(-1);
				createVariables(0);
				assertInductiveTransition(0);
				assertInvariants(inv_candidates, 0);
				for (int k = 1; k <= k_inv; k++) {
					comment("INV K-IND on mutation, K = " + k);
					createVariables(k);
					assertInductiveTransition(k);
					inductiveReduceInvariants(inv_candidates, k);
					comment("INV K-IND K = " + k + ", surviving invariants " +  Arrays.toString(inv_candidates.stream().map(e -> labeling_inv.get(e).str).toArray()));
					assertInvariants(inv_candidates, k);
				}

				solver.pop();

				solver.push();

				solver.assertSexp(SexpUtil.conjoin(inv_candidates.stream().map(labeling_inv::get).collect(toList())));

				createVariables(-1);
				for (int k = 0; k <= k_max_prop + settings.kIndWidening; k++) {
					comment("PROP K-IND on mutation, K = " + k);
					createVariables(k);
					assertInductiveTransition(k);
					assertInvariants(inv_candidates, k);
					inductiveCheckProperties(k, mut);
					if (mut.surviving_properties.containsAll(validProperty_k.keySet())) {
						mut.verdict = Verdict.SURVIVED;
						break;
					}
				}

				solver.pop();

				if (mut.verdict == Verdict.UNKNOWN) {

					solver.push();

					solver.assertSexp(SexpUtil.conjoin(inv_candidates.stream().map(labeling_inv::get).collect(toList())));

					createVariables(-1);
					for (int k = 0; k <= settings.kKill ; k++) {
						comment("BMC on UNKNOWN mutation, K = " + k);
						createVariables(k);
						assertBaseTransition(k);
						assertInvariants(inv_candidates, k);
						if (!baseCheckProperties(k, mut)) {
							comment("BMC K = " + k + ", mutation: " + mut.verdict);
							break;
						}
						assertProperties(k);
					}

					solver.pop();
				}

				solver.pop();
			}

			solver.pop();

		}

	}

	private boolean baseCheckProperties(int k, Mutation mut) {
		Sexp query_prop = StreamIndex.conjoinEncodings(new ArrayList<>(validProperty_k.keySet()), k);
		Result result = solver.query(query_prop);

		if (!(result instanceof UnsatResult)) {
			mut.verdict = Verdict.UNKNOWN;

			Model model = getModel(result);
			if (model == null) {
				return false;
			}

			mut.killing_properties = getFalseProperties(new ArrayList<String>(validProperty_k.keySet()), k, model);
			mut.killing_k = k;

			if (!mut.killing_properties.isEmpty()) {
				mut.verdict = Verdict.KILLED;
			}
			return false;
		}

		return true;
	}

	private void inductiveCheckProperties(int k, Mutation mut) {
		List<String> current_k_properties = new ArrayList<>();
		for (String prop : validProperty_k.keySet()) {
			if (validProperty_k.get(prop) <= k && !mut.surviving_properties.contains(prop)) {
				current_k_properties.add(prop);
			}
		}

		if(current_k_properties.isEmpty()) {
			return;
		}

		Result result;

		do {
			solver.push();

			List<Sexp> hyps = new ArrayList<>();
			for (int i = 0; i < k; i++) {
				hyps.add(StreamIndex.conjoinEncodings(current_k_properties, i));
			}
			Sexp conc = StreamIndex.conjoinEncodings(current_k_properties, k);

			result = solver.query(new Cons("=>", SexpUtil.conjoin(hyps), conc));

			if (!(result instanceof UnsatResult)) {
				mut.verdict = Verdict.UNKNOWN;
				Model model = getModel(result);

				if (model == null) {
					current_k_properties.clear();
					solver.pop();
					break;
				}

				List<String> toRemove = getFalseProperties(current_k_properties, k, model);
				if (toRemove.isEmpty()) {
					current_k_properties.clear();
				} else {
					current_k_properties.removeAll(toRemove);
				}
			}

			solver.pop();

		} while (!current_k_properties.isEmpty() && !(result instanceof UnsatResult));

		mut.surviving_properties.addAll(current_k_properties);
	}

	private void assertProperties(int k) {
		for (String prop : validProperty_k.keySet()) {
			solver.assertSexp(new StreamIndex(prop, k).getEncoded());
		}
	}

	private boolean baseReduceInvariants(Collection<Expr> inv_candidates, int k) {

		Result result;

		do {
			solver.push();

			solver.assertSexp(SexpUtil.conjoin(inv_candidates.stream().map(labeling_inv::get).collect(toList())));

			result = solver.query(SexpUtil.conjoinInvariants(inv_candidates, k));

			if (!(result instanceof UnsatResult)) {
				Model model = getModel(result);
				if (model == null) {
					solver.pop();
					return false;
				}
				Evaluator eval = new ModelEvaluator(model, k);
				if (!inv_candidates.removeIf(e -> eval.eval(e) == BooleanValue.FALSE)) {
					solver.pop();
					return false;
				}

			}

			solver.pop();

		} while (!inv_candidates.isEmpty() && !(result instanceof UnsatResult));

		return true;
	}

	private void inductiveReduceInvariants(Collection<Expr> inv_candidates, int k) {

		Set<Expr> current_k_candidates = new HashSet<>();
		for (Expr inv : inv_candidates) {
			if (invariant_k.get(inv) == k) {
				current_k_candidates.add(inv);
			}
		}

		if(current_k_candidates.isEmpty()) {
			return;
		}

		Result result;

		do {
			solver.push();

			solver.assertSexp(SexpUtil.conjoin(current_k_candidates.stream().map(labeling_inv::get).collect(toList())));

			result = solver.query(SexpUtil.conjoinInvariants(current_k_candidates, k));

			if (!(result instanceof UnsatResult)) {
				Model model = getModel(result);
				if (model == null) {
					current_k_candidates.clear();
					solver.pop();
					break;
				}
				Evaluator eval = new ModelEvaluator(model, k);

				if (!current_k_candidates.removeIf(e -> eval.eval(e) == BooleanValue.FALSE)) {
					current_k_candidates.clear();
				}
			}

			solver.pop();

		} while (!current_k_candidates.isEmpty() && !(result instanceof UnsatResult));

		inv_candidates.removeIf(e -> invariant_k.get(e) == k && !current_k_candidates.contains(e));

	}

	private void assertInvariants(Collection<Expr> inv_candidates, int k) {
		for (Expr inv : inv_candidates) {
			Sexp inv_k = inv.accept(new Lustre2Sexp(k));
			solver.assertSexp(new Cons("=>", labeling_inv.get(inv), inv_k));
		}
	}

	@Override
	protected void handleMessage(BaseStepMessage bsm) {
	}

	@Override
	protected void handleMessage(InductiveCounterexampleMessage icm) {
	}

	@Override
	protected void handleMessage(InvalidMessage im) {
	}

	@Override
	protected void handleMessage(InvariantMessage im) {
	}

	@Override
	protected void handleMessage(UnknownMessage um) {
	}

	@Override
	protected void handleMessage(ValidMessage vm) {
	}

	@Override
	protected void handleMessage(MutationMessage vm) {
	}

	@Override
	protected Sexp getTransition(int k, Sexp init) {
		List<Sexp> args = new ArrayList<>();
		args.add(init);
		args.addAll(getSymbols(getOffsetVarDecls(k - 1)));
		args.addAll(getSymbols(getOffsetVarDecls(k)));
		Sexp t_orig = new Cons(transition_orig.getName(), args);
		Sexp t_mut = new Cons(transition_mut.getName(), args);
		return SexpUtil.conjoin(Arrays.asList(t_orig, t_mut));
	}

	@Override
	protected void comment(String str) {
		super.comment(name + " : " + str);
	}

	@Override
	protected void handleMessage(NodeInputMutationMessage vm) {
	}
}
