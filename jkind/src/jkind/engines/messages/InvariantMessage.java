package jkind.engines.messages;

import java.util.Collections;
import java.util.List;

import jkind.lustre.Expr;
import jkind.util.Util;

public class InvariantMessage extends Message {
	public final String source;
	public final List<Expr> invariants;
	public final int k;


	public InvariantMessage(String source, List<Expr> invs, int k) {
		this.source = source;
		this.invariants = Util.safeList(invs);
		this.k = k;
	}

	public InvariantMessage(String source, Expr invariant, int k) {
		this(source, Collections.singletonList(invariant), k);
	}

	@Override
	public void accept(MessageHandler handler) {
		handler.handleMessage(this);
	}
}
