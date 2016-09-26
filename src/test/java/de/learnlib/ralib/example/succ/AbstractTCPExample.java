package de.learnlib.ralib.example.succ;

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
		SYN_SENT,
		SYN_RECEIVED,
		ESTABLISHED;
		
		public State next() {
			switch(this) {
			case CLOSED: return SYN_SENT;
			case SYN_SENT: return SYN_RECEIVED;
			case SYN_RECEIVED: return ESTABLISHED;
			case ESTABLISHED: return CLOSED;
			}
			return null;
		}
	}
	
	   public boolean succ(Double currentSeq, Double nextSeq) {
	    	return nextSeq == currentSeq+1;
	    }
	    
	    public boolean inWin(Double currentSeq, Double nextSeq) {
	    	return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
	    }
}
