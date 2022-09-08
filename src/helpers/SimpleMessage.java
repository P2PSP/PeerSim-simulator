package txrelaysim.src.helpers;

import peersim.config.FastConfig;
import peersim.core.Node;
import peersim.transport.Transport;

public class SimpleMessage extends SimpleEvent {

	private Node sender;

	public SimpleMessage(int type, Node sender) {
		super(type);
		this.sender = sender;
	}

	public Node getSender() {
		return this.sender;
	}
}