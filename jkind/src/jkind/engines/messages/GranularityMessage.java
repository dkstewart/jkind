package jkind.engines.messages;

import java.util.HashMap;
import java.util.List;

import jkind.lustre.Equation;
import jkind.lustre.Node;

public class GranularityMessage extends Message {
	public final HashMap<Node, List<Equation>> granularity_vars;

	public GranularityMessage(HashMap<Node, List<Equation>> granularity_vars) {
		super();
		this.granularity_vars = granularity_vars;
	}

	@Override
	public void accept(MessageHandler handler) {
		handler.handleMessage(this);
	}
}
