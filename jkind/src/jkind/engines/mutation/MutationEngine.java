package jkind.engines.mutation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jkind.JKindSettings;
import jkind.engines.Director;
import jkind.engines.Engine;
import jkind.engines.messages.BaseStepMessage;
import jkind.engines.messages.EngineType;
import jkind.engines.messages.GuaranteeMutationMessage;
import jkind.engines.messages.InductiveCounterexampleMessage;
import jkind.engines.messages.InvalidMessage;
import jkind.engines.messages.InvariantMessage;
import jkind.engines.messages.Itinerary;
import jkind.engines.messages.MutationMessage;
import jkind.engines.messages.NodeInputMutationMessage;
import jkind.engines.messages.UnknownMessage;
import jkind.engines.messages.ValidMessage;
import jkind.lustre.Expr;
import jkind.lustre.Location;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.builders.ProgramBuilder;
import jkind.sexp.Symbol;
import jkind.translation.InlineSimpleEquations;
import jkind.translation.Specification;
import jkind.util.GuaranteeMutationExtractor;
import jkind.util.NodeInputMutationExtractor;

public class MutationEngine extends Engine {
	public static final String NAME = "mutation";

	private final List<ValidMessage> validMessages;
	private final Map<String, Integer> validProperty_k;
	private final Map<Expr, Integer> invariant_k;
	private final Set<String> ivc_ids;
	private final Program program;

	private long startTime;

	private final Set<Mutator> mutators;

 	private final List<Thread> threads;
 	private final List<MutationSubEngine> subEngines;
 	private ConcurrentLinkedQueue<List<Mutation>> mutations_tasks;

	private static Specification inlineSliceBeforeMutation(Specification spec, JKindSettings settings) {
		if(!settings.inlining) {
			return spec;
		}

		Node inlined = InlineSimpleEquations.node(spec.node);
		Program program = new ProgramBuilder().addFunctions(spec.functions).addConstants(spec.constants).addNode(inlined).build();
		return new Specification(program, true);
	}

