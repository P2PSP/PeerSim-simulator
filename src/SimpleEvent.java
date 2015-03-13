package sim.src;

public class SimpleEvent {
	
	public static final int HELLO = 1;
	public static final int GOODBYE = 2;
	public static final int CHUNK = 3;
	public static final int PEERLIST = 4;
	public static final int BAD_PEER = 5;
	
	private int type;
	
	public SimpleEvent(int type) {
		this.type = type;
	}
	
	public int getType() {
		return this.type;
	}

}
