package de.learnlib.ralib.example.sumc.inequality;

public interface TCPExample {
	public static enum Option {
		/**
		 * Activates transition from CONNECTING to CLOSED on an out of window client seq (where applicable).
		 */
		WIN_CONNECTING_TO_CLOSED,
		/**
		 * Activates transition from SYN_RECEIVED to CLOSED on an out of window client seq
		 */
		WIN_SYNRECEIVED_TO_CLOSED,
		/**
		 * Activates transition from SYN_SENT to CLOSED on an out of window server seq
		 */
		WIN_SYNSENT_TO_CLOSED;
	}
	
	public enum State{
	//	INIT,
		// connect(10)...
		CONNECTING, // special state after a connect call has been received, before issuing of a SYN
		// connect call determines the sequence number to use (supplied as input parameter or as a fresh output value)
		// s(10,0)
		SYN_SENT, 
		// sa(20,11)
		SYN_RECEIVED, // special state after a SYN+ACK has been received, before issuing of an ACK  
		// a(11, 21)
		ESTABLISHED,
		FIN_WAIT_1,
		TIME_WAIT,		
		CLOSEWAIT,
		CLOSED;
	}
	
	public enum FlagConfig {
		SYNACK,
		ACK,
		FINACK,
		RST,
		RSTACK
	}
}
