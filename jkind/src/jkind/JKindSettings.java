package jkind;


public class JKindSettings extends Settings {
	public int n = Integer.MAX_VALUE;
	public int timeout = Integer.MAX_VALUE;

	public boolean excel = false;
	public boolean xml = false;
	public boolean xmlToStdout = false;

	public String main = null;
	public boolean boundedModelChecking = true;
	public boolean kInduction = true;
	public boolean invariantGeneration = true;
   	public int pdrMax = 1;
	public boolean inductiveCounterexamples = false;
	public boolean reduceIvc = true;
	public boolean smoothCounterexamples = false;
	public boolean inlining = false;
	public boolean slicing = true;

	public SolverOption solver = SolverOption.Z3; // default SMTINTERPOL
	public boolean scratch = false;

	public String writeAdvice = null;
	public String readAdvice = null;

	// INVGEN options

	// LEVEL 0 (default JKind level)
	// Boolean candidates generation, default true
	public boolean boolCandidates = true;
	// Integer subrange candidates generation, default true
	public boolean subrangeCandidates = true;
	// ENUM candidates generation, default true
	public boolean enumCandidates = true;
	// Integer with init values candidates generation, default true
	public boolean initCandidates = true;

	// LEVEL 1
	// Adds additional candidates of INT and CONST having the same type, default false
	public boolean typedIntConstCandidates = true;
	// Adds additional candidates of INT and INT having the same type, default false
	public boolean typedIntIntCandidates = true;

	// LEVEL 2
	// Adds additional candidates of INT and CONST no matter their type, default false
	public boolean allIntConstCandidates = true;
	// Adds additional candidates of INT and INT no matter their type, default false
	public boolean allIntIntCandidates = true;

	// LEVEL 3
	// Adds also non combinatorial candidates, default false
	public boolean combinatorialCandidates = false;

	// MUTATION
	public boolean allAssigned = true; // Activates reduce_ivc
	public boolean mutation = true; // Activates mutation
	public int kIndWidening = 1; // To try a larger k-ind than the original ">= 0"
	public int kKill = 7; // For the unknown properties try BMC to that k ">= 0"
	public int parallelMutants = 1; // Number of parallel tasks for mutation ">= 1"
	public boolean ivcMutation = false; // Whether concentrate mutation on ivc invariants or use all inv-gen invariants

	public boolean debug = false; // Show more information about the progression
}
