package jkind.util;

import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import jkind.engines.mutation.Mutation;
import jkind.lustre.Expr;
import jkind.lustre.Location;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;

public class GuaranteeMutationExtractor {
	Program program;
	SortedMap<Location, List<Mutation>> location_mutations;
	HashMap<Expr, Mutation> guaranteeMutationMap = new HashMap<>();

	public GuaranteeMutationExtractor(Program program, SortedMap<Location, List<Mutation>> location_mutations) {
		this.program = program;
		this.location_mutations = location_mutations;
	}

	public void setGuaranteeMutationMap() {
		Node main = null;
		for (Node n : program.nodes) {
			if (n.id.contentEquals("main")) {
				main = n;
				break;
			}
		}
		if (main != null) {
			for (Expr ex : main.assertions) {
				if (ex instanceof NodeCallExpr) {
					NodeCallExpr nodeCall = (NodeCallExpr) ex;
					for (Expr arg : nodeCall.args) {
						if (location_mutations.containsKey(arg.location)
								&& !(location_mutations.get(arg.location).isEmpty())) {
							guaranteeMutationMap.put(arg, location_mutations.get(arg.location).get(0));
						}
					}
				}
			}
		}
	}

	public HashMap<Expr, Mutation> getGuaranteeMutationMap() {
		return guaranteeMutationMap;
	}
}
