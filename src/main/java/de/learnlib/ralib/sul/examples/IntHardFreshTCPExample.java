package de.learnlib.ralib.sul.examples;


// mimics the model of the windows server learned in 2014
public class IntHardFreshTCPExample extends IntAbstractTCPExample{

	private Integer clSeq = null;
	private Integer svSeq = null;
	private State state = State.CLOSED;
	// if true, the looping variable makes it always possible to transition out of the CLOSED state by a Connect. 

    public IntHardFreshTCPExample(Integer window) {
		super(window);
	}
    
    
    public static interface Response {
    	
    }
    
    public class Packet implements Response{
    	public final FlagConfig flags;
    	public final Integer seqNum;
    	public final Integer ackNum;
		public Packet(FlagConfig flags, Integer seqNum, Integer ackNum) {
			super();
			this.flags = flags;
			this.seqNum = seqNum;
			this.ackNum = ackNum;
		} 
    }
    
    public class Timeout implements Response {
    	
    }
    
    public IntHardFreshTCPExample() {
    	super();
    }

	public Response ISYN(Integer seq, Integer ack) {
    	if (state == State.CLOSED) {
    		clSeq = seq;
    		Integer rseq = super.newFresh();
    		svSeq = rseq;
    		Integer rack = clSeq+1;
    		state = State.SYN_RECEIVED;
    		return new Packet(FlagConfig.SYNACK, rseq, rack);
    	}
    	if (state == State.CLOSEWAIT) {
    		if (equ(clSeq, seq)) 
    			state = State.CLOSED;
    	}
    	
    	return new Timeout();
    }
    
    public Response IACK(Integer seq, Integer ack) {
    	if (state == State.SYN_RECEIVED) {
    		if (succ(clSeq, seq) && succ(svSeq, ack)) {
    			svSeq = ack;
    			clSeq = seq;
    			state = State.ESTABLISHED;
    		} else {
    			if (succ(clSeq, seq) && !succ(svSeq, ack)) 
    				return new Packet(FlagConfig.RST, ack, ack);
    		} 
    	}
    	if (state == State.CLOSEWAIT || state == State.ESTABLISHED) {
    		if (!equ(clSeq, seq)) {
    			return new Packet(FlagConfig.ACK, this.svSeq, this.clSeq);
    		}
    	}
    	
    	return new Timeout();
    }
    
    public Response IPSHACK(Integer seq, Integer ack) {
    	if (state == State.ESTABLISHED) {
    		if (equ(clSeq, seq) && equ(svSeq, ack)) {
    			clSeq = clSeq+1;
    			return new Packet(FlagConfig.ACK, svSeq, clSeq);
    		} 
    	}
    	
    	return new Timeout();
    }

    public Response IFINACK(Integer seq, Integer ack) {
    	if (state == State.ESTABLISHED) {
    		if (equ(clSeq, seq) && equ(svSeq, ack)) {
    			state = State.CLOSEWAIT;
    			return new Packet(FlagConfig.ACK, this.newFresh(), this.newFresh());
    		} 
    	}
    	
    	if (state == State.SYN_RECEIVED) {
    		if (succ(clSeq, seq) && succ(svSeq, ack)) {
    			svSeq = ack;
    			clSeq = seq;
    			state = State.CLOSEWAIT;
    			return new Packet(FlagConfig.ACK, svSeq, clSeq);
    		}
    	}
    	
    	if (state == State.CLOSEWAIT || state == State.ESTABLISHED) {
    		if (!equ(clSeq, seq)) {
    			return new Packet(FlagConfig.ACK, this.svSeq, this.clSeq);
    		}
    	}
    	
    	return new Timeout();
    }

	public void configure(Option[] options) {
	}
}
