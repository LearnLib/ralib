package de.learnlib.ralib.sul.examples;

public class LongWindowProtocolExample extends LongAbstractWindowProtocol{

	private Long seq = 0L;
	private State state;
	
	public LongWindowProtocolExample(Long win) {
		super(win);
		this.state = State.CLOSED;
	}
	
	public LongWindowProtocolExample() {
		super();
		this.state = State.CLOSED;
	}
	
	public enum State{
		CLOSED,
		CONNECTED,
	}

    public Long IConnect() {
    	Long fresh = newFresh();
    	if (state == State.CLOSED 
    			//&& !initSeq.equals(initAck) 
    			//&& !succ(initSeq, initAck) && !succ(initAck, initSeq)
    			//&& !inWin(initSeq, initAck) && !inWin(initAck, initSeq)
    			) {
    		this.seq = fresh;  
    		state = State.CONNECTED;
    	}
        return fresh;
    }
    
    public Long INEXT(Long seq) {
    	Long ret;
    	if (state == State.CONNECTED) {
    		if (equ(this.seq, seq)) {
    			ret = seq + 1;
    			this.seq = seq + 1;
    		} else {
    			ret = newFresh();
    			if (!inWin(this.seq, seq)) {
    				state = State.CLOSED;
    			}
    		}
    	} else {
    		ret = newFresh();
    	}
    	
    	return ret;
    }
    
    public boolean ICurrent(Long seq) {
    	if (state == State.CONNECTED) 
    		if (equ(this.seq, seq)) 
    			return true;
    	return false;
    }
}
