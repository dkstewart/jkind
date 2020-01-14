package jkind;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;

import jkind.engines.SolverUtil;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class JKindArgumentParser extends ArgumentParser {
	private static final String EXCEL = "excel";
	private static final String INDUCT_CEX = "induct_cex";
	private static final String INV_GEN_LEVEL = "inv_gen_level";
	private static final String IVC = "ivc";
	private static final String MAIN = "main";
	private static final String N = "n";
	private static final String NO_BMC = "no_bmc";
	private static final String NO_INV_GEN = "no_inv_gen";
	private static final String NO_K_INDUCTION = "no_k_induction";
	private static final String NO_SLICING = "no_slicing";
	private static final String PDR_MAX = "pdr_max";
	private static final String READ_ADVICE = "read_advice";
	private static final String SCRATCH = "scratch";
	private static final String SMOOTH = "smooth";
	private static final String SOLVER = "solver";
	private static final String TIMEOUT = "timeout";
	private static final String WRITE_ADVICE = "write_advice";
	private static final String XML = "xml";
	private static final String XML_TO_STDOUT = "xml_to_stdout";

	private final JKindSettings settings;

	private JKindArgumentParser() {
		this("JKind", new JKindSettings());
	}

	private JKindArgumentParser(String name, JKindSettings settings) {
		super(name, settings);
		this.settings = settings;
	}

	@Override
	protected Options getOptions() {
		Options options = super.getOptions();
		options.addOption(EXCEL, false, "generate results in Excel format");
		options.addOption(INDUCT_CEX, false, "generate inductive counterexamples");
		options.addOption(INV_GEN_LEVEL, true,
				"invariant generator level for more and more invariants (default: 0, alternatives: 1, 2, 3)");
		options.addOption(IVC, false,
				"find an inductive validity core for valid properties (based on --%IVC annotated elements)");
		options.addOption(MAIN, true, "specify main node (overrides --%MAIN)");
		options.addOption(N, true, "maximum depth for bmc and k-induction (default: unbounded)");
		options.addOption(NO_BMC, false, "disable bounded model checking");
		options.addOption(NO_INV_GEN, false, "disable invariant generation");
		options.addOption(NO_K_INDUCTION, false, "disable k-induction");
		options.addOption(NO_SLICING, false, "disable slicing");
		options.addOption(PDR_MAX, true, "maximum number of PDR parallel instances (0 to disable PDR)");
		options.addOption(READ_ADVICE, true, "read advice from specified file");
		options.addOption(SCRATCH, false, "produce files for debugging purposes");
		options.addOption(SMOOTH, false, "smooth counterexamples (minimal changes in input values)");
		options.addOption(SOLVER, true,
				"SMT solver (default: smtinterpol, alternatives: z3, yices, yices2, cvc4, mathsat)");
		options.addOption(TIMEOUT, true, "maximum runtime in seconds (default: unbounded)");
		options.addOption(WRITE_ADVICE, true, "write advice to specified file");
		options.addOption(XML, false, "generate results in XML format");
		options.addOption(XML_TO_STDOUT, false, "generate results in XML format on stardard out");
		return options;
	}

	public static JKindSettings parse(String[] args) {
		JKindArgumentParser parser = new JKindArgumentParser();
		parser.parseArguments(args);
		parser.checkSettings();
		return parser.settings;
	}

	@Override
	protected void parseCommandLine(CommandLine line) {
		if (line.hasOption(VERSION)) {
			StdErr.println(name + " " + Main.VERSION);
			printDectectedSolvers();
			System.exit(0);
		}

		super.parseCommandLine(line);

		ensureExclusive(line, EXCEL, XML);
		ensureExclusive(line, EXCEL, XML_TO_STDOUT);
		ensureExclusive(line, XML, XML_TO_STDOUT);

		if (line.hasOption(EXCEL)) {
			settings.excel = true;
		}

		if (line.hasOption(INDUCT_CEX)) {
			settings.inductiveCounterexamples = true;
		}

		if (line.hasOption(INV_GEN_LEVEL)) {
			if (parseNonnegativeInt(line.getOptionValue(INV_GEN_LEVEL)) == 0) { // LEVEL 0
				settings.boolCandidates = true;
				settings.subrangeCandidates = true;
				settings.enumCandidates = true;
				settings.initCandidates = true;
				settings.typedIntConstCandidates = false;
				settings.typedIntIntCandidates = false;
				settings.allIntConstCandidates = false;
				settings.allIntIntCandidates = false;
				settings.combinatorialCandidates = false;
			} else if (parseNonnegativeInt(line.getOptionValue(INV_GEN_LEVEL)) == 1) { // LEVEL 1
				settings.boolCandidates = true;
				settings.subrangeCandidates = true;
				settings.enumCandidates = true;
				settings.initCandidates = true;
				settings.typedIntConstCandidates = true;
				settings.typedIntIntCandidates = true;
				settings.allIntConstCandidates = false;
				settings.allIntIntCandidates = false;
				settings.combinatorialCandidates = false;
			} else if (parseNonnegativeInt(line.getOptionValue(INV_GEN_LEVEL)) == 2) { // LEVEL 2
				settings.boolCandidates = true;
				settings.subrangeCandidates = true;
				settings.enumCandidates = true;
				settings.initCandidates = true;
				settings.typedIntConstCandidates = true;
				settings.typedIntIntCandidates = true;
				settings.allIntConstCandidates = true;
				settings.allIntIntCandidates = true;
				settings.combinatorialCandidates = false;
			} else { // LEVEL 3
				settings.boolCandidates = true;
				settings.subrangeCandidates = true;
				settings.enumCandidates = true;
				settings.initCandidates = true;
				settings.typedIntConstCandidates = true;
				settings.typedIntIntCandidates = true;
				settings.allIntConstCandidates = true;
				settings.allIntIntCandidates = true;
				settings.combinatorialCandidates = true;
			}
		}

		if (line.hasOption(IVC)) {
			settings.reduceIvc = true;
		}

		if (line.hasOption(MAIN)) {
			settings.main = line.getOptionValue(MAIN);
		}

		if (line.hasOption(NO_BMC)) {
			settings.boundedModelChecking = false;
		}

		if (line.hasOption(NO_INV_GEN)) {
			settings.invariantGeneration = false;
		}

		if (line.hasOption(NO_K_INDUCTION)) {
			settings.kInduction = false;
		}

		if (line.hasOption(NO_SLICING)) {
			settings.slicing = false;

			/**
			 * Inlining without slicing away the inlined equations creates
			 * problems for reconstruction
			 */
			settings.inlining = false;
		}

		if (line.hasOption(N)) {
			settings.n = parseNonnegativeInt(line.getOptionValue(N));
		}

		if (line.hasOption(PDR_MAX)) {
			settings.pdrMax = parseNonnegativeInt(line.getOptionValue(PDR_MAX));
		} else {
			if (settings.pdrMax != 0) { // If pdrMax = 0, PDR is disabled
				int available = Runtime.getRuntime().availableProcessors();
				int heuristic = (available - 4) / 2;
				settings.pdrMax = Math.max(1, heuristic);
			}
		}

		if (line.hasOption(READ_ADVICE)) {
			settings.readAdvice = line.getOptionValue(READ_ADVICE);
		}

		if (line.hasOption(TIMEOUT)) {
			settings.timeout = parseNonnegativeInt(line.getOptionValue(TIMEOUT));
		}

		if (line.hasOption(SCRATCH)) {
			settings.scratch = true;
		}

		if (line.hasOption(SMOOTH)) {
			settings.smoothCounterexamples = true;
		}

		if (line.hasOption(SOLVER)) {
			settings.solver = getSolverOption(line.getOptionValue(SOLVER));
		}

		if (line.hasOption(WRITE_ADVICE)) {
			settings.writeAdvice = line.getOptionValue(WRITE_ADVICE);
		}

		if (line.hasOption(XML)) {
			settings.xml = true;
		}

		if (line.hasOption(XML_TO_STDOUT)) {
			settings.xmlToStdout = true;
			settings.xml = true;
		}
	}

	private static SolverOption getSolverOption(String solver) {
		List<SolverOption> options = Arrays.asList(SolverOption.values());
		for (SolverOption option : options) {
			if (solver.equals(option.toString())) {
				return option;
			}
		}

		StdErr.error("unknown solver: " + solver);
		StdErr.println("Valid options: " + options);
		System.exit(ExitCodes.INVALID_OPTIONS);
		return null;
	}

	private void checkSettings() {
		if (settings.reduceIvc) {
			if (settings.solver == SolverOption.CVC4) {
				StdErr.warning(settings.solver + " does not support unsat-cores so IVC reduction will be slow");
			}
		}

		if (settings.smoothCounterexamples) {
			if (settings.solver != SolverOption.YICES && settings.solver != SolverOption.Z3) {
				StdErr.fatal(ExitCodes.INVALID_OPTIONS, "smoothing not supported with " + settings.solver);
			}
		}

		if (!settings.boundedModelChecking && !settings.kInduction && !settings.invariantGeneration
				&& settings.pdrMax == 0 && settings.readAdvice == null) {
			StdErr.fatal(ExitCodes.INVALID_OPTIONS, "all proving engines disabled");
		}

		if (!settings.boundedModelChecking && settings.kInduction) {
			StdErr.warning("k-induction requires bmc");
		}
		
		if (settings.mutation && (settings.kIndWidening < 0 || settings.kKill < 0 || settings.parallelMutants <= 0)) {
			StdErr.fatal(ExitCodes.INVALID_OPTIONS, "wrong mutation config (negative parameters)");
		}
		
		if (settings.mutation && !settings.reduceIvc && settings.ivcMutation) {
			StdErr.fatal(ExitCodes.INVALID_OPTIONS, "please activate ivc-reduction to enable mutation using ivc invariants");
		}
		
		if (settings.mutation && !settings.reduceIvc) {
			StdErr.warning("Attention : les invariants n'auront pas le temps d'arriver ;-)");
		}
		
	}

	private void printDectectedSolvers() {
		String detected = SolverUtil.availableSolvers().stream().map(Object::toString).collect(joining(", "));
		StdErr.println("Detected solvers: " + detected);
	}
}
