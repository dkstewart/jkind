package jkind.translation.tuples;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Location;
import jkind.lustre.TupleExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.translation.compound.CompoundUtil;

public class TupleUtil {
	public static TupleExpr mapBinary(Location loc, BinaryOp op, TupleExpr expr1, TupleExpr expr2) {
		return new TupleExpr(CompoundUtil.mapBinary(loc, op, expr1.elements, expr2.elements));
	}

	public static TupleExpr mapIf(Location loc, Expr cond, TupleExpr expr1, TupleExpr expr2) {
		return new TupleExpr(mapIf(loc, cond, expr1.elements, expr2.elements));
	}

	private static List<Expr> mapIf(Location loc, Expr cond, List<Expr> exprs1, List<Expr> exprs2) {
		List<Expr> result = new ArrayList<>();
		for (int i = 0; i < exprs1.size(); i++) {
			result.add(new IfThenElseExpr(loc, cond, exprs1.get(i), exprs2.get(i)));
		}
		return result;
	}

	public static TupleExpr mapUnary(Location loc, UnaryOp op, TupleExpr expr) {
		return new TupleExpr(mapUnary(loc, op, expr.elements));
	}

	public static List<Expr> mapUnary(Location loc, UnaryOp op, List<Expr> exprs) {
		List<Expr> result = new ArrayList<>();
		for (int i = 0; i < exprs.size(); i++) {
			result.add(new UnaryExpr(loc, op, exprs.get(i)));
		}
		return result;
	}
}
