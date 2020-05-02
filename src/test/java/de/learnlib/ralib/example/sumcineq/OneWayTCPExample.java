package de.learnlib.ralib.example.sumcineq;


public class OneWayTCPExample extends AbstractTCPExample{

	private Double seq = null;
	
	public OneWayTCPExample(Double win) {
		super(win);
	}
	
	public OneWayTCPExample() {
		super();
	}

    public boolean IConnect(Double initSeq) {
    	boolean ret = false;
    	if (state == State.CLOSED 
    			//&& !initSeq.equals(initAck) 
    			//&& !succ(initSeq, initAck) && !succ(initAck, initSeq)
    			//&& !inWin(initSeq, initAck) && !inWin(initAck, initSeq)
    			) {
    		this.seq = initSeq;
    		ret = true;
    		state = State.SYN_SENT;
    	}
        return ret;
    }     
    
    public boolean ISYN(Double seq) {
    	boolean ret = false;
    	if (state == State.SYN_SENT) {
    		if (this.seq.equals(seq)) {
    			ret = true;
    			state = State.SYN_RECEIVED;
    		} else {
    			if (!inWin(this.seq, seq) && options.contains(Option.WIN_SYNSENT_TO_CLOSED)) {
    				state = State.CLOSED;
    			}
    		}
    	}
    	
    	return ret;
    }
    
    public boolean ISYNACK(Double ack) {
    	boolean ret = false;
    	if (state == State.SYN_RECEIVED) {
    		if (succ(this.seq, ack)) {
    			ret = true;
    			this.seq = ack;
    			state = State.ESTABLISHED;
    		} else {
    			if (!inWin(this.seq, ack) && options.contains(Option.WIN_SYNRECEIVED_TO_CLOSED)) {
    				state = State.CLOSED;
    			}
    		}
    	}
    	return ret;
    }
    
    public boolean IACK(Double seq) {
    	boolean ret = false;
    	
    	if (state == State.ESTABLISHED) {
    		if (equ(this.seq, seq)) {
    			this.seq = seq;
    			ret = true;
    		} 
    	}
    	
    	if (state == State.FIN_WAIT_1) {
    		if (succ(this.seq, seq)) {
    			state = State.TIME_WAIT;
    			ret = true;
    		} 
    	}
    	
    	return ret;
    }
    
    
    public boolean IFINACK(Double seq) {
    	boolean ret = false;
    	
    	if (state == State.ESTABLISHED) {
    		if (equ(this.seq, seq)) {
    			state = State.FIN_WAIT_1;
    			this.seq = seq;
    			ret = true;
    		} 
    	}
    	
    	return ret;
    }
}