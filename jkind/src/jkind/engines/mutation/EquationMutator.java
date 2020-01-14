package jkind.engines.mutation;

import java.math.BigDecimal;

import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.IntExpr;
import jkind.lustre.Location;
import jkind.lustre.NamedType;
import jkind.lustre.RealExpr;

public class EquationMutator extends Mutator {
		
	private int arith_value;
	private boolean bool_value;
	
	public EquationMutator(MutationEngine mutE, String name, int val, boolean b) {
		super(mutE, name);
		this.arith_value = val;
		this.bool_value = b;
	}
	
	@Override
	public Equation visit(Equation e) {
		if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location))
			current_mutation.location = e.location;
	
		if (e.location == current_mutation.location) 
			return mutate(e);
		
		return super.visit(e);
	}
	
	private Equation mutate(Equation e) {
		was_mutated = true;
		
		if (mutE.getSpec().typeMap.get(e.lhs.get(0).id) == NamedType.BOOL)
			return new Equation(e.location, e.lhs, new BoolExpr(bool_value));
		
		else if (mutE.getSpec().typeMap.get(e.lhs.get(0).id) == NamedType.REAL)
			return new Equation(e.location, e.lhs, new RealExpr(new BigDecimal(arith_value)));
		
		else if (mutE.getSpec().typeMap.get(e.lhs.get(0).id) == NamedType.INT)
			return new Equation(e.location, e.lhs, new IntExpr(arith_value));
		
		was_mutated = false;
		current_mutation.location = Location.NULL;
		return super.visit(e);
	}
	
}
