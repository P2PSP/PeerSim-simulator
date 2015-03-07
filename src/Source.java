package sim.src;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;


public class Source implements CDProtocol
{
	public static  int pidSource;
	
	public boolean isSource = false;
	private int packetIndex = 1;
	private int recipientIndex = 1;
	private int cycle = 1;
	
	public Source(String prefix){	}
	
	@Override
	public void nextCycle(Node node, int pid) {
		Node recipient;
		int nextNodeIndex;
		
		if(isSource == false)
			return;
		
		System.out.println("\nCycle " + cycle +". This is SOURCE sending packet "+packetIndex+" to node "+recipientIndex+".\n");
		recipient = Network.get(recipientIndex);
		//next node in the list
		nextNodeIndex = (recipientIndex+1) % Network.size();
		if(nextNodeIndex==0)
			nextNodeIndex++;
		
		//send packet to this node, with nextNodeIndex in the resendTo field
		((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, new Packet(node.getIndex(), packetIndex, nextNodeIndex), Peer.pidPeer);
		
		//for next cycle
		packetIndex++;
		recipientIndex = nextNodeIndex;
		
		cycle++;
	}

	/*
	 * Returns the regular peer with absolute index "index"
	 */
	public Peer getPeer(int index) {
		Node node = Network.get(index);
		//look for the Peer protocol
		for(int p = 0; p < node.protocolSize(); p++)
		{
			if(node.getProtocol(p) instanceof Peer)
				return (Peer)node.getProtocol(p);
		}
		
		return null;		
	}
	
	public Object clone() {
		return new Source("");
	}
}
