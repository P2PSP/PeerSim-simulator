package sim.src;

import java.util.ArrayList;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;


public class Source implements CDProtocol, EDProtocol
{
	public static  int pidSource;
	
	public boolean isSource = false;
	private int packetIndex = 1;
	private int recipientIndex = 1;
	private int cycle = 1;
	private ArrayList<Neighbor> peerList;
	
	public Source(String prefix) {
		this.peerList = new ArrayList<Neighbor>();
	}
	
	@Override
	public void nextCycle(Node node, int pid) {
		Node recipient;
		int nextNodeIndex;
		
		if(isSource == false)
			return;
		
		System.out.println("\nCycle " + cycle +". This is SOURCE sending packet "+packetIndex+" to node "+recipientIndex+".\n");
		if (peerList.size() > 0) {
			recipient = peerList.get(recipientIndex).getNode();
			//next node in the list
			nextNodeIndex = (recipientIndex+1) % peerList.size();
			
			//send packet to this node, with nextNodeIndex in the resendTo field
			IntMessage chunkMessage = new IntMessage(SimpleEvent.CHUNK, node, packetIndex);
			((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, chunkMessage, Peer.pidPeer);
			
			//for next cycle
			packetIndex++;
			recipientIndex = nextNodeIndex;
		}
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

	@Override
	public void processEvent(Node node, int pid, Object event) {
		SimpleEvent castedEvent = (SimpleEvent)event;
		switch (castedEvent.getType()) {
		case SimpleEvent.HELLO:
			processHelloMessage(node, pid, (SimpleMessage)castedEvent);
			break;
		case SimpleEvent.GOODBYE:
			processGoodbyeMessage(node, pid, (SimpleMessage)castedEvent);
			break;
		case SimpleEvent.CHUNK_CHECK:
			processChunkCheckMessage(node, pid, (TupleMessage)castedEvent);
		}
	}
	
	private void processHelloMessage(Node node, int pid, SimpleMessage receivedMessage) {
		ArrayList<Neighbor> clone = new ArrayList<Neighbor>();
		for (Neighbor peer : this.peerList) clone.add(peer);
		ArrayListMessage<Neighbor> message = new ArrayListMessage<Neighbor>(SimpleEvent.PEERLIST, node, clone);
		Node sender = receivedMessage.getSender();
		((Transport)node.getProtocol(FastConfig.getTransport(pid))).send(node, sender, message, Peer.pidPeer);
		peerList.add(new Neighbor(receivedMessage.getSender()));
	}
	
	private void processGoodbyeMessage(Node node, int pid, SimpleMessage receivedMessage) {
		Neighbor peerToRemove = null;
		for (Neighbor peer : peerList) {
			if (peer.getNode().getID() == receivedMessage.getSender().getID()) {
				peerToRemove = peer;
				break;
			}
		}
		if (peerToRemove != null) {
			peerList.remove(peerToRemove);
		}
	}
	
	private void processChunkCheckMessage(Node node, int pid, TupleMessage receivedMessage) {
		int chunkNum = receivedMessage.getY();
		if (chunkNum < 0) { // poisoned chunk
			removeNeighbor(receivedMessage.getX());
			IntMessage badPeerMessage = new IntMessage(SimpleEvent.BAD_PEER, node, receivedMessage.getX());
			for (Neighbor peer : peerList) {
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).send(node, peer.getNode(), badPeerMessage, Peer.pidPeer);
			}
		}
	}
	
	private void removeNeighbor(int index) {
		Neighbor toRemove = null;
		for (Neighbor peer : peerList) {
			if (peer.getNode().getIndex() == index) {
				toRemove = peer;
				break;
			}
		}
		peerList.remove(toRemove);
	}
	
}