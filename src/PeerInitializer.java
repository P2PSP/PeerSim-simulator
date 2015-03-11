package sim.src;

import peersim.config.*;
import peersim.core.*;
import peersim.transport.Transport;

public class PeerInitializer implements Control
{
	int pid;
    private static final String PAR_PROT = "protocol";
	
	public PeerInitializer(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}	
	
	@Override
	public boolean execute() {
		//set the Peer pid
		Peer.pidPeer = pid;
		
		//set source as not peer
		((Peer)Network.get(SourceInitializer.sourceIndex).getProtocol(pid)).isPeer = false;
		
		//set other peers as peer
		for(int i = 1; i < Network.size(); i++) {
			Node source = Network.get(0);
			SimpleMessage message = new SimpleMessage(SimpleEvent.HELLO, Network.get(i));
			((Transport)source.getProtocol(FastConfig.getTransport(pid))).send(Network.get(i), source, message, Source.pidSource);
			((Peer)Network.get(i).getProtocol(pid)).isPeer = true;
			
		}
			
		
		return true;
	}
}