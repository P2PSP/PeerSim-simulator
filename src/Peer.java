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
import java.util.Collections;

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
	public double inFloodLimitPercent;
	public double outFloodLimitPercent;
	public int reconciliationInterval;
	public int inFloodDelay;
	public int outFloodDelay;
	public double defaultQ;

	/* State */
	public boolean isReachable = false;
	public boolean isBlackHole = false;
	public ArrayList<Node> outboundPeers;
	public ArrayList<Node> inboundPeers;
	public HashMap<Integer, Long> txArrivalTimes;
	public HashMap<Node, HashSet<Integer>> peerKnowsTxs;
	public long nextFloodInbound = 0;

	/* Reconciliation state */
	public boolean reconcile = false;
	public Queue<Node> reconciliationQueue;
	public long nextRecon = 0;
	// This variable is used to check if a peer supports reconciliations.
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
				nextRecon = curTime + (reconciliationInterval / reconciliationQueue.size());
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
			if (reconcile && reconSets.containsKey(sender)) {
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
		HashSet<Integer> reconSet = reconSets.get(sender);
		ArrayListMessage<Integer> sketch = new ArrayListMessage<Integer>(SimpleEvent.SKETCH, node, new ArrayList<Integer>(reconSet));
		((Transport)sender.getProtocol(FastConfig.getTransport(Peer.pidPeer))).send(node, sender, sketch, Peer.pidPeer);
		for (Integer txId: reconSet) {
			peerKnowsTxs.get(sender).add(txId);
		}
		reconSet.clear();
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
				theyMiss++;
				IntMessage inv = new IntMessage(SimpleEvent.INV, node, txId);
				((Transport)sender.getProtocol(FastConfig.getTransport(Peer.pidPeer))).send(node, sender, inv, Peer.pidPeer);
				++invsSent;
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
		int capacity = Math.abs(localSetSize - remoteSetSize) + (int)(defaultQ * Math.min(localSetSize, remoteSetSize)) + 1;
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
		boolean shouldFlood = scheduledInv.getZ();
		if (!peerKnowsTxs.get(recipient).contains(txId)) {
			peerKnowsTxs.get(recipient).add(txId);

			if (reconcile && reconSets.containsKey(recipient)) {
				if (shouldFlood) {
					removeFromReconSet(node, txId, recipient);
				} else {
					reconSets.get(recipient).add(txId);
				}
			}

			if (shouldFlood) {
				IntMessage inv = new IntMessage(SimpleEvent.INV, node, txId);
				((Transport)recipient.getProtocol(FastConfig.getTransport(Peer.pidPeer))).send(node, recipient, inv, Peer.pidPeer);
				++invsSent;
			}
		}
	}

	private void relayTx(Node node, int pid, int txId, Node sender) {
		if (isBlackHole) {
			// Black holes don't relay. Note that black holes are only useful to measure latency
			// impact, measuring/comparing bandwidth is currently not supported because it depends
			// on how exactly black holes operate (do they reconcile with empty sketches? or drop
			// sketches/requests on the floor?).
			return;
		}

		// Send to inbounds (flood or recon).
		// To preserve privacy against inbound observers with multiple connections,
		// they share the timer (as in the Bitcoin peer-to-peer layer).
		long delay;
		long curTime = CommonState.getTime();
		if (nextFloodInbound < curTime) {
			nextFloodInbound = curTime + generateRandomDelay(this.inFloodDelay);
			delay = 0;
		} else {
			delay = nextFloodInbound - curTime;
		}

		// Send to inbounds.
		// First flood to all non-reconciling peers.
		// Then flood to a random subset of remaining reconciling peers, according to a defined
		// fraction. For the rest, reconcile.
		int inboundFloodTargets = (int)(inboundPeers.size() * inFloodLimitPercent / 100);
		for (Node peer : inboundPeers) {
			if (!reconSets.containsKey(peer)) { // check for non-reconciling
				scheduleInv(node, delay, peer, txId, true);
				if (inboundFloodTargets > 0) inboundFloodTargets--;
			}
		}

		// Now flood to a random subset of remaining (reconciling) peers, according to a defined
		// fraction. For the rest, reconcile.
		Collections.shuffle(inboundPeers);
		for (Node peer : inboundPeers) {
			// Skip non-reconciling peers.
			if (!reconSets.containsKey(peer)) continue;

			boolean shouldFlood = false;
			if (inboundFloodTargets > 0) {
				shouldFlood = true;
				inboundFloodTargets--;
			}
			scheduleInv(node, delay, peer, txId, shouldFlood);
		}

		// Send to outbounds.
		// First flood to all non-reconciling peers.
		// Then flood to a random subset of remaining reconciling peers, according to a defined
		// fraction. For the rest, reconcile.
		int outboundFloodTargets = (int)(outboundPeers.size() * outFloodLimitPercent / 100);
		for (Node peer : outboundPeers) {
			if (!reconSets.containsKey(peer)) { // check for non-reconciling
				delay = generateRandomDelay(this.outFloodDelay);
				scheduleInv(node, delay, peer, txId, true);
				if (outboundFloodTargets > 0) outboundFloodTargets--;
			}
		}

		// Now flood to a random subset of remaining (reconciling) peers, according to a defined
		// fraction. For the rest, reconcile.
		Collections.shuffle(outboundPeers);
		for (Node peer : outboundPeers) {
			// Skip non-reconciling peers.
			if (!reconSets.containsKey(peer)) continue;

			delay = generateRandomDelay(this.outFloodDelay);
			boolean shouldFlood = false;
			if (outboundFloodTargets > 0) {
				shouldFlood = true;
				outboundFloodTargets--;
			}
			scheduleInv(node, delay, peer, txId, shouldFlood);
		}
	}

	private void removeFromReconSet(Node node, int txId, Node target) {
		if (reconSets.get(target).contains(txId)) {
			reconSets.get(target).remove(txId);
		}
	}

	// We don't announce transactions right away, because usually the delay takes place to make it
	// more private.
	private void scheduleInv(Node node, long delay, Node recipient, int txId, boolean shouldFlood) {
		if (recipient.getID() == 0) {
			// Don't send to source.
			return;
		}

		if (peerKnowsTxs.get(recipient).contains(txId)) {
			return;
		}
		TupleMessage scheduledInv = new TupleMessage(SimpleEvent.SCHEDULED_INV, node, recipient, txId, shouldFlood);
		EDSimulator.add(delay, scheduledInv, node, Peer.pidPeer); // send to self.
	}

	// A helper for scheduling events which happen after a random delay.
	private long generateRandomDelay(long avgDelay) {
		return CommonState.r.nextLong(avgDelay * 2 + 1);
	}

	// Used for setting up the topology.
	public void addPeer(Node peer, boolean outbound, boolean supportsRecon) {
		if (outbound) {
			assert(!outboundPeers.contains(peer));
			outboundPeers.add(peer);
		} else {
			assert(!inboundPeers.contains(peer));
			inboundPeers.add(peer);
		}
		peerKnowsTxs.put(peer, new HashSet<>());
		if (reconcile && supportsRecon) {
			if (outbound) { reconciliationQueue.offer(peer); }
			reconSets.put(peer, new HashSet<>());
		}
	}
}