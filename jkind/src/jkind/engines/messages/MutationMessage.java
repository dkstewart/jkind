package jkind.engines.messages;

import java.util.List;
import java.util.SortedMap;

import jkind.engines.mutation.Mutation;
import jkind.lustre.Location;

public class MutationMessage extends Message {
	public final SortedMap<Location, List<Mutation>> location_mutations;
	public final long startTime;

	public MutationMessage(SortedMap<Location, List<Mutation>> location_mutations, long startTime) {
		super();
		this.location_mutations = location_mutations;
		this.startTime = startTime;
	}

	@Override
	public void accept(MessageHandler handler) {
		handler.handleMessage(this);
	}
}
