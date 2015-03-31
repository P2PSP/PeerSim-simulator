package sim.src;

import java.util.ArrayList;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.*;
import peersim.transport.Transport;


public class Peer implements CDProtocol, EDProtocol
{
	public static int pidPeer;
	public boolean isPeer = false;
	private int bufferSize;
	public IntMessage[] buffer;
	public ArrayList<Neighbor> peerList;
	public ArrayList<Integer> badPeerList;
	public boolean isMalicious = false;
	public boolean isTrusted = false;

	public Peer(String prefix) {
		bufferSize = Configuration.getInt(prefix+".buffer_size", 32);
		buffer = new IntMessage[bufferSize];
		peerList = new ArrayList<Neighbor>();
		badPeerList = new ArrayList<Integer>();
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
			processPeerlistMessage(node, pid, (ArrayListMessage<Neighbor>)castedEvent);
			break;
		case SimpleEvent.HELLO:
			processHelloMessage(node, pid, (SimpleMessage)castedEvent);
			break;
		case SimpleEvent.GOODBYE:
			processGoodbyeMessage(node, pid, (SimpleMessage)castedEvent);
			break;	
		case SimpleEvent.BAD_PEER:
			processBadPeerMessage(node, pid, (IntMessage)castedEvent);
			break;
		}
	}
	
	private void processChunkMessage(Node node, int pid, IntMessage message) {
		storeInBuffer(node, message);
		if(message.getSender().getIndex() == SourceInitializer.sourceIndex) { //the sender is the source
			int latencySum = 0;
			for (Neighbor peer : peerList) {
				IntMessage chunkMessage = new IntMessage(SimpleEvent.CHUNK, node, message.getInteger() * (this.isMalicious ? -1 : 1));
				latencySum += chunkMessage.getLatency(peer.getNode(), pid);
				EDSimulator.add(latencySum, chunkMessage, peer.getNode(), pid);
			}
		} else {
			if (this.isTrusted) {
				TupleMessage chunkCheckMessage = new TupleMessage(SimpleEvent.CHUNK_CHECK, node, message.getSender().getIndex(), message.getInteger());
				long latency = chunkCheckMessage.getLatency(Network.get(0), pid);
				EDSimulator.add(latency, chunkCheckMessage, Network.get(0), Source.pidSource);
			}
			if (!isInBadPeerList(message.getSender().getIndex())) {
				addNewNeighbor(message.getSender());
			}
		}
	}
	
	private void storeInBuffer(Node node, IntMessage message) {
		if (!isInBadPeerList(message.getSender().getIndex())) {
			buffer[Math.abs(message.getInteger()) % buffer.length] = message;
		}
	}
	
	private boolean isInBadPeerList(int index) {
		boolean isInBadPeerList = false;
		for (int peer : badPeerList) {
			if (peer == index) {
				isInBadPeerList = true;
				break;
			}
		}
		return isInBadPeerList;
	}
	
	private void processPeerlistMessage(Node node, int pid, ArrayListMessage<Neighbor> message) {
		peerList.clear();
		for (Neighbor peer : message.getArrayList()) {
			peerList.add(peer);
			SimpleMessage helloMessage = new SimpleMessage(SimpleEvent.HELLO, node);
			long latency = helloMessage.getLatency(peer.getNode(), pid);
			EDSimulator.add(latency, helloMessage, peer.getNode(), pid);
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
	
	private void processBadPeerMessage(Node node, int pid, IntMessage message) {
		badPeerList.add(message.getInteger());
		removeNeighbor(message.getInteger());
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
	
	public Object clone() {
		return new Peer("");
	}	
}