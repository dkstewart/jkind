package jkind.util;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

public class Granularity {
	private Program program;
	private List<Equation> freshVars = new ArrayList<Equation>();
	private List<Equation> removeEqs = new ArrayList<Equation>();
	private List<Equation> newEquations = new ArrayList<Equation>();
	private static int unique = 0;

	public Granularity(Program program) {
		this.program = program;

	}

	public Program decomposeProgram() {
		for (Node n : program.nodes) {
			for (Equation e : n.equations) {
				// check lhs of equation for local vars with bool type
				if (okayType(n, e)) {
					removeEqs.add(e);
					createFreshVars(e);
				}
			}
			addEquations(n);
			addIVCs(n);
			addLocals(n);
			resetGlobals();
		}
		return program;
	}

	private void createFreshVars(Equation e) {
		// Init process
		if (e.expr instanceof BinaryExpr) {
			BinaryExpr binEx = (BinaryExpr) e.expr;
			if (isSafeBoolBinaryOp(binEx.op)) {
				IdExpr id = new IdExpr("FRESHVAR" + unique);
				unique++;
				newEquations.add(new Equation(e.lhs, id));
				removeEqs.add(e);
				unInline(binEx, id);
			}
		} else if (e.expr instanceof UnaryExpr) {
			UnaryExpr unEx = (UnaryExpr) e.expr;
			if (isSafeBoolUnaryOp(unEx.op)) {
				IdExpr id = new IdExpr("FRESHVAR" + unique);
				unique++;
				newEquations.add(new Equation(e.lhs, id));
				removeEqs.add(e);
//				freshVars.add(new Equation(id, new UnaryExpr(unEx.op, unInline(unEx.expr, ))));
				unInline(unEx, id);
			}
		}

	}

	private void unInline(Expr ex, IdExpr fresh) {
		// // We know expr is binary or unary with safe bool operator
		// and originalEquation = fresh, but fresh is not assigned yet.
		Expr finalLeft;
		Expr finalRight;
		Expr finalUnary;
		// Binary case
		if (ex instanceof BinaryExpr) {
			BinaryExpr expr = (BinaryExpr) ex;
			// Check left for binary/unary case
			if (expr.left instanceof BinaryExpr) {
				finalLeft = unInlineBinary((BinaryExpr) expr.left);
			} else if (expr.left instanceof UnaryExpr) {
				finalLeft = unInlineUnary((UnaryExpr) expr.left);
			} else {
				finalLeft = expr.left;
			}
			// Check right for binary/unary case
			if (expr.right instanceof BinaryExpr) {
				finalRight = unInlineBinary((BinaryExpr) expr.right);
			} else if (expr.right instanceof UnaryExpr) {
				finalRight = unInlineUnary((UnaryExpr) expr.right);
			} else {
				finalRight = expr.right;
			}
			// Now, finalLeft and finalRight should be assigned to 'fresh'
			// and given the top level operator
			freshVars.add(new Equation(fresh, new BinaryExpr(finalLeft, expr.op, finalRight)));
		} else if (ex instanceof UnaryExpr) {
			UnaryExpr unExpr = (UnaryExpr) ex;
			finalUnary = unInlineUnary(unExpr);
			freshVars.add(new Equation(fresh, finalUnary));
		}


	}

	private Expr unInlineBinary(BinaryExpr binEx) {
		Expr finalExpr = null;
		if (isSafeBoolBinaryOp(binEx.op)) {
			IdExpr idR = new IdExpr("FRESHVAR" + unique);
			unique++;
			finalExpr = idR;
			unInline(binEx, idR);
		} else {
			finalExpr = binEx;
		}
		return finalExpr;
	}

	private Expr unInlineUnary(UnaryExpr unEx) {
		Expr finalExpr = null;
		if (isSafeBoolUnaryOp(unEx.op)) {
			if ((unEx.expr instanceof UnaryExpr) || (unEx.expr instanceof BinaryExpr)) {
				IdExpr idUnary = new IdExpr("FRESHVAR" + unique);
				unique++;
				finalExpr = new UnaryExpr(unEx.op, idUnary);
				unInline(unEx.expr, idUnary);
			} else {
				finalExpr = unEx;
			}
		} else {
			finalExpr = unEx;
		}

		return finalExpr;
	}

	private List<Equation> deepCopyWithRemoval(List<Equation> equations, List<Equation> removal) {
		List<Equation> newList = new ArrayList<Equation>();
		for (Equation e : equations) {
			if (!(removal.contains(e))) {
				newList.add(e);
			}
		}
		return newList;
	}

	private static boolean isSafeBoolBinaryOp(BinaryOp op) {
		if (op.equals(BinaryOp.EQUAL) || op.equals(BinaryOp.NOTEQUAL) || op.equals(BinaryOp.GREATER)
				|| op.equals(BinaryOp.LESS) || op.equals(BinaryOp.GREATEREQUAL) || op.equals(BinaryOp.LESSEQUAL)
				|| op.equals(BinaryOp.OR) || op.equals(BinaryOp.AND) || op.equals(BinaryOp.XOR)
				|| op.equals(BinaryOp.IMPLIES) || op.equals(BinaryOp.ARROW) || op.equals(UnaryOp.NOT)
				|| op.equals(UnaryOp.PRE)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isSafeBoolUnaryOp(UnaryOp op) {
		if (op.equals(UnaryOp.NOT)) {
			return true;
		} else {
			return false;
		}
	}

	private void addIVCs(Node n) {
		List<String> newIVCs = new ArrayList<String>();
//		for (String s : n.ivc) {
//			newIVCs.add(s);
//		}
		for (Equation id : freshVars) {
			newIVCs.add(id.lhs.get(0).toString());
		}
		n.resetIVC(newIVCs);
	}

	private void addEquations(Node n) {
		List<Equation> newEqs = new ArrayList<Equation>();
		for (Equation e : n.equations) {
			if (!removeEqs.contains(e)) {
				newEqs.add(e);
			}
		}
		newEqs.addAll(newEquations);
		newEqs.addAll(freshVars);
		n.resetEquation(newEqs);
	}

	private void addLocals(Node n) {
		List<VarDecl> newLocals = new ArrayList<VarDecl>();
		for (VarDecl var : n.locals) {
			newLocals.add(var);
		}
		for (Equation f : freshVars) {
			VarDecl v = new VarDecl(f.lhs.get(0).toString(), NamedType.BOOL);
			newLocals.add(v);
		}
		n.resetLocals(newLocals);
	}

	private boolean okayType(Node n, Equation e) {
		boolean isBoolean = false;
		for (VarDecl vd : n.locals) {
			if (vd.id.equals(e.lhs.get(0).toString())) {
				if (vd.type.toString().equals("bool")) {
					return true;
				}
			}
		}

		return isBoolean;
	}

	private void resetGlobals() {
		unique = 0;
		newEquations.clear();
		removeEqs.clear();
		freshVars.clear();
	}
}