	private Set<Mutator> defineMutators() {
		Set<Mutator> mutators = new HashSet<>();

		// binary boolean mutators
//		mutators.add(new BinaryOpMutator(this, "or2xor", BinaryOp.OR, BinaryOp.XOR));
//		mutators.add(new BinaryOpMutator(this, "xor2implies", BinaryOp.XOR, BinaryOp.IMPLIES));
//		mutators.add(new BinaryOpMutator(this, "implies2and", BinaryOp.IMPLIES, BinaryOp.AND));
//		mutators.add(new BinaryOpMutator(this, "and2or", BinaryOp.AND, BinaryOp.OR));
//
//		mutators.add(new ReduceBinaryOpMutator(this, "or2left", BinaryOp.OR, Side.Left));
//		mutators.add(new ReduceBinaryOpMutator(this, "or2right", BinaryOp.OR, Side.Right));
//		mutators.add(new ReduceBinaryOpMutator(this, "and2left", BinaryOp.AND, Side.Left));
//		mutators.add(new ReduceBinaryOpMutator(this, "and2right", BinaryOp.AND, Side.Right));
//
//		// binary equality mutators
//		mutators.add(new BinaryOpMutator(this, "eq2neq", BinaryOp.EQUAL, BinaryOp.NOTEQUAL));
//		mutators.add(new BinaryOpMutator(this, "neq2eq", BinaryOp.NOTEQUAL, BinaryOp.EQUAL));
//
//		// binary relational mutators
//		mutators.add(new BinaryOpMutator(this, "g2ge", BinaryOp.GREATER, BinaryOp.GREATEREQUAL));
//		mutators.add(new BinaryOpMutator(this, "ge2g", BinaryOp.GREATEREQUAL, BinaryOp.GREATER));
//		mutators.add(new BinaryOpMutator(this, "l2le", BinaryOp.LESS, BinaryOp.LESSEQUAL));
//		mutators.add(new BinaryOpMutator(this, "le2l", BinaryOp.LESSEQUAL, BinaryOp.LESS));
//		mutators.add(new BinaryOpMutator(this, "g2l", BinaryOp.GREATER, BinaryOp.LESS));
//		mutators.add(new BinaryOpMutator(this, "l2g", BinaryOp.LESS, BinaryOp.GREATER));
//		mutators.add(new BinaryOpMutator(this, "ge2le", BinaryOp.GREATEREQUAL, BinaryOp.LESSEQUAL));
//		mutators.add(new BinaryOpMutator(this, "le2ge", BinaryOp.LESSEQUAL, BinaryOp.GREATEREQUAL));
//
//		// binary linear arith mutators
//		mutators.add(new BinaryOpMutator(this, "plus2minus", BinaryOp.PLUS, BinaryOp.MINUS));
//		mutators.add(new BinaryOpMutator(this, "minus2plus", BinaryOp.MINUS, BinaryOp.PLUS));
//
//		// unary mutators
//		mutators.add(new UnaryOpMutator(this, "rm_minus", UnaryOp.NEGATIVE));
//		mutators.add(new UnaryOpMutator(this, "rm_not", UnaryOp.NOT));
//
//		// if then else mutator
//		mutators.add(new IfThenElseMutator(this, "ifthen", IfThenElseMutation.IFTHEN));
//		mutators.add(new IfThenElseMutator(this, "ifelse", IfThenElseMutation.IFELSE));
//		mutators.add(new IfThenElseMutator(this, "ifelsethen", IfThenElseMutation.IFELSETHEN));
//
//		// constant mutator
//		mutators.add(new ConstantMutator(this));

//		// equation remover
//		mutators.add(new EquationRemover(this, "eq_remove"));
//		mutators.add(new EquationRemover(this, "eq_remove", program));

//		// OLD equation init mutator
//		mutators.add(new InitMutator(this, "init_5_false", 5, false));
//		mutators.add(new InitMutator(this, "init_-1_true", -1, true));
//		// old equation mutator
//		mutators.add(new EquationMutator(this, "eq_5_false", 5, false));
//		mutators.add(new EquationMutator(this, "eq_-2_true", -2, true));

		return mutators;
	}

	public ConcurrentLinkedQueue<List<Mutation>> processMutations(int nb_tasks, Specification spec, Set<Mutator> mutators2){

		SortedMap<Location, List<Mutation>> location_mutations = new TreeMap<>();

		for (Mutator mutator : mutators) {
			Mutator.compute_mutations(mutator);
			for (Location loc : mutator.location_mutation.keySet()) {
				if (! location_mutations.containsKey(loc)) {
					location_mutations.put(loc, new ArrayList<Mutation>());
				}
				location_mutations.get(loc).add(mutator.location_mutation.get(loc));
			}
		}

		return new ConcurrentLinkedQueue<>(location_mutations.values());

	}

	public MutationEngine(Specification spec, JKindSettings settings, Director director, Program program) {
		super(NAME, inlineSliceBeforeMutation(spec, settings), settings, director);

		validMessages = new ArrayList<>();
		validProperty_k = new HashMap<>();
		invariant_k = new HashMap<>();
		ivc_ids = new HashSet<>();
		this.program = program;

		// compute mutations : each location has a list of possible mutations coming from different mutators
		mutators = defineMutators();
		threads = new ArrayList<>();
		subEngines = new ArrayList<>();

		if (!settings.ivcMutation) {
			mutations_tasks = processMutations(settings.parallelMutants, this.spec, mutators);
		}

	}



