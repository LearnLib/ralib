package de.learnlib.ralib.example.ineq;

public class BranchClass{

	private Double c = null;
	private Double b = null;
	private Double a = null;
	private boolean init = false;
	
	
	public BranchClass() {
		super();
	}
	
	

    //handling each Input

    /* a > b > c
     */
    public boolean IInit(Double a, Double b, Double c) {
    	boolean ret = false;
    	if (!this.init 
    			&& (c < b) && (b < a)) {
    		this.c = c;
    		this.b = b;
    		this.a = a;
    		ret = true;
    		this.init = true;
    	}
        return ret;
    }     
    
    /** the intervals shouldn't be merged **/
    public boolean ITestNoMerge(Double seq) {
    	boolean ret = false;
    	if (this.init) {
    		ret = (seq < this.c) || (seq == this.b) || (seq > this.c && seq < this.b) || (seq >= this.a);
    	}

    	return ret;
    }
}
