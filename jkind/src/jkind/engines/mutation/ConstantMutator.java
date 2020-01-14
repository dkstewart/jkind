package jkind.engines.mutation;

import java.math.BigDecimal;
import java.math.BigInteger;

import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IntExpr;
import jkind.lustre.Location;
import jkind.lustre.RealExpr;

public class ConstantMutator extends Mutator {
		
	public ConstantMutator(MutationEngine mutE) {
		super(mutE, "const");
	}
	
	@Override
	public Expr visit(IntExpr e) {
		if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location)) {
			current_mutation.description = "(const int 1 -> -1, x -> 1)";
			current_mutation.location = e.location;
		}
		if (e.location == current_mutation.location) 
			return mutate(e);
		
		return super.visit(e);
	}
	
	private Expr mutate(IntExpr e) {
		was_mutated = true;
		
		if (e.value.intValue() == 1)
			return new IntExpr(e.location, new BigInteger("-1"));
		else 
			return new IntExpr(e.location, new BigInteger("1"));
	}
	
	@Override
	public Expr visit(RealExpr e) {
		if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location)) {
			current_mutation.description = "(const real 1.0 -> -1.0, x -> 1.0)";
			current_mutation.location = e.location;
		}
					
		if (e.location == current_mutation.location) 
			return mutate(e);
		
		return super.visit(e);
	}
	
	private Expr mutate(RealExpr e) {
		was_mutated = true;
		
		if (e.value.doubleValue() == 1.0)
			return new RealExpr(e.location, new BigDecimal(-1.0));
		else 
			return new RealExpr(e.location, new BigDecimal(1.0));
	}
	
	@Override
	public Expr visit(BoolExpr e) {
		if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location)) {
			current_mutation.description = "(const bool true -> false, false -> true)";
			current_mutation.location = e.location;
		}
		
		if (e.location == current_mutation.location) 
			return mutate(e);
		
		return super.visit(e);
	}
	
	private Expr mutate(BoolExpr e) {
		was_mutated = true;
		
		if (e.value)
			return new BoolExpr(e.location, false);
		else 
			return new BoolExpr(e.location, true);
	}
	
}
