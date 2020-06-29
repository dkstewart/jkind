package jkind.util;

import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import jkind.engines.mutation.Mutation;
import jkind.lustre.Equation;
import jkind.lustre.IdExpr;
import jkind.lustre.Location;
import jkind.lustre.Node;
import jkind.lustre.Program;

public class GuaranteeMutationExtractor {
	Program program;
	SortedMap<Location, List<Mutation>> location_mutations;
	HashMap<Equation, Mutation> guaranteeMutationMap = new HashMap<>();

	public GuaranteeMutationExtractor(Program program, SortedMap<Location, List<Mutation>> location_mutations) {
		this.program = program;
		this.location_mutations = location_mutations;
	}

	public void setGuaranteeMutationMap() {
		for (Node n : program.nodes) {
			for (Equation eq : n.equations) {
				for (IdExpr id : eq.lhs) {
					if (id.id.contains("GUARANTEE")) {
						if (location_mutations.containsKey(eq.location)
								&& !(location_mutations.get(eq.location).isEmpty())) {
							guaranteeMutationMap.put(eq, location_mutations.get(eq.location).get(0));
							break;
						}
					}
				}
			}
		}
	}

	public HashMap<Equation, Mutation> getGuaranteeMutationMap() {
		return guaranteeMutationMap;
	}
}
