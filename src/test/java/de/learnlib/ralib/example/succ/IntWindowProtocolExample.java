package de.learnlib.ralib.example.succ;

import de.learnlib.ralib.sul.examples.IntAbstractWindowProtocol;

public class IntWindowProtocolExample extends IntAbstractWindowProtocol{

	private Integer seq = 0;
	private State state;
	
	public IntWindowProtocolExample(Integer win) {
		super(win);
		this.state = State.CLOSED;
	}
	
	public IntWindowProtocolExample() {
		super();
		this.state = State.CLOSED;
	}
	
	public enum State{
		CLOSED,
		CONNECTED,
	}

    public Integer IConnect() {
    	Integer fresh = newFresh();
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
    
    public Integer INEXT(Integer seq) {
    	Integer ret;
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
    
    public boolean ICurrent(Integer seq) {
    	if (state == State.CONNECTED) 
    		if (equ(this.seq, seq)) 
    			return true;
    	return false;
    }
}
