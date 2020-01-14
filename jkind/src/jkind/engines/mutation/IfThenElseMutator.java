package jkind.engines.mutation;

import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Location;

public class IfThenElseMutator extends Mutator {
	
	public enum IfThenElseMutation {IFTHEN, IFELSE, IFELSETHEN};
	
	private IfThenElseMutation ite_m;
	
	public IfThenElseMutator(MutationEngine mutE, String name, IfThenElseMutation ite_m) {
		super(mutE, name);
		this.ite_m = ite_m;
	}
	
	@Override
	public Expr visit(IfThenElseExpr e) {
		if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location))
				current_mutation.location = e.location;
	
		if (e.location == current_mutation.location) 
			return mutate(e);
		
		return super.visit(e);
	}
	
	private Expr mutate(IfThenElseExpr e) {
		was_mutated = true;
		
		Expr cond = e.cond.accept(this);
		Expr thenExpr = e.thenExpr.accept(this);
		Expr elseExpr = e.elseExpr.accept(this);
		
		switch (ite_m) {
			case IFELSETHEN:
				return new IfThenElseExpr(e.location, cond, elseExpr, thenExpr);
			case IFELSE:
				return new IfThenElseExpr(e.location, new BoolExpr(false), thenExpr, elseExpr);
			case IFTHEN:
				return new IfThenElseExpr(e.location, new BoolExpr(true), thenExpr, elseExpr);
			default:
				return new IfThenElseExpr(e.location, cond, thenExpr, elseExpr);
		}
	}
	
}
