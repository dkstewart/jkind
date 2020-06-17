package jkind.engines.messages;

import java.util.HashMap;

import jkind.engines.mutation.Mutation;
import jkind.lustre.Expr;

public class NodeInputMutationMessage extends Message {
	public final HashMap<Expr, Mutation> node_input_mutations;

	public NodeInputMutationMessage(HashMap<Expr, Mutation> node_input_mutations) {
		super();
		this.node_input_mutations = node_input_mutations;
	}

	@Override
	public void accept(MessageHandler handler) {
		handler.handleMessage(this);
	}
}