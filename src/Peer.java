package txrelaysim.src;

import txrelaysim.src.helpers.*;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Queue;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.CommonState;
import peersim.edsim.*;
import peersim.transport.Transport;


public class Peer implements CDProtocol, EDProtocol
{
	/* System */
	public static int pidPeer;

	/* Constants and delays. Reconciliation only! */
	public int inFloodLimit;
	public int outFloodLimit;
	public int reconciliationInterval;
	public int inFloodDelay;
	public int outFloodDelay;
	public double defaultQ;

	/* State */
	public boolean isReachable = false;
	public boolean isBlackHole = false;
	public ArrayList<Node> outboundPeers;
	public ArrayList<Node> inboundPeers;
	public ArrayList<Node> inboundFloodDestinations;
	public ArrayList<Node> outboundFloodDestinations;
	public HashMap<Integer, Long> txArrivalTimes;
	public HashMap<Node, HashSet<Integer>> peerKnowsTxs;
	public long nextFloodInbound = 0;

	/* Reconciliation state */
	public boolean reconcile = false;
	public Queue<Node> reconciliationQueue;
	public long nextRecon = 0;
	public long nextReconResponse = 0;
	private HashMap<Node, HashSet<Integer>> reconSets;

	/* Stats */
	public int invsSent;
	public int shortInvsSent;
	public int txSent;

	public int successRecons;
	public int extSuccessRecons;
	public int failedRecons;

	public Peer(String prefix) {
		inboundPeers = new ArrayList<>();
		outboundPeers = new ArrayList<>();
		inboundFloodDestinations = new ArrayList<>();
		outboundFloodDestinations = new ArrayList<>();
		reconciliationQueue = new LinkedList<>();
		reconSets = new HashMap<>();
		peerKnowsTxs = new HashMap<>();
		txArrivalTimes = new HashMap<>();
	}

	public Object clone() {
		return new Peer("");
	}

