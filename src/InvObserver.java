/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package txrelaysim.src;

import txrelaysim.src.helpers.*;

import peersim.config.*;
import peersim.core.*;
import peersim.util.*;

import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.*;


public class InvObserver implements Control
{
	/**
	 * The protocol to operate on.
	 * @config
	 */
	private static final String PAR_PROT = "protocol";

	/** The name of this observer in the configuration */
	private final String name;

	/** Protocol identifier */
	private final int pid;

	/**
	 * Standard constructor that reads the configuration parameters.
	 * Invoked by the simulation engine.
	 * @param name the configuration prefix for this class
	 */
	public InvObserver(String name) {
		this.name = name;
		pid = Configuration.getPid(name + "." + PAR_PROT);
	}

	public enum Protocol {
		ERLAY,
		LEGACY,
	}
	public enum NodeType {
		REACHABLE,
		PRIVATE,
	}

	public boolean execute() {
		// Track how many invs and txs were sent.
		HashMap<Protocol, Integer> invsByProtocol = new HashMap<>();
		HashMap<Protocol, Integer> txsByProtocol = new HashMap<>();
		HashMap<NodeType, Integer> invsByNodeType = new HashMap<>();
		HashMap<NodeType, Integer> txsByNodeType = new HashMap<>();
		HashMap<NodeType, Integer> shortInvsByNodeType = new HashMap<>();

		// Track reconciliation results across experiments.
		ArrayList<Integer> successRecons = new ArrayList<>();
		ArrayList<Integer> failedRecons = new ArrayList<>();
		// Track how soon transactions were propagating across the network.
		HashMap<Integer, ArrayList<Long>> txArrivalTimes = new HashMap<Integer, ArrayList<Long>>();
		int blackHoles = 0, reconcilingNodes = 0, reachableNodes = 0;

		for(int i = 1; i < Network.size(); i++) {
			Peer peer = (Peer) Network.get(i).getProtocol(pid);

			// Store all arrival times (at every node) for all transactions. We will later use this
			// to calculate latency.
			Iterator it = peer.txArrivalTimes.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				Integer txId = (Integer)pair.getKey();
				Long arrivalTime = (Long)pair.getValue();
				if (txArrivalTimes.get(txId) == null) {
					txArrivalTimes.put(txId, new ArrayList<>());
				}
				txArrivalTimes.get(txId).add(arrivalTime);
			}

			// See how many black holes there were.
			if (peer.isBlackHole) {
				++blackHoles;
				continue;
			}

			if (peer.isReachable) {
				invsByNodeType.put(NodeType.REACHABLE, invsByNodeType.getOrDefault(NodeType.REACHABLE, 0) + peer.invsSent);
				txsByNodeType.put(NodeType.REACHABLE, txsByNodeType.getOrDefault(NodeType.REACHABLE, 0) + peer.txSent);
				reachableNodes++;
			} else {
				invsByNodeType.put(NodeType.PRIVATE, invsByNodeType.getOrDefault(NodeType.PRIVATE, 0) + peer.invsSent);
				txsByNodeType.put(NodeType.PRIVATE, txsByNodeType.getOrDefault(NodeType.PRIVATE, 0) + peer.txSent);
			}

			// See how many inv/shortinv/tx messages every node sent.
			if (peer.reconcile) {
				invsByProtocol.put(Protocol.ERLAY, invsByProtocol.getOrDefault(Protocol.ERLAY, 0) + peer.invsSent);
				txsByProtocol.put(Protocol.ERLAY, txsByProtocol.getOrDefault(Protocol.ERLAY, 0) + peer.txSent);
				reconcilingNodes++;
				successRecons.add(peer.successRecons);
				failedRecons.add(peer.failedRecons);

				if (peer.isReachable) {
					shortInvsByNodeType.put(NodeType.REACHABLE, shortInvsByNodeType.getOrDefault(NodeType.REACHABLE, 0) + peer.shortInvsSent);
				} else {
					shortInvsByNodeType.put(NodeType.PRIVATE, shortInvsByNodeType.getOrDefault(NodeType.PRIVATE, 0) + peer.shortInvsSent);
				}
			} else {
				invsByProtocol.put(Protocol.LEGACY, invsByProtocol.getOrDefault(Protocol.LEGACY, 0) + peer.invsSent);
				txsByProtocol.put(Protocol.LEGACY, txsByProtocol.getOrDefault(Protocol.LEGACY, 0) + peer.txSent);
			}
		}

