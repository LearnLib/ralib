package de.learnlib.ralib.sul.examples;

public class ProofDiscreteEquMerge {
	private Integer x = null;
	private Integer y = null; 
	public boolean IMSG(Integer p) {
		if (x == null) {
			x = p;
			y = p;
			return true;
		} else 
			if (p== x+1) {
				x = x+1;
				return true;
			} else 
				if (p >= y && p <= x) 
					return true;
				else 
					return false;
	}
}
