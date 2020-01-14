package jkind.engines.mutation;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.Location;

public class ReduceBinaryOpMutator extends Mutator {
	
	protected enum Side {Left, Right};
	
	private BinaryOp op;
	private Side side;
	
	public ReduceBinaryOpMutator(MutationEngine mutE, String name, BinaryOp o, Side s) {
		super(mutE, name);
		this.op = o;
		this.side = s;
	}
	
	@Override
	public Expr visit(BinaryExpr e) {
		if (op == e.op) {
			if (current_mutation.location == Location.NULL && !location_mutation.containsKey(e.location))
					current_mutation.location = e.location;
		
			if (e.location == current_mutation.location) 
				return mutate(e);
		}	
		
		return super.visit(e);
	}
	
	private Expr mutate(BinaryExpr e) {
		was_mutated = true;
		
		if (side == Side.Left)
			return e.left.accept(this);
		else 
			return e.right.accept(this);
	}
	
}
