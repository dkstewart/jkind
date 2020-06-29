package jkind.engines.messages;

import java.util.HashMap;

import jkind.engines.mutation.Mutation;
import jkind.lustre.Equation;

public class GuaranteeMutationMessage extends Message {
	public final HashMap<Equation, Mutation> guarantee_mutations;

	public GuaranteeMutationMessage(HashMap<Equation, Mutation> guarantee_mutations) {
		super();
		this.guarantee_mutations = guarantee_mutations;
	}

	@Override
	public void accept(MessageHandler handler) {
		handler.handleMessage(this);
	}
}
