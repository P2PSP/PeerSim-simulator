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
		
package sim.src;

import peersim.config.*;
import peersim.core.*;
import peersim.util.*;

/**
* Print statistics over a vector. The vector is defined by a protocol,
* specified by {@value #PAR_PROT}, that has to  implement
* {@link SingleValue}.
* Statistics printed are: min, max, number of samples, average, variance,
* number of minimal instances, number of maximal instances (using
* {@link IncrementalStats#toString}).
* @see IncrementalStats
*/
public class PeerObserver implements Control 
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
	
	private  int cycle_length;
	
	/**
	 * Standard constructor that reads the configuration parameters.
	 * Invoked by the simulation engine.
	 * @param name the configuration prefix for this class
	 */
	public PeerObserver(String name) {
		this.name = name;
		pid = Configuration.getPid(name + "." + PAR_PROT);
		cycle_length = Configuration.getInt("CYCLE");
	}
	
	public boolean execute() {
		Peer peer;
		
		System.out.println("---------------------------------------------------------------------------------");
		System.out.println("This is PeerObserver. Buffers...");
		for(int i = 1; i < Network.size(); i++) {
			peer = (Peer) Network.get(i).getProtocol(pid);

			System.out.print("Node "+i+" buffer: ");
			for(int j = 0; j < peer.buffer.length; j++) {
				if(peer.buffer[j] == null)
					System.out.print("   - ");
				else
					System.out.print(peer.buffer[j].getInteger() + " - ");
			}
			System.out.println();
		}
		System.out.println("---------------------------------------------------------------------------------");

		return false;
	}
}