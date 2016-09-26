package de.learnlib.ralib.example.succ;

public class OneWayTCPExample extends AbstractTCPExample{

	private Double seq = null;
	
	public OneWayTCPExample(Double win) {
		super(win);
	}
	
	public OneWayTCPExample() {
		super();
	}

    //handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
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
    		if (succ(this.seq, seq)) {
    			ret = true;
    			state = State.SYN_RECEIVED;
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
    			state = State.SYN_SENT;
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
    	if (state == State.SYN_SENT) {
    		if (seq.equals(seq)) {
    			ret = true;
    			state = State.ESTABLISHED;
    		} 
    	}
    	
    	if (state == State.ESTABLISHED) {
    		if (this.seq.equals(seq)) {
    			ret = true;
    		} 
    	}
    	
    	return ret;
    }
    
}
