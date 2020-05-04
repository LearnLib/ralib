package de.learnlib.ralib.example.sumc.inequality;


public class OneWayFreshTCPExample extends AbstractTCPExample{

	private Double seq = 0.0;
	
	public OneWayFreshTCPExample(Double win) {
		super(win);
	}
	
	public OneWayFreshTCPExample() {
		super();
		//configure();
	}

    public Double IConnect() {
    	Double fresh = newFresh();
    	if (state == State.CLOSED) {
    		seq = fresh;  
    		state = State.CONNECTING;
    	}
        return fresh;
    }
    
    public boolean ISYN(Double seq) {
    	boolean ret = false;
    	if (state == State.CONNECTING) {
    		if (this.seq.equals(seq)) {
    			ret = true;
    			state = State.SYN_SENT;
    		} else {
    			if (!inWin(this.seq, seq) && options.contains(Option.WIN_CONNECTING_TO_CLOSED)) {
    				state = State.CLOSED;
    			}
    		}
    	}
    	
    	return ret;
    }
    
    public boolean ISYNACK(Double ack) {
    	boolean ret = false;
    	if (state == State.SYN_SENT) {
    		if (succ(this.seq, ack)) {
    			ret = true;
    			this.seq = ack;
    			state = State.SYN_RECEIVED;
    		} else {
    			if (!inWin(this.seq, ack) && options.contains(Option.WIN_SYNSENT_TO_CLOSED)) {
    				state = State.CLOSED;
    			}
    			
    		}
    	}
    	return ret;
    }
    
    public boolean IACK(Double seq) {
    	boolean ret = false;
    	
    	if (state == State.SYN_RECEIVED) {
    		if (seq.equals(this.seq)) {
    			ret = true;
    			state = State.ESTABLISHED;
    		} else {
    			if(!inWin(this.seq, seq) && options.contains(Option.WIN_SYNRECEIVED_TO_CLOSED)) {
    				state = State.CLOSED;
    			}
    		}
    	} else {
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
