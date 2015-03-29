package sim.src;

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
	
	public long getLatency(Node dest, int pid) {
		Node src = this.getSender();
		long latency = ((Transport)src.getProtocol(FastConfig.getTransport(pid))).getLatency(src, dest);
		if (this.getType() != SimpleEvent.CHUNK) {
			latency = 1;
		}
		return latency;
	}
	
}