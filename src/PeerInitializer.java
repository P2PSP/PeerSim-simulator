package txrelaysim.src;

import txrelaysim.src.helpers.*;

import java.util.HashSet;
import java.util.HashMap;

import peersim.config.*;
import peersim.core.*;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

public class PeerInitializer implements Control
{
	private int pid;
	private int reachableCount;
	private int privateBlackHolesPercent;
	private int outPeers;
	private int inFloodDelay;
	private int outFloodDelay;

	private boolean allReconcile;
	// Reconciliation params
	private int outFloodPeers;
	private int inFloodPeers;
	private double defaultQ;
	private int reconciliationInterval;

	public PeerInitializer(String prefix) {
		pid = Configuration.getPid(prefix + "." + "protocol");
		reachableCount = Configuration.getInt(prefix + "." + "reachable_count");
		outPeers = Configuration.getInt(prefix + "." + "out_peers");
		inFloodDelay = Configuration.getInt(prefix + "." + "in_flood_delay");
		outFloodDelay = Configuration.getInt(prefix + "." + "out_flood_delay");

		privateBlackHolesPercent = Configuration.getInt(prefix + "." + "private_black_holes_percent", 0);
		allReconcile = Configuration.getBoolean(prefix + "." + "all_reconcile");
		if (allReconcile) {
			reconciliationInterval = Configuration.getInt(prefix + "." + "reconciliation_interval");
			outFloodPeers = Configuration.getInt(prefix + "." + "out_flood_peers", outPeers);
			inFloodPeers = Configuration.getInt(prefix + "." + "in_flood_peers");
			defaultQ = Configuration.getDouble(prefix + "." + "default_q");
		}
	}

	@Override
	public boolean execute() {
		Peer.pidPeer = pid;

		int privateBlackHolesCount = (Network.size() - reachableCount) * privateBlackHolesPercent / 100;
		// Set a subset of nodes to be reachable by other nodes.
		while (reachableCount > 0) {
			int r = CommonState.r.nextInt(Network.size() - 1) + 1;
			if (!((Peer)Network.get(r).getProtocol(pid)).isReachable) {
				((Peer)Network.get(r).getProtocol(pid)).isReachable = true;
				--reachableCount;
			}
		}

		System.err.println("Black holes: " + privateBlackHolesCount);
		while (privateBlackHolesCount > 0) {
			int r = CommonState.r.nextInt(Network.size() - 1) + 1;
			if (!((Peer)Network.get(r).getProtocol(pid)).isReachable) {
				((Peer)Network.get(r).getProtocol(pid)).isBlackHole = true;
				--privateBlackHolesCount;
			}
		}
		System.err.println("Black holes: " + privateBlackHolesCount);

		// A list storing who is already connected to who, so that we don't make duplicate conns.
		HashMap<Integer, HashSet<Integer>> peers = new HashMap<>();
		for (int i = 1; i < Network.size(); i++) {
			peers.put(i, new HashSet<>());
			// Initial parameters setting for all nodes.
			((Peer)Network.get(i).getProtocol(pid)).inFloodDelay = inFloodDelay;
			((Peer)Network.get(i).getProtocol(pid)).outFloodDelay = outFloodDelay;
			if (allReconcile) {
				((Peer)Network.get(i).getProtocol(pid)).reconcile = true;
				((Peer)Network.get(i).getProtocol(pid)).reconciliationInterval = reconciliationInterval;
				((Peer)Network.get(i).getProtocol(pid)).inFloodLimit = inFloodPeers;
				((Peer)Network.get(i).getProtocol(pid)).outFloodLimit = outFloodPeers;
				((Peer)Network.get(i).getProtocol(pid)).defaultQ = defaultQ;
			}
		}

		// Connect all nodes to a limited number of reachable nodes.
		for(int i = 1; i < Network.size(); i++) {
			Node curNode = Network.get(i);
			int conns = 0;
			while (conns < outPeers) {
				int randomNodeIndex = CommonState.r.nextInt(Network.size() - 1) + 1;
				if (randomNodeIndex == i) {
					continue;
				}

				Node randomNode = Network.get(randomNodeIndex);

				if (!((Peer)randomNode.getProtocol(pid)).isReachable) {
					continue;
				}
				if (peers.get(i).contains(randomNodeIndex) || peers.get(randomNodeIndex).contains(i)) {
					continue;
				}

				peers.get(i).add(randomNodeIndex);
				peers.get(randomNodeIndex).add(i);

				// Actual connecting.
				((Peer)curNode.getProtocol(pid)).addOutboundPeer(randomNode);
				((Peer)randomNode.getProtocol(pid)).addInboundPeer(curNode);
				++conns;
			}
		}

		System.err.println("Initialized peers");
		return true;
	}
}