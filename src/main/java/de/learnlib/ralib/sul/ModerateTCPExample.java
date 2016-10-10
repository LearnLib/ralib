package de.learnlib.ralib.sul;

public class ModerateTCPExample {

	private static final Double win = 100.0;
	private Double clSeq = null;
	private Double svSeq = null;
	private State state = State.CLOSED;
	enum State{
		CLOSED,
		CONNECTING, // extra state
		SYN_SENT,
		SYN_RECEIVED,
		ESTABLISHED;
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
    		state = State.CONNECTING;
    	}
        return ret;
    }     
    
    public boolean ISYN(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.CONNECTING) {
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
//    			if (!inWin(clSeq, ack)) {
//    				state = State.CLOSED;
//    			}
    			
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
//    			if (!inWin(svSeq, ack)) {
//    				state = State.CLOSED;
//    			}
    			
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
