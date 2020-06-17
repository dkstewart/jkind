package jkind.writers;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.JKindSettings;
import jkind.engines.mutation.Mutation;
import jkind.engines.mutation.Mutation.Verdict;
import jkind.lustre.Expr;
import jkind.lustre.Location;
import jkind.results.Counterexample;
import jkind.results.layout.Layout;
import jkind.util.Util;

public class ConsoleWriter extends Writer {
	private final Layout layout;

	public ConsoleWriter(Layout layout) {
		super();
		this.layout = layout;
	}

	@Override
	public void begin() {
	}

	@Override
	public void end() {
	}

	private void writeLine() {
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}

	@Override
	public void writeValid(List<String> props, String source, int k, double runtime,
			List<Expr> invariants, Set<String> ivc) {
		writeLine();
		System.out.println("VALID PROPERTIES: " + props + " || " + source + " || K = " + k
				+ " || Time = " + Util.secondsToTime(runtime));
		if (!invariants.isEmpty()) {
			System.out.println("INVARIANTS:");
			List<String> stringInvariants = invariants.stream().map(Object::toString).collect(toList());
			for (String invariant : Util.safeStringSortedSet(stringInvariants)) {
				System.out.println("  " + invariant);
			}
		}
		if (!ivc.isEmpty()) {
			System.out.println("INDUCTIVE VALIDITY CORE:");
			for (String e : Util.safeStringSortedSet(ivc)) {
				System.out.println("  " + e);
			}
		}
		writeLine();
		System.out.println();
	}

	@Override
	public void writeInvalid(String prop, String source, Counterexample cex,
			List<String> conflicts, double runtime) {
		writeLine();
		System.out.println("INVALID PROPERTY: " + prop + " || " + source + " || K = "
				+ cex.getLength() + " || Time = " + Util.secondsToTime(runtime));
		System.out.println(cex.toString(layout));
		writeLine();
		System.out.println();
	}

	@Override
	public void writeUnknown(List<String> props, int trueFor,
			Map<String, Counterexample> inductiveCounterexamples, double runtime) {
		writeLine();
		System.out.println("UNKNOWN PROPERTIES: " + props + " || True for " + trueFor + " steps"
				+ " || Time = " + Util.secondsToTime(runtime));
		writeLine();
		System.out.println();
		for (String prop : props) {
			Counterexample cex = inductiveCounterexamples.get(prop);
			if (cex != null) {
				writeLine();
				System.out.println("INDUCTIVE COUNTEREXAMPLE: " + prop + " || K = "
						+ cex.getLength());
				System.out.println(cex.toString(layout));
				writeLine();
				System.out.println();
			}
		}
	}

	@Override
	public void writeBaseStep(List<String> props, int k) {
	}

	@Override
	public void writeInconsistent(String prop, String source, int k, double runtime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeMutation(Map<Location, List<Mutation>> location_mutations, double startTime, JKindSettings settings) {
		writeLine();

		int surv = 0, kill = 0, unkn = 0;

		for (Location loc : location_mutations.keySet()) {
			for (Mutation mut : location_mutations.get(loc)) {
				if (mut.verdict == Verdict.KILLED) {
					kill++;
					System.out.println("KILLED" + " at " + mut.location + " " + mut.description + " by " + mut.killing_properties + " at k = " + mut.killing_k);
				}
			}
		}

		for (Location loc : location_mutations.keySet()) {
			for (Mutation mut : location_mutations.get(loc)) {
				if (mut.verdict == Verdict.UNKNOWN) {
					unkn++;
					System.out.println("UNKNOWN" + " at " + mut.location + " " + mut.description + " surviving to " + mut.surviving_properties);
				}
			}
		}

		for (Location loc : location_mutations.keySet()) {
			for (Mutation mut : location_mutations.get(loc)) {
				if (mut.verdict == Verdict.SURVIVED) {
					surv++;
					System.out.println("SURVIVED" + " at " + mut.location + " " + mut.description);
				}
			}
		}

		writeLine();
		System.out.println("MUTATION +++ k-ind widening: " + settings.kIndWidening + ", killing k: "
							+ settings.kKill + ", parrallel tasks: " + settings.parallelMutants);
 		System.out.println("SURVIVED : " + surv + ", KILLED :" + kill + ", UNKNOWN :" + unkn
							+ ", running time :" + Util.secondsToTime((System.currentTimeMillis() - startTime) / 1000.0));
		writeLine();

	}

	@Override
	public void writeNodeInputMutation(HashMap<Expr, Mutation> node_input_mutations) {
		writeLine();

		for (Expr ex : node_input_mutations.keySet()) {
			if (node_input_mutations.get(ex).verdict == Verdict.KILLED) {
				System.out.println("KILLED: Node input: " + ex.toString() + " at location: "
						+ node_input_mutations.get(ex).location);
			}
		}
		writeLine();
	}
}
