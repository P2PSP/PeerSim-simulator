package sim.src;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
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
	
	private Packet lastPacketFromSource = null;
	
	private Packet lastPacket = null;
	
	private int bufferSize;
	public Packet[] buffer;
	

	public Peer(String prefix)
	{
		bufferSize = Configuration.getInt(prefix+".buffer_size", 32);
		buffer = new Packet[bufferSize];
	}
	
	@Override
	public void nextCycle(Node node, int pid) 
	{	}
	
	/**
	 * The last packet FROM THE SOURCE from anyone is resent to everyone
	 */
//	@Override
	public void processEvent(Node node, int pid, Object event)
	{
		if(event instanceof Packet == false)
			return;
		
		Packet packet = (Packet) event;
		System.out.print("Peer "+node.getIndex()+": packet "+packet.index+" received from "+packet.sender);

		//store in buffer
		buffer[packet.index%buffer.length] = packet;
		
		if(packet.sender == SourceInitializer.sourceIndex)	//the sender is the source
		{
			System.out.print(", resending to "+ packet.resendTo +"...");
			Node recipient = Network.get(packet.resendTo);
			//now resend to recipient the incoming packet, which is also the new "lastPacketFromSource"
			lastPacketFromSource = packet;
			((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, new Packet(node.getIndex(), lastPacketFromSource.index, null), Peer.pidPeer);
		}
		else	//the sender is not the source
		{
			Node recipient = Network.get(packet.sender);
			if(lastPacketFromSource != null && recipient.getIndex() != lastPacketFromSource.resendTo) //last condition is used to prevent an endless "ping-pong"
			{
				System.out.print(", sending packet "+lastPacketFromSource.index+" back");
				//now first send the last packet from source
				((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, new Packet(node.getIndex(), lastPacketFromSource.index, null), Peer.pidPeer);
			}
		}
		System.out.println();
	}

	
	/**
	 * The last packet FROM ANYONE is resent to everyone
	 */
	//@Override
	public void processEvent2(Node node, int pid, Object event)
	{
		if(event instanceof Packet == false)
			return;
		
		Packet packet = (Packet) event;
		System.out.print("Peer "+node.getIndex()+": packet "+packet.index+" received from "+packet.sender);

		//store in buffer
		buffer[packet.index%buffer.length] = packet;
		
		if(packet.sender == SourceInitializer.sourceIndex)	//the sender is the source
		{
			System.out.print(", resending to "+ packet.resendTo +"...");
			Node recipient = Network.get(packet.resendTo);
			//now resend to recipient the incoming packet, which is also the new "lastPacket"
			lastPacket = packet;
			((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, new Packet(node.getIndex(), lastPacket.index, null), Peer.pidPeer);
		}
		else	//the sender is not the source
		{
			Node recipient = Network.get(packet.sender);
			if(lastPacket != null && recipient.getIndex() != lastPacket.resendTo)	//last condition is used to prevent an endless "ping-pong"
			{
				System.out.print(", sending packet "+lastPacket.index+" back");
				//now first send the last packet and then replace "lastPacket"
				((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, new Packet(node.getIndex(), lastPacket.index, null), Peer.pidPeer);
				lastPacket = packet;
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
