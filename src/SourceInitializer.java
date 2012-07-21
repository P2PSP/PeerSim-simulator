package sim.src;

import peersim.config.*;
import peersim.core.*;

public class SourceInitializer implements Control
{
	public static final int sourceIndex = 0;
	
	private static final String PAR_PROT = "protocol";
	private final int pid;
	
	public SourceInitializer(String prefix)
	{
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	
	@Override
	public boolean execute() 
	{
		//set the Source pid
		Source.pidSource = pid;
		
		//set node 0 as source
		Node nodo = Network.get(sourceIndex);
		((Source) Network.get(sourceIndex).getProtocol(pid)).isSource = true;
		
		//set other nodes as not source
		for(int i = 1; i < Network.size()-1; i++)
			((Source) Network.get(i).getProtocol(pid)).isSource = false;

		return true;
	}
	

}
