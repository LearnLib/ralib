package de.learnlib.ralib.example.succ;

import java.util.Random;

public class SimpleTCPExample {

	private static Random random = new Random();
	private static Double win = 100.0;
	private Double seqNum = null;

    //handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public Double IConnect() {
    	Double ret = null;
    	if (seqNum == null) {
    		Double seqNum = new Double(random.nextInt(10000000));
    		ret = seqNum;
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
    			if (nextSeq > seqNum + 1 && nextSeq <= seqNum + win) {
    				ret = true;
    			}
    		}
    	}
    	
    	return ret;
    }
}
