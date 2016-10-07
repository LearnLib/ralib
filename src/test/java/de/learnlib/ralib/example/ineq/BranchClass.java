package de.learnlib.ralib.example.ineq;

public class BranchClass{

	private Double small = null;
	private Double mid = null;
	private Double big = null;
	private boolean init = false;
	
	
	public BranchClass() {
		super();
	}
	
	

    //handling each Input

    /* a > b > c
     */
    public boolean IInit(Double small, Double mid, Double big) {
    	boolean ret = false;
    	if (!this.init 
    			&& (small < mid) && (mid < big)) {
    		this.small = small;
    		this.mid = mid;
    		this.big = big;
    		ret = true;
    		this.init = true;
    	}
        return ret;
    }     
    
    
    /** the none of the intervals should be merged **/
    public boolean ITestNoMerge(Double seq) {
    	boolean ret = false;
    	if (this.init) {
    		ret = (seq < this.small) || 
    				(seq.equals(this.mid)) || 
    				(seq > this.small && seq < this.mid) || 
    				(seq >= this.big);
    	}

    	return ret;
    }
    
    
    /** merge the last two (incl. the bigger interval) **/
    public boolean ITestMergeLastTwo(Double seq) {
    	boolean ret = false;
    	if (this.init) {
    		ret = (seq.equals(this.small)) || (seq > this.mid);
    	}

    	return ret;
    }

    /** merge the first two (incl. the smaller interval) **/
    public boolean ITestMergeFirstTwo(Double seq) {
    	boolean ret = false;
    	if (this.init) {
    		ret = (seq < this.small) || (seq.equals(this.mid));
    	}

    	return ret;
    }
    
    /** merge result should be == and != **/
    public boolean ITestMergeEquDiseq(Double seq) {
    	boolean ret = false;
    	if (this.init) {
    		ret = seq.equals(this.mid);
    	}

    	return ret;
    }
    
    /** can be used in the suffix in combination with some of the other suffixes that merging still 
     * reduces to the same initial branch, regardless of the suffix length
     * **/
    public boolean ITestIsMax(Double seq) {
    	boolean ret = false;
    	if (this.init) {
    		ret = seq.equals(this.mid);
    	}
    	return ret;
    }
}
