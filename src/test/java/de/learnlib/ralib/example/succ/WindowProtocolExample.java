package de.learnlib.ralib.example.succ;

import de.learnlib.ralib.sul.examples.AbstractWindowProtocol;

public class WindowProtocolExample extends AbstractWindowProtocol{

	private Double seq = 0.0;
	private State state;
	
	public WindowProtocolExample(Double win) {
		super(win);
		this.state = State.CLOSED;
	}
	
	public WindowProtocolExample() {
		super();
		this.state = State.CLOSED;
	}
	
	public enum State{
		CLOSED,
		CONNECTED,
	}

    public Double IConnect() {
    	Double fresh = newFresh();
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
    
    public Double INEXT(Double seq) {
    	Double ret;
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
    
    public boolean ICurrent(Double seq) {
    	if (state == State.CONNECTED) 
    		if (equ(this.seq, seq)) 
    			return true;
    	return false;
    }
}