		int allTxs = txArrivalTimes.size();

		if (allTxs == 0) {
			return false;
		}

		// Measure the delays it took to reach majority of the nodes (based on receival time).
		ArrayList<Long> avgTxArrivalDelay = new ArrayList<>();
		Iterator it = txArrivalTimes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			// A workaround to avoid unchecked cast.
			ArrayList<?> ar = (ArrayList<?>) pair.getValue();
			ArrayList<Long> arrivalTimes = new ArrayList<>();

			if (ar.size() < (Network.size() - 1) * 0.99)  {
				// Don't bother printing results if relay is in progress (some nodes didn't receive
				// the transactions yet).
				continue;
			}

			for (Object x : ar) {
				arrivalTimes.add((Long) x);
			}

	   		Collections.sort(arrivalTimes);
			int percentile95Index = (int)(arrivalTimes.size() * 0.95);
			Long percentile95delay = (arrivalTimes.get(percentile95Index) - arrivalTimes.get(0));
			avgTxArrivalDelay.add(percentile95delay);
		}

		System.err.println("");
		System.err.println("");
		System.err.println("");
		System.err.println("-----------RESULTS--------");
		System.err.println("Relayed txs: " + allTxs);

		double avgMaxDelay = avgTxArrivalDelay.stream().mapToLong(val -> val).average().orElse(0.0);
		System.out.println("Avg max latency: " + avgMaxDelay);

		if (blackHoles != 0) {
			// Note that black holes are only useful to measure latency
			// impact, measuring/comparing bandwidth is currently not supported because it depends
			// on how exactly black holes operate (do they reconcile with empty sketches? or drop
			// sketches/requests on the floor?).
			return false;
		}
		System.out.println("Total bandwidth per tx");
		int shortInvsTotal = shortInvsByNodeType.getOrDefault(NodeType.REACHABLE, 0) + shortInvsByNodeType.getOrDefault(NodeType.PRIVATE, 0);
		int invsTotal = invsByNodeType.get(NodeType.REACHABLE) + invsByNodeType.get(NodeType.PRIVATE);
		System.out.println("INV items: " + (invsTotal + shortInvsTotal * 0.25)  / allTxs / (Network.size() - 1));

		System.out.println("");
		System.out.println("Total bandwidth per tx based on the protocol");
		int legacyNodes = Network.size() - reconcilingNodes - 1;
		if (legacyNodes > 0) {
			System.out.println("Legacy:");
			System.out.println("INV items: " + (invsByProtocol.get(Protocol.LEGACY) * 1.0 / allTxs / legacyNodes));
			System.out.println("TX items: " + (txsByProtocol.get(Protocol.LEGACY) * 1.0 / allTxs / legacyNodes));
		}
		if (reconcilingNodes > 0) {
			System.out.println("Reconciling:");
			System.out.println("INV items: " + (invsByProtocol.get(Protocol.ERLAY) + shortInvsTotal * 0.25) / allTxs / reconcilingNodes);
			System.out.println("Of them short invs: " + (shortInvsTotal * 0.25 / allTxs / reconcilingNodes));
			System.out.println("TX items: " + (txsByProtocol.get(Protocol.ERLAY) * 1.0 / allTxs / reconcilingNodes));

			double avgSuccessRecons = successRecons.stream().mapToInt(val -> val).average().orElse(0.0);
			if (avgSuccessRecons > 0) {
				System.out.println(avgSuccessRecons + " successful recons on average.");

				double avgFailedRecons = failedRecons.stream().mapToInt(val -> val).average().orElse(0.0);
				System.out.println(avgFailedRecons + " failed recons on average.");
			}
		}

		System.out.println("");
		System.out.println("Total bandwidth per tx based on reachability");
		int privateNodes = Network.size() - reachableNodes - 1;
		System.out.println("Reachable:");
		System.out.println("INV items: " + (invsByNodeType.get(NodeType.REACHABLE) * 1.0 / allTxs / reachableNodes));
		System.out.println("TX items: " + (txsByNodeType.get(NodeType.REACHABLE) * 1.0 / allTxs / reachableNodes));

		System.out.println("Private:");
		System.out.println("INV items: " + (invsByNodeType.get(NodeType.PRIVATE) * 1.0 / allTxs / privateNodes));
		System.out.println("TX items: " + (txsByNodeType.get(NodeType.PRIVATE) * 1.0 / allTxs / privateNodes));
		System.err.println("");
		return false;
	}
}
