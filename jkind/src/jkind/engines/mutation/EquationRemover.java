package jkind.engines.mutation;

import jkind.lustre.Equation;
import jkind.lustre.Location;
import jkind.lustre.Program;

public class EquationRemover extends Mutator {

	public EquationRemover(MutationEngine mutE, String name) {
		super(mutE, name);
	}

	public EquationRemover(MutationEngine mutE, String name, Program program) {
		super(mutE, name, program);
	}

	@Override
	public Equation visit(Equation e) {
		if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location)) {
			current_mutation.location = e.location;
		}

		if (e.location == current_mutation.location) {
			return mutate(e);
		}

		return super.visit(e);
	}

	private Equation mutate(Equation e) {
		was_mutated = true;
		return new Equation(e.location, e.lhs.get(0), e.lhs.get(0));  // a valid Equation
	}

}
