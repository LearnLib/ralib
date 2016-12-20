package de.learnlib.ralib.sul.examples;

public class IntOneWayTCPExample extends IntAbstractTCPExample{

	private Integer seq = null;
	
	public IntOneWayTCPExample(Integer win) {
		super(win);
	}
	
	public IntOneWayTCPExample() {
		super();
	}

    //handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public boolean IConnect(Integer initSeq) {
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
    
    public boolean ISYN(Integer seq) {
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
    
    public boolean ISYNACK(Integer ack) {
    	boolean ret = false;
    	if (state == State.SYN_RECEIVED) {
    		if (succ(this.seq, ack)) {
    			ret = true;
    			this.seq = ack;
    			state = State.ESTABLISHED;
    		} else {
//    			if (!inWin(this.seq, ack) && options.contains(Option.WIN_SYNRECEIVED_TO_CLOSED)) {
//    				state = State.CLOSED;
//    			}
    			
    		}
    	}
    	return ret;
    }
    
    public boolean IACK(Integer seq) {
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
    
    
    public boolean IFINACK(Integer seq) {
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
