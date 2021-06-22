package txrelaysim.src;

import peersim.config.*;
import peersim.core.*;

public class SourceInitializer implements Control
{
	public static final int sourceIndex = 0;

	private static final String PAR_PROT = "protocol";
	private final int pid;
	private int tps;

	public SourceInitializer(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		tps = Configuration.getInt(prefix + ".tps");
	}

	@Override
	public boolean execute() {
		// Set the Source pid.
		Source.pidSource = pid;

		// Set node 0 as source.
		((Source) Network.get(sourceIndex).getProtocol(pid)).isSource = true;
		((Source) Network.get(sourceIndex).getProtocol(pid)).tps = tps;

		//set other nodes as not source.
		for(int i = 1; i < Network.size()-1; i++)
			((Source) Network.get(i).getProtocol(pid)).isSource = false;

		// Source connects to some nodes.
		Node source = Network.get(0);
		int sourceConns = 0;
		while (sourceConns < 20) {
			int randomNodeIndex = CommonState.r.nextInt(Network.size() - 1) + 1;
			Node node = Network.get(randomNodeIndex);
			((Source)source.getProtocol(pid)).addPeer(node);
			++sourceConns;
		}

		return true;
	}
}
