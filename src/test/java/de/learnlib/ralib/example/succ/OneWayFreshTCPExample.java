package de.learnlib.ralib.example.succ;


import java.util.Random;

public class OneWayFreshTCPExample extends AbstractTCPExample{

	private static Random rand = new Random(0);
	private Double seq = 0.0;
	private static Double gen = 0.0;
	
	public OneWayFreshTCPExample(Double win) {
		super(win);
	}
	
	public OneWayFreshTCPExample() {
		super();
		configure();
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
    		state = State.SYN_SENT;
    	}
        return fresh;
    }
    
    private Double newFresh() {
    	return gen = gen + 10000;
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
//    			if (!inWin(this.seq, ack) && options.contains(Option.WIN_SYNRECEIVED_TO_CLOSED)) {
//    				state = State.CLOSED;
//    			}
    			
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
