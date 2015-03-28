package sim.src;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

public class PoisonedChunksObserver implements Control {

	private static final String PAR_PROT = "protocol";
	
	private String name;
	private int pid;
	private int cycleLength;
	private int poisonedChunks;
	
	public PoisonedChunksObserver(String name) {
		this.name = name;
		pid = Configuration.getPid(name + "." + PAR_PROT);
		cycleLength = Configuration.getInt("CYCLE");
		poisonedChunks = 0;
	}
	
	@Override
	public boolean execute() {
		Peer peer;
		int currentPoisonedChunks = 0;
		for (int i = 1; i < Network.size(); i++) {
			peer = (Peer) Network.get(i).getProtocol(pid);
			for(int j = 0; j < peer.buffer.length; j++) {
				if(peer.buffer[j] == null) {
					
				} else {
					if (peer.buffer[j].getInteger() < 0) {
						currentPoisonedChunks++;
					}
				}
			}
		}
		if (currentPoisonedChunks >= poisonedChunks && CommonState.getEndTime() - CommonState.getTime() > 2 * cycleLength) {
			poisonedChunks = currentPoisonedChunks;
		} else {
			System.out.println("== " + poisonedChunks + " poisoned chunks" + " " + CommonState.getTime() );
			return true;
		}

		return false;
	}
	
}
