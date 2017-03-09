package de.learnlib.ralib.sul.examples;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class IntAbstractTCPExample extends IntAbstractWindowProtocol{
	protected State state;
	protected Set<Option> options;
	
	public IntAbstractTCPExample(Integer win) {
		super(win);
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
		state = State.CLOSED;
	}
	
	public IntAbstractTCPExample() {
		super();
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
		state = State.CLOSED;
	}
	
	public void configure (Option ... options) {
		this.options = Arrays.asList(options).stream().collect(Collectors.toSet());
	}
	
	public static enum Option {
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
		// connect...
		CONNECTING, // special state after a connect call has been received, before issuing of a SYN
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
