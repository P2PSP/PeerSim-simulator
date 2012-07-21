package sim.src;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;


public class Peer implements CDProtocol, EDProtocol, Linkable
{
	public static int pidPeer;
	
	public boolean isPeer = false;

	public Peer(String prefix){	}
	
	@Override
	public void nextCycle(Node node, int pid) 
	{
		/*
		if(isPeer == false)
			return;
		
		System.out.println("I'm node "+node.getIndex());
		*/		
	}

	
	
	@Override
	public void processEvent(Node node, int pid, Object event) 
	{
		if(event instanceof Packet == false)
			return;
		
		Packet packet = (Packet)event;
		
		System.out.print("Peer "+node.getIndex()+": packet "+packet.index+" received");
		
		if(packet.source == SourceInitializer.sourceIndex)
		{
			System.out.print(", resending...");
			Node recipient;
			//change the source by ourselves so the recipient does not resend it
			packet.source = node.getIndex();
			//now send to the rest of peers
			for(int i = 1; i < node.getIndex() && i < Network.size(); i++)
			{
				recipient = Network.get(i);
				((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, packet, Peer.pidPeer);
			}
			for(int i = node.getIndex()+1; i < Network.size(); i++)
			{
				recipient = Network.get(i);
				((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, packet, Peer.pidPeer);
			}	
		}
		System.out.println();
		
	}


	
	
	
	
	
	
	/*
	 * Other overriden methods
	 */
		
	/**
	 * Clones the object.
	 */
	public Object clone()
	{
		return new Peer("");
	}


	@Override
	public void onKill() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean addNeighbor(Node arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Node arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int degree() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Node getNeighbor(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void pack() {
		// TODO Auto-generated method stub
		
	}

	
}
