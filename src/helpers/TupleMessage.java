package txrelaysim.src.helpers;

import peersim.core.Node;

public class TupleMessage extends SimpleMessage {

	private Node x;
	private int y;
	private boolean z;

	public TupleMessage(int type, Node sender, Node x, int y, boolean z) {
		super(type, sender);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Node getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public boolean getZ() {
		return this.z;
	}
}
