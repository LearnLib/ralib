package de.learnlib.ralib.sul.examples;

public class ModerateFreshTCPExample extends AbstractTCPExample{

	private Double clSeq = null;
	private Double svSeq = null;
	private State state = State.CLOSED;

    public Double IConnect() {
    	Double ret = super.newFresh();
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
    
    public boolean ISYN(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.CONNECTING) {
    		if (seq.equals(clSeq)) {
    			ret = true;
    			state = State.SYN_SENT;
    		}
    	}
    	
    	return ret;
    }
    
    public boolean ISYNACK(Double seq, Double ack) {
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
    
    public boolean IACK(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.SYN_SENT) {
    		if (equ(seq, clSeq) && succ(svSeq, ack)) {
    			ret = true;
    			svSeq = ack;
    			state = State.ESTABLISHED;
    		} 
    	}
    	
    	if (state == State.ESTABLISHED) {
    		if (equ(seq, clSeq) && succ(svSeq, ack) ||  
    				equ(seq, clSeq) && equ(svSeq, ack)) {
    			clSeq = seq;
    			svSeq = ack;
    			
    			ret = true;
    		} else if (
    				equ(seq, svSeq) && succ(clSeq, ack) ||
    				equ(seq, svSeq) && equ(ack, clSeq)) {
    					clSeq = ack;
    	    			svSeq = seq;
    	    			ret = true;
    		} 
    	}
    	
    	return ret;
    }
}
