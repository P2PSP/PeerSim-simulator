package sim.src;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;


public class Peer implements CDProtocol, EDProtocol
{
	public static int pidPeer;
	public boolean isPeer = false;
	private Packet lastPacketFromSource = null;
	private int bufferSize;
	public Packet[] buffer;

	public Peer(String prefix) {
		bufferSize = Configuration.getInt(prefix+".buffer_size", 32);
		buffer = new Packet[bufferSize];
	}
	
	@Override
	public void nextCycle(Node node, int pid) {}
	
	/**
	 * The last packet FROM THE SOURCE from anyone is resent to everyone
	 * @Override
	 */
	public void processEvent(Node node, int pid, Object event) {
		if(event instanceof Packet == false)
			return;
		
		Packet packet = (Packet) event;
		System.out.print("Peer "+node.getIndex()+": packet "+packet.index+" received from "+packet.sender);

		//store in buffer
		buffer[packet.index%buffer.length] = packet;
		
		if(packet.sender == SourceInitializer.sourceIndex) { //the sender is the source
			System.out.print(", resending to "+ packet.resendTo +"...");
			Node recipient = Network.get(packet.resendTo);
			//now resend to recipient the incoming packet, which is also the new "lastPacketFromSource"
			lastPacketFromSource = packet;
			((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, new Packet(node.getIndex(), lastPacketFromSource.index, null), Peer.pidPeer);
		} else { //the sender is not the source
			Node recipient = Network.get(packet.sender);
			if(lastPacketFromSource != null && recipient.getIndex() != lastPacketFromSource.resendTo) { //last condition is used to prevent an endless "ping-pong"
				System.out.print(", sending packet "+lastPacketFromSource.index+" back");
				//now first send the last packet from source
				((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, new Packet(node.getIndex(), lastPacketFromSource.index, null), Peer.pidPeer);
			}
		}
		System.out.println();
	}
	
	public Object clone() {
		return new Peer("");
	}	
}