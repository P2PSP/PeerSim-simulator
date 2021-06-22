package txrelaysim.src.helpers;

import peersim.core.Node;

public class TupleMessage extends SimpleMessage {

	private Node x;
	private int y;

	public TupleMessage(int type, Node sender, Node x, int y) {
		super(type, sender);
		this.x = x;
		this.y = y;
	}

	public Node getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

}
