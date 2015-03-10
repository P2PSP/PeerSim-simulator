package sim.src;

import peersim.core.Node;

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