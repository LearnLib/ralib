package de.learnlib.ralib.sul.examples;

public class Test {
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
