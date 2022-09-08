package txrelaysim.src.helpers;

import peersim.core.Node;

public class IntMessage extends SimpleMessage {

	private int integer;

	public IntMessage(int type, Node sender, int integer) {
		super(type, sender);
		this.integer = integer;
	}

	public int getInteger() {
		return this.integer;
	}

}
