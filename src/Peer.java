package sim.src;

import java.util.ArrayList;

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
	public IntMessage[] buffer;
	private ArrayList<Neighbor> peerList;

	public Peer(String prefix) {
		bufferSize = Configuration.getInt(prefix+".buffer_size", 32);
		buffer = new IntMessage[bufferSize];
		peerList = new ArrayList<Neighbor>();
		
	}
	
	@Override
	public void nextCycle(Node node, int pid) {}
	
	/**
	 * The last packet FROM THE SOURCE from anyone is resent to everyone
	 * @Override
	 */
	public void processEvent(Node node, int pid, Object event) {
		SimpleEvent castedEvent = (SimpleEvent)event;
		switch (castedEvent.getType()) {
		case SimpleEvent.CHUNK:
			processChunkMessage(node, pid, (IntMessage)castedEvent);
			break;
		case SimpleEvent.PEERLIST:
			processPeerlistMessage(node, pid, (ArrayListMessage)castedEvent);
			break;
		case SimpleEvent.HELLO:
			processHelloMessage(node, pid, (SimpleMessage)castedEvent);
			break;
		case SimpleEvent.GOODBYE:
			processGoodbyeMessage(node, pid, (SimpleMessage)castedEvent);
			break;	
		}
	}
	
	private void processChunkMessage(Node node, int pid, IntMessage message) {
		//store in buffer
		buffer[message.getInteger() % buffer.length] = message;
		if(message.getSender().getIndex() == SourceInitializer.sourceIndex) { //the sender is the source
			for (Neighbor peer : peerList) {
				IntMessage chunkMessage = new IntMessage(SimpleEvent.CHUNK, node, message.getInteger());
				((Transport)node.getProtocol(FastConfig.getTransport(pid))).send(node, peer.getNode(), chunkMessage, pid);
			}
		} else {
			addNewNeighbor(message.getSender());
		}
	}
	
	private void processPeerlistMessage(Node node, int pid, ArrayListMessage<Neighbor> message) {
		peerList.clear();
		for (Neighbor peer : message.getArrayList()) {
			peerList.add(peer);
			SimpleMessage helloMessage = new SimpleMessage(SimpleEvent.HELLO, node);
			((Transport)node.getProtocol(FastConfig.getTransport(pid))).send(node, peer.getNode(), helloMessage, pid);
		}
	}

	private void processHelloMessage(Node node, int pid, SimpleMessage message) {
		addNewNeighbor(message.getSender());
	}

	private void processGoodbyeMessage(Node node, int pid, SimpleMessage message) {
		// remove neighbor from peerList
	}
	
	private void addNewNeighbor(Node node) {
		boolean isExist = false;
		for (Neighbor peer : peerList) {
			if (peer.getNode().getID() == node.getID()) {
				isExist = true;
				break;
			}
		}
		if (!isExist) {
			peerList.add(new Neighbor(node));
		}
	}
	
	public Object clone() {
		return new Peer("");
	}	
}