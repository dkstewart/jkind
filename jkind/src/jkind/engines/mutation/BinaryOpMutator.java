package jkind.engines.mutation;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.Location;

public class BinaryOpMutator extends Mutator {
		
	private BinaryOp origOp, newOp;
	
	public BinaryOpMutator(MutationEngine mutE, String name, BinaryOp oOp, BinaryOp nOp) {
		super(mutE, name);
		this.origOp = oOp;
		this.newOp = nOp;
	}
	
	@Override
	public Expr visit(BinaryExpr e) {
		if (origOp == e.op) {
			if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location))
					current_mutation.location = e.location;
		
			if (e.location == current_mutation.location) 
				return mutate(e);
		}	
		
		return super.visit(e);
	}
	
	private Expr mutate(BinaryExpr e) {
		was_mutated = true;
		
		Expr left = e.left.accept(this);
		Expr right = e.right.accept(this);
		
		return new BinaryExpr(e.location, left, newOp, right);
	}
	
}
