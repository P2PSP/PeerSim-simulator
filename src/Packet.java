package sim.src;

public class Packet 
{
	/**
	 * Node index of the sender
	 */
	
	public int sender;
	/**
	 * Index (sequence number of the packet)
	 */
	public int index;
	
	/**
	 * Node index of the node this message should be resent to, if the sender is Source
	 * The type is Integer because it might be null ("don't resend to anyone")
	 */
	public Integer resendTo;

	public Packet(int source, int index, Integer resendTo)
	{
		this.sender = source;
		this.index = index;
		this.resendTo = resendTo;
	}
}
