package de.learnlib.ralib.sul.examples;

public class SimpleTCPExample {

	private static final Double win = 100.0;
	private Double seqNum = null;

    //handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public boolean IConnect(Double initSeq) {
    	boolean ret = false;
    	if (seqNum == null) {
    		this.seqNum = initSeq;
    		ret = true;
    	}
        return ret;
    }     
    
    public boolean IMSG(Double nextSeq) {
    	boolean ret = false;
    	if (seqNum != null) {
    		if (nextSeq.equals(seqNum + 1)) {
    			seqNum = nextSeq;
    			ret = true;
    		} else {
    			if (nextSeq > seqNum + 1 && nextSeq < seqNum + win) {
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
