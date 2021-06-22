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
		// Track how many invs were sent.
		ArrayList<Integer> extraInvs = new ArrayList<>();
		ArrayList<Integer> shortInvs = new ArrayList<>();
		// Track reconciliation results across experiments.
		ArrayList<Integer> successRecons = new ArrayList<>();
		ArrayList<Integer> failedRecons = new ArrayList<>();
		// Track how soon transactions were propagating across the network.
		HashMap<Integer, ArrayList<Long>> txArrivalTimes = new HashMap<Integer, ArrayList<Long>>();
		for(int i = 1; i < Network.size(); i++) {
			Peer peer = (Peer) Network.get(i).getProtocol(pid);
			extraInvs.add(peer.extraInvs);
			shortInvs.add(peer.shortInvs);

			successRecons.add(peer.successRecons);
			failedRecons.add(peer.failedRecons);

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
		}

		// Measure the delays it took to reach majority of the nodes (based on receival time).
		ArrayList<Long> avgTxArrivalDelay = new ArrayList<>();
		Iterator it = txArrivalTimes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			// A workaround to avoid unchecked cast.
			ArrayList<?> ar = (ArrayList<?>) pair.getValue();
			ArrayList<Long> arrivalTimes = new ArrayList<>();
			for (Object x : ar) {
				arrivalTimes.add((Long) x);
			}

			if (arrivalTimes.size() < Network.size() - 1) {
				// Don't bother printing results if relay is in progress (some nodes didn't receive
				// all transactions yet).
				System.err.println("Transactions are still propagating");
				return false;
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

		double avgMaxDelay = avgTxArrivalDelay.stream().mapToLong(val -> val).average().orElse(0.0);
		System.out.println("Avg max latency: " + avgMaxDelay);

		double avgExtraInvs = extraInvs.stream().mapToInt(val -> val).average().orElse(0.0);
		System.out.println(avgExtraInvs / allTxs + " extra inv per tx on average.");

		double avgShortInvs = shortInvs.stream().mapToInt(val -> val).average().orElse(0.0);
		System.out.println(avgShortInvs / allTxs + " shortInvs per tx on average.");

		double avgSuccessRecons = successRecons.stream().mapToInt(val -> val).average().orElse(0.0);
		System.out.println(avgSuccessRecons + " successful recons on average.");

		double avgFailedRecons = failedRecons.stream().mapToInt(val -> val).average().orElse(0.0);
		System.out.println(avgFailedRecons + " failed recons on average.");

		return false;
	}
}