package txrelaysim.src;

import txrelaysim.src.helpers.*;

import java.util.ArrayList;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.CommonState;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;


public class Source implements CDProtocol, EDProtocol
{
	public static int pidSource;
	public static int tps;

	public boolean isSource = false;
	public int txId = 0;
	private ArrayList<Node> peerList;

	public Source(String prefix) {
		this.peerList = new ArrayList<>();
	}

	@Override
	public void nextCycle(Node node, int pid) {
		Node recipient;
		int nextNodeIndex;

		if (isSource == false)
			return;

		if (CommonState.getEndTime() < CommonState.getTime() + 40 * 1000) {
			// if the experiment is over soon, stop issuing transactions and let existing propagate.
			return;
		}

		int randomNumberOfTxs = CommonState.r.nextInt(this.tps * 2); // anything from 0 to tps * 2.

		for (int i = 0; i < randomNumberOfTxs; ++i) {
			txId++;
			int randomRecipientIndex = CommonState.r.nextInt(peerList.size() - 1) + 1;
			recipient = peerList.get(randomRecipientIndex);
			IntMessage inv = new IntMessage(SimpleEvent.INV, node, txId);
			((Transport)recipient.getProtocol(FastConfig.getTransport(pid))).send(node, recipient, inv, Peer.pidPeer);
		}
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		return;
	}

	public Object clone() {
		return new Source("");
	}

	public void addPeer(Node peer) {
		peerList.add(peer);
	}

}