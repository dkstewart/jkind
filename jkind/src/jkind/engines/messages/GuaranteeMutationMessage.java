package jkind.engines.messages;

import java.util.HashMap;

import jkind.engines.mutation.Mutation;
import jkind.lustre.Expr;

public class GuaranteeMutationMessage extends Message {
	public final HashMap<Expr, Mutation> guarantee_mutations;

	public GuaranteeMutationMessage(HashMap<Expr, Mutation> guarantee_mutations) {
		super();
		this.guarantee_mutations = guarantee_mutations;
	}

	@Override
	public void accept(MessageHandler handler) {
		handler.handleMessage(this);
	}
}
