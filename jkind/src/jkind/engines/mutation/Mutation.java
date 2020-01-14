package jkind.engines.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jkind.lustre.Ast;
import jkind.lustre.Location;

public class Mutation {
	public Location location;
 	public final Mutator mutator;
	public final Map<Ast, Ast> mutationMap;
	public String description;
	
	public enum Verdict {SURVIVED, KILLED, UNKNOWN};
	public Verdict verdict;
	public List<String> killing_properties;
	public int killing_k;
	public List<String> surviving_properties;
	
	public Mutation(Location location, Mutator mutator, Map<Ast, Ast> mutationMap) {
		super();
		this.location = location;
		this.mutator = mutator;
		this.mutationMap = mutationMap;
		this.description = mutator.name;
		this.verdict = Verdict.SURVIVED;
		this.killing_properties = new ArrayList<>();
		this.surviving_properties = new ArrayList<>();
	}	
}
