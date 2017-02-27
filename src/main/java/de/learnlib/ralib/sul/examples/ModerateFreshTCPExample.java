package de.learnlib.ralib.sul.examples;

/*
 * Before learning this, ensure first that the window size in AbstractWindowProtocol coincides
 * with the sumc supplied to prevent canonization issues
 */
public class ModerateFreshTCPExample extends AbstractTCPExample{

	private Double clSeq = null;
	private Double svSeq = null;
	private State state = State.CLOSED;
	// if true, the looping variable makes it always possible to transition out of the CLOSED state by a Connect. 
	private boolean looping = true;

    public Double IConnect() {
    	Double ret = super.newFresh();
    	if (state == State.CLOSED && 
    			(looping || this.clSeq == null) 
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
    			state = State.SYN_RECEIVED;
    		} else {
    			if(!inWin(this.clSeq, ack) && options.contains(Option.WIN_SYNSENT_TO_CLOSED)) 
    				state = State.CLOSED;
    			
    		}
    	}
    	return ret;
    }
    
    public boolean IACK(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.SYN_RECEIVED) {
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
    		} 
    	}
    	
    	return ret;
    }
    

    public boolean IFINACK(Double seq, Double ack) {
    	boolean ret = false;
    	if (state == State.ESTABLISHED) {
    		if (seq.equals(clSeq) && succ(svSeq, ack) ||  
    				seq.equals(clSeq) && ack.equals(svSeq)) {
    			state = State.CLOSEWAIT;
    			
    			ret = true;
    		} else if (
    				seq.equals(svSeq) && succ(clSeq, ack) ||
    				seq.equals(svSeq) && ack.equals(clSeq)) {
    				state = State.CLOSEWAIT;
    	    		ret = true;
    		} 
    	}
    	
    	return ret;
    }
    
    public boolean ICLOSE() {
    	if (state == State.CLOSEWAIT) {
    		state = State.CLOSED;
    		return true;
    	}
    	return false;
    }
    
}
