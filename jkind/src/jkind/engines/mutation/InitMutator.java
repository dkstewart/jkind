package jkind.engines.mutation;

import java.math.BigDecimal;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IntExpr;
import jkind.lustre.Location;
import jkind.lustre.NamedType;
import jkind.lustre.RealExpr;

public class InitMutator extends Mutator {
		
	private int arith_value;
	private boolean bool_value;
	
	public InitMutator(MutationEngine mutE, String name, int val, boolean b) {
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
		
		if (mutE.getSpec().typeMap.get(e.lhs.get(0).id) == NamedType.BOOL) {
			Expr expr = e.expr.accept(this);
			Expr pre_expr = new BinaryExpr(new BoolExpr(bool_value), BinaryOp.ARROW, expr);
			return new Equation(e.location, e.lhs, pre_expr);
		}
		
		else if (mutE.getSpec().typeMap.get(e.lhs.get(0).id) == NamedType.REAL) {
			Expr expr = e.expr.accept(this);
			Expr pre_expr = new BinaryExpr(new RealExpr(new BigDecimal(arith_value)), BinaryOp.ARROW, expr);
			return new Equation(e.location, e.lhs, pre_expr);
		}
		
		else if (mutE.getSpec().typeMap.get(e.lhs.get(0).id) == NamedType.INT) {
			Expr expr = e.expr.accept(this);
			Expr pre_expr = new BinaryExpr(new IntExpr(arith_value), BinaryOp.ARROW, expr);
			return new Equation(e.location, e.lhs, pre_expr);
		}
		
		was_mutated = false;
		current_mutation.location = Location.NULL;
		return super.visit(e);
	}
	
}
