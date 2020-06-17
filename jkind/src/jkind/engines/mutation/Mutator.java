package jkind.engines.mutation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Location;
import jkind.lustre.Program;
import jkind.lustre.visitors.AstMapVisitor;

public class Mutator extends AstMapVisitor {
	protected String name;
	protected Program program;
	protected Mutation current_mutation;
	protected boolean was_mutated;
	protected MutationEngine mutE;

	public Map<Location, Mutation> location_mutation;

	public Mutator(MutationEngine mutE, String name, Program program) {
		super();
		this.name = name;
		this.current_mutation = null;
		this.was_mutated = false;
		this.location_mutation = new HashMap<>();
		this.mutE = mutE;
		this.program = program;
	}

	public Mutator(MutationEngine mutE, String name) {
		super();
		this.name = name;
		this.current_mutation = null;
		this.was_mutated = false;
		this.location_mutation = new HashMap<>();
		this.mutE = mutE;
	}

	public String getName() {
		return name;
	}

	public static void compute_mutations (Mutator mutator) {

		// keep looping until no more mutations are found
		while(true){

			mutator.current_mutation = new Mutation(Location.NULL, mutator, new HashMap<>());

			mutator.visitEquations(mutator.mutE.getSpec().node.equations);
			mutator.visitAssertions(mutator.mutE.getSpec().node.assertions);

			if (mutator.current_mutation.location == Location.NULL) {
				break;
			}

			mutator.location_mutation.put(mutator.current_mutation.location, mutator.current_mutation);
		}
	}

	@Override
	protected List<Equation> visitEquations(List<Equation> es) {
		for (Equation e : es) {
			if (current_mutation.location == Location.NULL && mutE.getSpec().node.properties.contains(e.lhs.get(0).id)) {
				continue;
			}

			if (mutE.getSettings().ivcMutation && current_mutation.location == Location.NULL && !mutE.inIVCs(e.lhs.get(0).id)) {
				continue;
			}

			was_mutated = false;
			Equation em = visit(e);
			if (was_mutated) {
				current_mutation.mutationMap.put(e, em);
			}
		}

		return null;
	}

	@Override
	protected List<Expr> visitAssertions(List<Expr> es) {
		for (Expr e : es) {
			was_mutated = false;
			Expr em = e.accept(this);
			if (was_mutated) {
				current_mutation.mutationMap.put(e, em);
			}
		}

		return null;
	}
}
