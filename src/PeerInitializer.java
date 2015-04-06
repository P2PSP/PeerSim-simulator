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
		while (maliciousCount > 0) {
			int r = CommonState.r.nextInt(Network.size() - 1) + 1;
			if (!((Peer)Network.get(r).getProtocol(pid)).isMalicious && !((Peer)Network.get(r).getProtocol(pid)).isTrusted) {
				((Peer)Network.get(r).getProtocol(pid)).isMalicious = true;
				maliciousCount--;
			}
		}
		while (trustedCount > 0) {
			int r = CommonState.r.nextInt(Network.size() - 1) + 1;
			if (!((Peer)Network.get(r).getProtocol(pid)).isMalicious && !((Peer)Network.get(r).getProtocol(pid)).isTrusted) {
				((Peer)Network.get(r).getProtocol(pid)).isTrusted = true;
				trustedCount--;
			}
		}
		for(int i = 1; i < Network.size(); i++) {
			Node node = Network.get(i);
			((Peer)node.getProtocol(pid)).isPeer = true;
			SimpleMessage message = new SimpleMessage(SimpleEvent.HELLO, Network.get(i));
			long latency = CommonState.r.nextInt(Network.size());
			EDSimulator.add(latency, message, source, Source.pidSource);
		}
			
		
		return true;
	}
}