	@Override
	public void nextCycle(Node node, int pid) {
		if (reconcile) {
			// If reconciliation is enabled on this node, it should periodically request reconciliations
			// with a queue of its reconciling peers.
			long curTime = CommonState.getTime();
			if (reconciliationQueue.peek() != null && curTime > nextRecon) {
				Node recipient = reconciliationQueue.poll();

				SimpleMessage request = new SimpleMessage(SimpleEvent.RECON_REQUEST, node);
				((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, request, Peer.pidPeer);

				// Move this node to the end of the queue, schedule the next reconciliation.
				reconciliationQueue.offer(recipient);
				nextRecon = curTime + reconciliationInterval;
			}
		}
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		SimpleEvent castedEvent = (SimpleEvent)event;
		switch (castedEvent.getType()) {
		case SimpleEvent.INV:
			// INV received from a peer.
			handleInvMessage(node, pid, (IntMessage)castedEvent);
			break;
		case SimpleEvent.RECON_REQUEST:
			// Reconciliation request from a peer.
			handleReconRequest(node, pid, (SimpleMessage)castedEvent);
			break;
		case SimpleEvent.SKETCH:
			// Sketch from a peer in response to reconciliation request.
			ArrayListMessage<?> ar = (ArrayListMessage<?>) castedEvent;
			ArrayList<Integer> remoteSet = new ArrayList<Integer>();
			for (Object x : ar.getArrayList()) {
				remoteSet.add((Integer) x);
			}
			handleSketchMessage(node, pid, ar.getSender(), remoteSet);
			break;
		case SimpleEvent.SCHEDULED_INV:
			// Self-scheduled INV to be sent to a peer.
			executeScheduledInv(node, pid, (TupleMessage)castedEvent);
			break;
		case SimpleEvent.SCHEDULED_SKETCH:
			// Self-scheduled SKETCH to be sent to a peer.
			executeScheduledSketch(node, pid, (SimpleMessage)castedEvent);
			break;
		case SimpleEvent.RECON_FINALIZATION:
			// We use this to track how many inv/shortinvs messages were sent for statas.
			handleReconFinalization(node, pid, (ArrayListMessage)castedEvent);
			break;
		case SimpleEvent.GETDATA:
			// We use this just for bandwidth accounting, the actual txId (what we need) was already
			// commnunicated so nothing to do here.
			++txSent;
			break;
		}
	}

	// Handle a transaction announcement (INV) from a peer. Remember when the transaction was
	// announced, and set it for further relay to other peers.
	private void handleInvMessage(Node node, int pid, IntMessage message) {
		int txId = message.getInteger();
		Node sender = message.getSender();

		if (sender.getID() != 0) {
			// Came not from source.
			peerKnowsTxs.get(sender).add(txId);
			if (reconcile) {
				removeFromReconSet(node, txId, sender);
			}
		}

		if (!txArrivalTimes.keySet().contains(txId)) {
			SimpleMessage getdata = new SimpleMessage(SimpleEvent.GETDATA, node);
			((Transport)sender.getProtocol(FastConfig.getTransport(pid))).send(node, sender, getdata, Peer.pidPeer);
			txArrivalTimes.put(txId, CommonState.getTime());
			relayTx(node, pid, txId, sender);
		}
	}

	private void handleReconRequest(Node node, int pid, SimpleMessage message) {
		Node sender = message.getSender();

		long curTime = CommonState.getTime();
		long delay;
		if (nextReconResponse < curTime) {
			delay = generateRandomDelay(inFloodDelay / 2);
			nextReconResponse = curTime + delay;
		} else {
			delay = nextReconResponse - curTime;
		}
		// TODO: it would be more efficient to batch them, and use a loop to check it.
		// A loop would help to not store scheduled messages in memory.
		SimpleMessage scheduledSketch = new SimpleMessage(SimpleEvent.SCHEDULED_SKETCH, sender);
		EDSimulator.add(delay, scheduledSketch, node, Peer.pidPeer); // send to self.
	}

	// Handle a sketch a peer sent us in response to our request. All sketch extension logic and
	// txId exchange is done here implicitly without actually sending messages, because a it can be
	// easily modeled and accounted at this node locally.
	private void handleSketchMessage(Node node, int pid, Node sender, ArrayList<Integer> remoteSet) {
		Set<Integer> localSet = reconSets.get(sender);
		int shared = 0, usMiss = 0, theyMiss = 0;
		// Handle transactions the local (sketch receiving) node doesn't have.
		for (Integer txId : remoteSet) {
			peerKnowsTxs.get(sender).add(txId);
			if (localSet.contains(txId)) {
				++shared;
			} else {
				++usMiss;
				if (!txArrivalTimes.keySet().contains(txId)) {
					SimpleMessage getdata = new SimpleMessage(SimpleEvent.GETDATA, node);
					((Transport)sender.getProtocol(FastConfig.getTransport(pid))).send(node, sender, getdata, Peer.pidPeer);
					txArrivalTimes.put(txId, CommonState.getTime());
					relayTx(node, pid, txId, sender);
				} else {
					// This is an edge case: sometimes a local set doesn't have a transaction
					// although we did receive/record it. It happens when we announce a transaction
					// to the peer and remove from the set while the peer sends us a sketch
					// including the same transaction.
				}
			}
		}

		// Handle transactions which the remote (sketch sending) node doesn't have.
		for (Integer txId : localSet) {
			if (!remoteSet.contains(txId)) {
				scheduleInv(node, 0, sender, txId);
				theyMiss++;
			}
		}

		// Compute the cost of this sketch exchange.
		int diff = usMiss + theyMiss;
		// This is a technicality of the simulator: in the finalization message we will notify
		// the node how much INV they supposedly sent us in this reconciliation round.
		int theySentInvs = 0, theySentShortInvs = 0;

		// Although diff estimation should happen at the sketch sender side, we do it here because
		// it works in our simplified model, to save extra messages.
		// To make it more detailed, we could remember the set size at request time here.
		int localSetSize = localSet.size();
		int remoteSetSize = remoteSet.size();
		// TODO: Q could be dynamicly updated after each reconciliation.
		int capacity = Math.abs(localSetSize - remoteSetSize) + (int)(defaultQ * (localSetSize + remoteSetSize)) + 1;
		if (capacity > diff) {
			// Reconciliation succeeded right away.
			successRecons++;
			theySentShortInvs = capacity; // account for sketch
			shortInvsSent += usMiss;
			theySentInvs += usMiss;
		} else if (capacity * 2 > diff) {
			// Reconciliation succeeded after extension.
			extSuccessRecons++;
			theySentShortInvs = capacity * 2;  // account for sketch and extension
			shortInvsSent += usMiss;
			theySentInvs += usMiss;
		} else {
			// Reconciliation failed.
			failedRecons++;
			theySentShortInvs = capacity * 2;  // account for sketch and extension
			// Above, we already sent them invs they miss.
			// Here, we just account for all the remaining full invs: what we miss, and shared txs.
			// I think ideally the "inefficient" overlap between our set and their set should
			// be sent by us, hence the accounting below.
			invsSent += shared;
			theySentInvs = usMiss;
		}

    	ArrayList<Integer> finalizationData = new ArrayList<Integer>();
		finalizationData.add(theySentInvs);
		finalizationData.add(theySentShortInvs);

		// System.err.println(theySentShortInvs);

		ArrayListMessage<Integer> reconFinalization = new ArrayListMessage<Integer>(
			SimpleEvent.RECON_FINALIZATION, node, finalizationData);
		((Transport)sender.getProtocol(FastConfig.getTransport(Peer.pidPeer))).send(
			node, sender, reconFinalization, Peer.pidPeer);

		localSet.clear();
	}

	private void handleReconFinalization(Node node, int pid, ArrayListMessage message) {
		invsSent += (Integer)message.getArrayList().get(0);
		shortInvsSent += (Integer)message.getArrayList().get(1);
	}

	// A node previously scheduled a transaction announcement to the peer. Execute it here when
	// this function is called by the scheduler.
	private void executeScheduledInv(Node node, int pid, TupleMessage scheduledInv) {
		Node recipient = scheduledInv.getX();
		int txId = scheduledInv.getY();
		if (!peerKnowsTxs.get(recipient).contains(txId)) {
			peerKnowsTxs.get(recipient).add(txId);
			IntMessage inv = new IntMessage(SimpleEvent.INV, node, txId);
			((Transport)recipient.getProtocol(FastConfig.getTransport(Peer.pidPeer))).send(node, recipient, inv, Peer.pidPeer);
			++invsSent;
			if (reconcile) {
				removeFromReconSet(node, txId, recipient);
			}
		}
	}

	// A node previously scheduled a sketch transmission to the peer. Execute it here when
	// this function is called by the scheduler.
	private void executeScheduledSketch(Node node, int pid, SimpleMessage scheduledSketch) {
		Node recipient = scheduledSketch.getSender();
		HashSet<Integer> reconSet = reconSets.get(recipient);
		ArrayListMessage<Integer> sketch = new ArrayListMessage<Integer>(SimpleEvent.SKETCH, node, new ArrayList<Integer>(reconSet));
		((Transport)recipient.getProtocol(FastConfig.getTransport(Peer.pidPeer))).send(node, recipient, sketch, Peer.pidPeer);
		for (Integer txId: reconSet) {
			peerKnowsTxs.get(recipient).add(txId);
		}
		reconSet.clear();
	}

	private void relayTx(Node node, int pid, int txId, Node sender) {
		if (isBlackHole) {
			// Black holes don't relay. Note that black holes are only useful to measure latency
			// impact, measuring/comparing bandwidth is currently not supported because it depends
			// on how exactly black holes operate (do they reconcile with empty sketches? or drop
			// sketches/requests on the floor?).
			return;
		}

		if (reconcile) {
			addToReconSets(node, pid, txId, sender);
		}
		flood(node, pid, sender, txId);
	}

	private void flood(Node node, int pid, Node sender, int txId) {
		// Send to inbounds.
		for (Node peer : inboundFloodDestinations) {
			long curTime = CommonState.getTime();
			// To preserve privacy against inbound observers with multiple connections,
			// they share the timer (as in the Bitcoin peer-to-peer layer).
			if (nextFloodInbound < curTime) {
				scheduleInv(node, 0, peer, txId);
				nextFloodInbound = curTime + generateRandomDelay(this.inFloodDelay);
			} else {
				scheduleInv(node, nextFloodInbound - curTime, peer, txId);
			}
		}

		// Rotate inbound flood destinations.
		if (reconcile && inFloodLimit != inboundPeers.size() && inboundPeers.size() != 0) {
			inboundFloodDestinations.clear();
			for (int i = 0; i < inFloodLimit;) {
				int randomIndex = CommonState.r.nextInt(inboundPeers.size());
				Node randomInboundPeer = inboundPeers.get(randomIndex);
				if (!inboundFloodDestinations.contains(randomInboundPeer)) {
					inboundFloodDestinations.add(randomInboundPeer);
					++i;
				}
			}
		}

		// Send to outbounds.
		for (Node peer : outboundFloodDestinations) {
			long delay = generateRandomDelay(this.outFloodDelay);
			scheduleInv(node, delay, peer, txId);
		}

		// Rotate outbound flood destinations.
		if (reconcile && outFloodLimit != outboundPeers.size() && outboundPeers.size() != 0) {
			outboundFloodDestinations.clear();
			for (int i = 0; i < outFloodLimit;) {
				int randomIndex = CommonState.r.nextInt(outboundPeers.size());
				Node randomInboundPeer = outboundPeers.get(randomIndex);
				if (!outboundFloodDestinations.contains(randomInboundPeer)) {
					outboundFloodDestinations.add(randomInboundPeer);
					++i;
				}
			}
		}
	}

	private void addToReconSets(Node node, int pid, int txId, Node sender) {
		for (Node n: reconSets.keySet()) {
			if (n != sender) {
				reconSets.get(n).add(txId);
			}
		}
	}

	private void removeFromReconSet(Node node, int txId, Node target) {
		if (reconSets.get(target).contains(txId)) {
			reconSets.get(target).remove(txId);
		}
	}

	// We don't announce transactions right away, because usually the delay takes place to make it
	// more private.
	private void scheduleInv(Node node, long delay, Node recipient, int txId) {
		if (recipient.getID() == 0) {
			// Don't send to source.
			return;
		}

		if (peerKnowsTxs.get(recipient).contains(txId)) {
			return;
		}
		TupleMessage scheduledInv = new TupleMessage(SimpleEvent.SCHEDULED_INV, node, recipient, txId);
		EDSimulator.add(delay, scheduledInv, node, Peer.pidPeer); // send to self.
	}

	// A helper for scheduling events which happen after a random delay.
	private long generateRandomDelay(long avgDelay) {
		return CommonState.r.nextLong(avgDelay * 2 + 1);
	}

	// The following methods used for setting up the topology.

	public void addInboundPeer(Node inboundPeer) {
		boolean alreadyConnected = false;
		for (Node existingPeer : inboundPeers) {
			if (existingPeer.getID() == inboundPeer.getID()) {
				alreadyConnected = true;
				break;
			}
		}
		if (!alreadyConnected) {
			inboundPeers.add(inboundPeer);
			if (reconcile) {
				reconSets.put(inboundPeer, new HashSet<>());
				if (inboundFloodDestinations.size() < inFloodLimit) {
					inboundFloodDestinations.add(inboundPeer);
				}
			} else {
				inboundFloodDestinations.add(inboundPeer);
			}
			peerKnowsTxs.put(inboundPeer, new HashSet<>());
		}
	}

	public void addOutboundPeer(Node outboundPeer) {
		boolean alreadyConnected = false;
		for (Node existingPeer : outboundPeers) {
			if (existingPeer.getID() == outboundPeer.getID()) {
				alreadyConnected = true;
				break;
			}
		}
		if (!alreadyConnected) {
			outboundPeers.add(outboundPeer);
			if (reconcile) {
				reconciliationQueue.offer(outboundPeer);
				reconSets.put(outboundPeer, new HashSet<>());
				if (outboundFloodDestinations.size() < outFloodLimit) {
					outboundFloodDestinations.add(outboundPeer);
				}
			} else {
				outboundFloodDestinations.add(outboundPeer);
			}
			peerKnowsTxs.put(outboundPeer, new HashSet<>());
		}
	}
}