package de.learnlib.ralib.sul.tcp;

import de.learnlib.ralib.tools.sulanalyzer.ConcreteInput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteOutput;

public class Serializer {
	
	public static String concreteInputToString(ConcreteInput input) {
		String inputString;
		String methodName = input.getMethodName();
		// first we remove tomte's I input char
		if (methodName.startsWith("I")) {
			methodName = methodName.substring(1);
		}
		Object [] params = input.getParameterValues();

		// packet input
		if (params.length > 0) {
			String flags = methodName;
			int seqNr = (int) params[0];
			int ackNr = (int) params[1];
			int payload = methodName.contains("P") ? 1 : 0;
			inputString = concreteMessageToString(flags, int2ULong(seqNr), int2ULong(ackNr), payload);
		}

		// tcp adapter input
		else {
			inputString = methodName.toLowerCase();
		}

		return inputString;
	}

	private static String concreteMessageToString(String flags, long seqNr, long ackNr, int payloadLength) {
		StringBuilder sb = new StringBuilder();
		sb.append(flags).append(" ").append(seqNr).append(" ").append(ackNr).append(" [");
		for (int i = 0; i < payloadLength; i++) {
			sb.append("x");
		}
		sb.append("]");
		return sb.toString();
	}

	public static ConcreteOutput stringToConcreteOutput(String concreteResponse) {
		String[] outputValues = concreteResponse.split(" ");
		String methodName = "O" + outputValues[0].toUpperCase();
		ConcreteOutput output = null;

		// timeout or broken pipe
		if (outputValues.length == 1) {
			output = new ConcreteOutput(methodName);
		} else {
			// SA 299 390 [] or AP 300 300 [x]
			Integer seqReceived = uLongString2Int(outputValues[1]);
			Integer ackReceived = uLongString2Int(outputValues[2]);
			String payload = outputValues[3];
			if (payload.length() < 2 || payload.charAt(0) != '[' || payload.charAt(payload.length() - 1) != ']') {
				throw new RuntimeException("Cannot parse packet '" + payload + "'");
			}
			if (payload.length() > 3) {
				throw new RuntimeException("Warning: payload big '" + payload + "'");
			}
			Integer intPayload = payload.length() - 2;
			// we'll ignore the payload for now
			output = new ConcreteOutput(methodName, seqReceived, ackReceived);
		}
		return output;
	}
	
	
	protected static int uLongString2Int(String num){
		return Long.valueOf(num).intValue();
	}
	
	   /**
     * Reads an int (which is always signed in java) as unsigned,
     * stored in a long
     * @param x
     * @return
     */
    protected static long int2ULong(int x) {
        return Integer.toUnsignedLong(x);
    }
    
}
