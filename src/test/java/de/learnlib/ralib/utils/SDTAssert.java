package de.learnlib.ralib.utils;

import org.testng.Assert;

public class SDTAssert {
	/**
	 * Hassle free SDT comparison using their string print-outs. 
	 * The idea is, we compare the strings minus any spaces.
	 * If comparison fails, we supply them to the assertEquals method (to get a proper message).
	 * The goal is to avoid failures due to mismatching space chars.
	 * 
	 * TODO remove the need for this method 
	 */
	public static void assertEquals(String actualSdtString, String expectedSdtString) {
		if (!expectedSdtString.replaceAll("\\s", "").equals(actualSdtString.replaceAll("\\s", "")) ) {
			Assert.assertEquals(actualSdtString, expectedSdtString);
		}
	}
}
