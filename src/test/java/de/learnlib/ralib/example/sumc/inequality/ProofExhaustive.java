package de.learnlib.ralib.example.sumcineq;

/**
 * An example which shows why we should use exhaustive suffixes, instead of suffixes with relations from the concrete suff/pref.
 */
public class ProofExhaustive {
	private Integer x = null;
	private Integer win = 100; 
	public boolean IMSG(Integer p) {
		if (x == null) {
			x = p;
			return true;
		} else {
			if (p>= x+1 && p<x+win) 
				return true;
			return false;
		}
	}
}
