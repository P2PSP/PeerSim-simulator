package sim.src;

import peersim.config.*;
import peersim.core.*;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

public class PeerInitializer implements Control
{
	private int pid;
	private int maliciousCount;
	private int trustedCount;
	private int randomArrival;
	
    private static final String PAR_PROT = "protocol";
    private static final String PAR_MALICIOUS_COUNT = "malicious_count";
    private static final String PAR_TRUSTED_COUNT = "trusted_count";
    private static final String PAR_RANDOM_ARRIVAL = "random_arrival";
    private static final int DELAY_UPPER_BOUND = 1000;
	
	public PeerInitializer(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		maliciousCount = Configuration.getInt(prefix + "." + PAR_MALICIOUS_COUNT);
		trustedCount = Configuration.getInt(prefix + "." + PAR_TRUSTED_COUNT);
		randomArrival = Configuration.getInt(prefix + "." + PAR_RANDOM_ARRIVAL);
	}	
	
	@Override
	public boolean execute() {
		//set the Peer pid
		Peer.pidPeer = pid;
		
		//set source as not peer
		((Peer)Network.get(SourceInitializer.sourceIndex).getProtocol(pid)).isPeer = false;
		
		Node source = Network.get(0);
		//set other peers as peer
		for(int i = 1; i < Network.size(); i++) {
			Node node = Network.get(i);
			SimpleMessage message = new SimpleMessage(SimpleEvent.HELLO, Network.get(i));
			if (randomArrival == 1) {
				long delay = CommonState.r.nextLong(DELAY_UPPER_BOUND);
				long messageLatency = ((Transport)node.getProtocol(FastConfig.getTransport(pid))).getLatency(node, source);
				EDSimulator.add(delay + messageLatency, message, source, Source.pidSource);
			} else {
				((Transport)source.getProtocol(FastConfig.getTransport(pid))).send(node, source, message, Source.pidSource);
			}
			((Peer)node.getProtocol(pid)).isPeer = true;
			
			if (maliciousCount > 0) {
				((Peer)node.getProtocol(pid)).isMalicious = true;
				maliciousCount--;
			} else if (trustedCount > 0) {
				((Peer)node.getProtocol(pid)).isTrusted = true;
				trustedCount--;
			}
		}
			
		
		return true;
	}
}