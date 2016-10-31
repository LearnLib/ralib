package de.learnlib.ralib.sul;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstractTCPExample {
	private static final Double DEFAULT_WIN = 100.0;
	protected State state = State.CLOSED;
	protected Set<Option> options;
	
	public AbstractTCPExample(Double win) {
		this.win = win;
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
	}
	
	public AbstractTCPExample() {
		this.win = DEFAULT_WIN;
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
	}
	
	protected final Double win;

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
		CLOSED,
		// connect...
		CONNECTING, // special state after a connect call has been received, before issuing of a SYN
		// s(10,0)
		SYN_SENT, 
		// sa(20,11)
		SYN_RECEIVED, // special state after a SYN+ACK has been received, before issuing of an ACK  
		// a(11, 21)
		ESTABLISHED,
		FIN_WAIT_1,
		TIME_WAIT;
	}
	
	   public boolean succ(Double currentSeq, Double nextSeq) {
	    	return nextSeq == currentSeq+1;
	   }
	   
	   public boolean equ(Double currentSeq, Double nextSeq) {
	    	return nextSeq.equals(currentSeq);
	    }
	    
	    public boolean inWin(Double currentSeq, Double nextSeq) {
	    	return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
	    }
}
