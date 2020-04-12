package de.learnlib.ralib.utils;

import org.testng.Assert;

public class SDTAssert {
	/**
	 * Hassle free SDT comparison using their string print-outs. 
	 * The idea is, we compare the strings minus any spaces which are a pain to get right.
	 * If comparison fails, we supply them to the assertEquals method (to get a proper message).
	 * 
	 * TODO remove the need for this method 
	 */
	public static void assertEquals(String actual, String expected) {
		if (!expected.replaceAll("\\s", "").equals(actual.replaceAll("\\s", "")) ) {
			Assert.assertEquals(actual, expected);
		}
	}
	
}
