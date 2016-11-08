package de.learnlib.ralib.sul;

import java.util.Random;

public class WindowProtocolExample{

	private static Random rand = new Random(0);
	private Double seq = 0.0;
	private Double win;
	private State state;
	private Double gen = 0.0;
	//private static Double INVALID = 0.0;
	private static Double DEFAULT_WIN = 1000.0;
	
	public WindowProtocolExample(Double win) {
		this.win = win;
		this.state = State.CLOSED;
	}
	
	public WindowProtocolExample() {
		this(DEFAULT_WIN);
	}
	
	public enum State{
		CLOSED,
		CONNECTED,
	}

    //handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public Double IConnect() {
    	Double fresh = newFresh();
    	if (state == State.CLOSED 
    			//&& !initSeq.equals(initAck) 
    			//&& !succ(initSeq, initAck) && !succ(initAck, initSeq)
    			//&& !inWin(initSeq, initAck) && !inWin(initAck, initSeq)
    			) {
    		this.seq = fresh;  
    		state = State.CONNECTED;
    	}
        return fresh;
    }
    
    private Double newFresh() {
    	return gen = gen + 1000000;
    }
    
    public Double INEXT(Double seq) {
    	Double ret;
    	if (state == State.CONNECTED) {
    		if (equ(this.seq, seq)) {
    			ret = seq + 1;
    			this.seq = seq + 1;
    		} else {
    			ret = newFresh();
    			if (!inWin(this.seq, seq)) {
    				state = State.CLOSED;
    			}
    		}
    	} else {
    		ret = newFresh();
    	}
    	
    	return ret;
    }
    
    public boolean ICurrent(Double seq) {
    	if (state == State.CONNECTED) 
    		if (equ(this.seq, seq)) 
    			return true;
    	return false;
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
