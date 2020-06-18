package jkind.translation;

import java.util.List;
import java.util.Map;

import jkind.lustre.Ast;
import jkind.lustre.Constant;
import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.sexp.Symbol;
import jkind.slicing.DependencyMap;
import jkind.util.Util;

public class Specification {
	public final Node node;
	public final List<Function> functions;
	public final DependencyMap dependencyMap;
	public final Map<String, Type> typeMap;
	private Relation transitionRelation;
	private Relation ivcTransitionRelation;
	public final List<Constant> constants; // Storing the constants with their original types

	public Specification(Program program, boolean slicing) {
		Node main = program.getMainNode();
		if (slicing) {
			this.dependencyMap = new DependencyMap(main, main.properties, program.functions);
		} else {
			this.dependencyMap = DependencyMap.full(main, program.functions);
		}
		this.node = main;// LustreSlicer.slice(main, dependencyMap);
		this.functions = Util.safeList(program.functions);
		this.typeMap = Util.getTypeMap(node);
		this.constants = program.constants; // Getting the constants
	}

	public Specification(Program program) {
		this(program, false);
	}

	public Relation getTransitionRelation() {
		if (transitionRelation == null) {
			transitionRelation = Lustre2Sexp.constructTransitionRelation(node);
		}
		return transitionRelation;
	}

	public Relation getIvcTransitionRelation(Map<String, Symbol> ivcMap) {
		if (ivcTransitionRelation == null) {
			ivcTransitionRelation = Lustre2Sexp.constructIvcTransitionRelation(node, ivcMap);
		}
		return ivcTransitionRelation;
	}

	public Relation getMutationTransitionRelation(String str, Map<Ast, Symbol> mutationMap) {
		return Lustre2Sexp.constructMutationTransitionRelation(str, node, mutationMap);
	}

}
