package de.learnlib.ralib.example.succ;

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
	
	public boolean ISEND(Integer p) {
		if (y != null && p == y+1) {
			y = y+1;
			return true;
		}
		return false;
	}
	
	public boolean IACK(Integer p) {
		if (x == null) {
			x = p;
			y = p;
			return true;
		} else if (p>x && p<=y) {
			this.x = p;
			return true;
		} else {
			return false;
		}
		
	}
}