	@Override
	public void main() {

		processMessagesAndWaitUntil(() -> properties.isEmpty()); // ATTENTION : IL FAUT GARDER IVC activée

		if(validMessages.isEmpty()) {
			return;
		}

		if (settings.ivcMutation) {
			mutations_tasks = processMutations(settings.parallelMutants, this.spec, mutators);
		}

		startTime = System.currentTimeMillis();

		distributeMutations();

		while (threads.stream().anyMatch(Thread::isAlive)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		exitMutation();
	}

	private void distributeMutations() {

		for (int i = 1; i <= settings.parallelMutants; i++) {
			MutationSubEngine sub = new MutationSubEngine(name+i, spec, settings, director, validProperty_k, invariant_k, mutations_tasks);
			subEngines.add(sub);
			threads.add(new Thread(sub));
		}

		threads.forEach(Thread::start);

	}

	protected void exitMutation() {
		SortedMap<Location, List<Mutation>> location_mutations = new TreeMap<>();
		for (MutationSubEngine sub : subEngines) {
			location_mutations.putAll(sub.task_computed_mutations);
		}

		// Create NodeInputMutationExtractor object and get node inputs
		NodeInputMutationExtractor nodeExtr = new NodeInputMutationExtractor(program, location_mutations);
		nodeExtr.setNodeInputMutationMap();
		// Create GuaranteeMutationExtractor object and get guarantees
		GuaranteeMutationExtractor guarExtr = new GuaranteeMutationExtractor(program, location_mutations);
		guarExtr.setGuaranteeMutationMap();

		for (ValidMessage vm : validMessages) {
			Itinerary itinerary = vm.getNextItinerary();
			director.broadcast(new ValidMessage(vm.source, vm.valid, vm.k, vm.invariants, vm.ivc, itinerary));
		}
		director.broadcast(new MutationMessage(location_mutations, startTime));
		director.broadcast(new NodeInputMutationMessage(nodeExtr.getNodeInputMutationMap()));
		director.broadcast(new GuaranteeMutationMessage(guarExtr.getGuaranteeMutationMap()));
	}

	@Override
	protected void handleMessage(BaseStepMessage bsm) {
	}

	@Override
	protected void handleMessage(InductiveCounterexampleMessage icm) {
	}

	@Override
	protected void handleMessage(InvalidMessage im) {
		properties.removeAll(im.invalid);
	}

	@Override
	protected void handleMessage(InvariantMessage im) {
		if (!settings.ivcMutation) {
			for (Expr inv : im.invariants) {
				if (!invariant_k.keySet().stream().anyMatch(e -> e.toString().equals(inv.toString()))) {
					invariant_k.put(inv, im.k);
				}
			}
		}
	}

	@Override
	protected void handleMessage(UnknownMessage um) {
		properties.removeAll(um.unknown);
	}

	@Override
	protected void handleMessage(ValidMessage vm) {
		if (vm.getNextDestination() == EngineType.MUTATION) {
			validMessages.add(vm);

			if (settings.ivcMutation) {
				for (Expr inv : vm.invariants) {
					if (!vm.valid.contains(inv.toString()) &&
							!invariant_k.keySet().stream().anyMatch(e -> e.toString().equals(inv.toString()))) {
						invariant_k.put(inv, vm.k);
					}
				}
				ivc_ids.addAll(vm.ivc);
			}

			for (String prop : vm.valid) {
				validProperty_k.put(prop, vm.k);
			}

			properties.removeAll(vm.valid);
		}
	}

	@Override
	protected void handleMessage(MutationMessage vm) {
	}

	public static <T> Map<T, Symbol> createMutationMap(String label, Collection<T> l) {
		Map<T, Symbol> mutationMap = new HashMap<>();
		int id = 0;
		for (T s : l) {
			mutationMap.put(s, new Symbol(label + id++));
		}
		return mutationMap;
	}

	Specification getSpec() {
		return spec;
	}

	JKindSettings getSettings() {
		return settings;
	}

	static public String trimInstance(String arg) {
		String res = new String(arg);
		return res.replaceAll("~[0-9]+", "");
	}

	public boolean inIVCs(String id) {
		String id_trim = new String(id).replaceAll("~[0-9]+", "");
		return ivc_ids.contains(id_trim);
	}

	@Override
	protected void handleMessage(NodeInputMutationMessage vm) {
	}

	@Override
	protected void handleMessage(GuaranteeMutationMessage vm) {
	}

}
