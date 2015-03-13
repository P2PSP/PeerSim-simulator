package sim.src;

import peersim.core.Node;

public class TupleMessage extends SimpleMessage {
	
	private int x;
	private int y;
	
	public TupleMessage(int type, Node sender, int x, int y) {
		super(type, sender);
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return this.x;
	}
	
	public int getY() {
		return this.y;
	}

}
