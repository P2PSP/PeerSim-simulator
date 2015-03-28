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
	
    private static final String PAR_PROT = "protocol";
    private static final String PAR_MALICIOUS_COUNT = "malicious_count";
    private static final String PAR_TRUSTED_COUNT = "trusted_count";
	
	public PeerInitializer(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		maliciousCount = Configuration.getInt(prefix + "." + PAR_MALICIOUS_COUNT);
		trustedCount = Configuration.getInt(prefix + "." + PAR_TRUSTED_COUNT);
	}	
	
	@Override
	public boolean execute() {
		Peer.pidPeer = pid;
		
		//set source as not peer
		((Peer)Network.get(SourceInitializer.sourceIndex).getProtocol(pid)).isPeer = false;
		
		Node source = Network.get(0);
		
		for(int i = 1; i < Network.size(); i++) {
			Node node = Network.get(i);
			((Peer)node.getProtocol(pid)).isPeer = true;
			if (maliciousCount > 0) {
				((Peer)node.getProtocol(pid)).isMalicious = true;
				maliciousCount--;
			} else if (trustedCount > 0) {
				((Peer)node.getProtocol(pid)).isTrusted = true;
				trustedCount--;
			}
			
			SimpleMessage message = new SimpleMessage(SimpleEvent.HELLO, Network.get(i));
			long latency = ((Transport)node.getProtocol(FastConfig.getTransport(Peer.pidPeer))).getLatency(node, source);
			if (((Peer)node.getProtocol(pid)).isMalicious) {
				latency += 200;
			}
			EDSimulator.add(latency, message, source, Source.pidSource);
		}
			
		
		return true;
	}
}