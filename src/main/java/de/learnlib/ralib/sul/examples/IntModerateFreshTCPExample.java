package de.learnlib.ralib.sul.examples;

public class IntModerateFreshTCPExample extends IntAbstractTCPExample{

	private Integer clSeq = null;
	private Integer svSeq = null;
	private State state = State.CLOSED;

    public Integer IConnect() {
    	Integer ret = super.newFresh();
    	if (state == State.CLOSED 
    			//&& !initSeq.equals(initAck) 
    			//&& !succ(initSeq, initAck) && !succ(initAck, initSeq)
    			//&& !inWin(initSeq, initAck) && !inWin(initAck, initSeq)
    			) {
    		this.clSeq = ret;
    		state = State.CONNECTING;
    	}
        return ret;
    }     
    
    public boolean ISYN(Integer seq, Integer ack) {
    	boolean ret = false;
    	if (state == State.CONNECTING) {
    		if (seq.equals(clSeq)) {
    			ret = true;
    			state = State.SYN_SENT;
    		}
    	}
    	
    	return ret;
    }
    
    public boolean ISYNACK(Integer seq, Integer ack) {
    	boolean ret = false;
    	if (state == State.SYN_SENT) {
    		if (succ(clSeq, ack)) {
    			ret = true;
    			clSeq = ack;
    			svSeq = seq;
    			state = State.SYN_SENT;
    		} else {
    			if(!inWin(this.clSeq, ack) && options.contains(Option.WIN_SYNSENT_TO_CLOSED)) 
    				state = State.CLOSED;
    			
    		}
    	}
    	return ret;
    }
    
    public boolean IACK(Integer seq, Integer ack) {
    	boolean ret = false;
    	if (state == State.SYN_SENT) {
    		if (seq.equals(clSeq) && succ(svSeq, ack)) {
    			ret = true;
    			svSeq = ack;
    			state = State.ESTABLISHED;
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
}
