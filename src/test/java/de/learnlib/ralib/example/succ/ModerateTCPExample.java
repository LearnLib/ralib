package de.learnlib.ralib.example.succ;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ModerateTCPExample {

	private static final Double DEFAULT_WIN = 100.0;
	private Double clSeq = null;
	private Double svSeq = null;
	private State state = State.CLOSED;
	private Set<Option> options;
	
	private final Double win;
	public ModerateTCPExample(Double win) {
		this.win = win;
		this.options = EnumSet.of(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
	}
	
	public ModerateTCPExample() {
		this(DEFAULT_WIN);
	}

	public void configure (Option ... options) {
		this.options = Arrays.asList(options).stream().collect(Collectors.toSet());
	}
	
	public static enum Option {
		WIN_SYNRECEIVED_TO_CLOSED,
		WIN_SYNSENT_TO_CLOSED;
	}
	
	enum State{
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

    //handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public boolean IConnect(Double initSeq, Double initAck) {
    	boolean ret = false;
    	if (state == State.CLOSED 
    			//&& !initSeq.equals(initAck) 
    			//&& !succ(initSeq, initAck) && !succ(initAck, initSeq)
    			//&& !inWin(initSeq, initAck) && !inWin(initAck, initSeq)
    			) {
    		this.clSeq = initSeq;
    		this.svSeq = initAck;
    		ret = true;
    		state = State.SYN_SENT;
    	}
        return ret;
    }     
    
    public boolean ISYN(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.SYN_SENT) {
    		if (seq.equals(clSeq)) {
    			ret = true;
    			state = State.SYN_RECEIVED;
    		}
    	}
    	
    	return ret;
    }
    
    public boolean ISYNACK(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.SYN_RECEIVED) {
    		if (seq.equals(svSeq) && succ(clSeq, ack)) {
    			ret = true;
    			clSeq = ack;
    			state = State.SYN_SENT;
    		} else {
    			if (!inWin(clSeq, ack) && options.contains(Option.WIN_SYNRECEIVED_TO_CLOSED)) {
    				state = State.CLOSED;
    			}
    			
    		}
    	}
    	return ret;
    }
    
    public boolean IACK(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.SYN_SENT) {
    		if (seq.equals(clSeq) && succ(svSeq, ack)) {
    			ret = true;
    			svSeq = ack;
    			state = State.ESTABLISHED;
    		} else {
    			if (!inWin(svSeq, ack)  && options.contains(Option.WIN_SYNSENT_TO_CLOSED)) {
    				state = State.CLOSED;
    			}
    			
    		}
    	}
    	
    	if (state == State.ESTABLISHED) {
    		if (seq.equals(clSeq) && succ(svSeq, ack) ||  
    				seq.equals(clSeq) && ack.equals(svSeq)) {
    			clSeq = seq;
    			svSeq = ack;
    			
    			ret = true;
    		} else if (
    				seq.equals(svSeq) && succ(clSeq, ack) ||
    				seq.equals(svSeq) && ack.equals(clSeq)) {
    					clSeq = ack;
    	    			svSeq = seq;
    	    			ret = true;
    		} 
    	}
    	
    	return ret;
    }
    
    public boolean succ(Double currentSeq, Double nextSeq) {
    	return nextSeq == currentSeq+1;
    }
    
    public boolean inWin(Double currentSeq, Double nextSeq) {
    	return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
    }
}
