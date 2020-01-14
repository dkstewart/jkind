package jkind.engines.mutation;

import jkind.lustre.Expr;
import jkind.lustre.Location;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public class UnaryOpMutator extends Mutator {
		
	private UnaryOp origOp;
	
	public UnaryOpMutator(MutationEngine mutE, String name, UnaryOp uOp) {
		super(mutE, name);
		this.origOp = uOp;
	}
	
	@Override
	public Expr visit(UnaryExpr e) {
		if (origOp == e.op) {
			if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location))
					current_mutation.location = e.location;
		
			if (e.location == current_mutation.location) 
				return mutate(e);
		}	
		
		return super.visit(e);
	}
	
	private Expr mutate(UnaryExpr e) {
		was_mutated = true;
		
		return e.expr.accept(this);
	}
	
}
