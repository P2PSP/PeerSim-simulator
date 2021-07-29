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

	public boolean execute() {
		// Track how many invs were sent. Reachable nodes are tracked by [0], private are tracked
		// by [1].
		int[] invsSent = new int[2];
		int[] shortInvsSent = new int[2];
		int[] txSent = new int[2];
		// Track reconciliation results across experiments.
		ArrayList<Integer> successRecons = new ArrayList<>();
		ArrayList<Integer> failedRecons = new ArrayList<>();
		// Track how soon transactions were propagating across the network.
		HashMap<Integer, ArrayList<Long>> txArrivalTimes = new HashMap<Integer, ArrayList<Long>>();
		int blackHoles = 0, reachableNodes = 0;
		for(int i = 1; i < Network.size(); i++) {
			Peer peer = (Peer) Network.get(i).getProtocol(pid);

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

			if (peer.isBlackHole) {
				++blackHoles;
				continue;
			}

			if (peer.isReachable) {
				invsSent[0] += peer.invsSent;
				shortInvsSent[0] += peer.shortInvsSent;
				txSent[0] += peer.txSent;
				++reachableNodes;
			} else {
				invsSent[1] += peer.invsSent;
				shortInvsSent[1] += peer.shortInvsSent;
				txSent[1] += peer.txSent;
			}

			successRecons.add(peer.successRecons);
			failedRecons.add(peer.failedRecons);
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

		// Print results.
		int allTxs = txArrivalTimes.size();

		if (allTxs == 0) {
			return false;
		}

		System.err.println("Relayed txs: " + allTxs);

		double avgMaxDelay = avgTxArrivalDelay.stream().mapToLong(val -> val).average().orElse(0.0);
		System.out.println("Avg max latency: " + avgMaxDelay);

		if (blackHoles == 0) {
			// Note that black holes are only useful to measure latency
			// impact, measuring/comparing bandwidth is currently not supported because it depends
			// on how exactly black holes operate (do they reconcile with empty sketches? or drop
			// sketches/requests on the floor?).

			System.out.println("Total bandwidth per tx:");
			System.out.println("INV items: " + (invsSent[0] + invsSent[1] +
				(shortInvsSent[0] + shortInvsSent[1]) * 0.25) / allTxs / (Network.size() - 1));
			System.out.println("Of them short invs: " + ((shortInvsSent[0] + shortInvsSent[1]) * 0.25) / allTxs / (Network.size() - 1));
			System.out.println("TX items: " + (txSent[0] + txSent[1]) * 1.0 / allTxs / (Network.size() - 1));

			System.out.println("An average reachable node spends the following bandwidth:");
			System.out.println("INV items: " + (invsSent[0] + shortInvsSent[0] * 0.25));
			System.out.println("TX items: " + txSent[0]);

			int privateNodes = Network.size() - 1 - reachableNodes;
			System.out.println("An average private node spends the following bandwidth:");
			System.out.println("INV items: " + (invsSent[1] + shortInvsSent[1] * 0.25));
			System.out.println("TX items: " + txSent[1]);

			double avgSuccessRecons = successRecons.stream().mapToInt(val -> val).average().orElse(0.0);
			if (avgSuccessRecons > 0) {
				System.out.println(avgSuccessRecons + " successful recons on average.");

				double avgFailedRecons = failedRecons.stream().mapToInt(val -> val).average().orElse(0.0);
				System.out.println(avgFailedRecons + " failed recons on average.");
			}
		}
		System.err.println("");
		return false;
	}
}
