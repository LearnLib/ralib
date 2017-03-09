package de.learnlib.ralib.example.succ;

import de.learnlib.ralib.sul.examples.IntAbstractWindowProtocol;

public class IntSimpleTCPExample extends IntAbstractWindowProtocol{

	private Integer seqNum = null;

    public boolean IConnect(Integer initSeq) {
    	boolean ret = false;
    	if (seqNum == null) {
    		this.seqNum = initSeq;
    		ret = true;
    	}
        return ret;
    }     
    
    public boolean IMSG(Integer nextSeq) {
    	boolean ret = false;
    	if (seqNum != null) {
    		if (super.succ(seqNum, nextSeq)) {
    			seqNum = nextSeq;
    			ret = true;
    		} else {
    			if (super.inWin(seqNum, nextSeq)) {
    				ret = false;
    			} else {
    				ret = false;
    				seqNum = null;
    			}
    		}
    	}
    	
    	return ret;
    }
}
