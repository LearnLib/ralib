package sut.implementation;

import java.util.ArrayList;
import java.util.List;

public class Serializer {
	
	// easy way to build responses
	private static OutputAction resp(String meth, Integer ...paramValues) {
		OutputAction output = null;
    	List<Parameter> params = new ArrayList<Parameter>();
    	int index = 0;
    	for(Integer paramValue : paramValues) {
    		params.add(new Parameter(paramValue, index));
    		index ++;
    	}
    	 output = new OutputAction(meth, params);
    	 return output;
	}
	
	public static String tomteInputToString(InputAction input) {
		String inputString;
	 	String methodName = input.getMethodName();
    	// first we remove tomte's I input char
	 	if (methodName.startsWith("I")) {
    		methodName = methodName.substring(1);
    	}
    	List<Parameter> params = input.getParameters();
    	
    	// packet input
    	if (params.size() > 0) {
    		String flags = methodName;
    		int seqNr = params.get(0).getValue();
    		int ackNr = params.get(1).getValue();
    		int payload = methodName.contains("P")?1:0;
    		inputString = concreteMessageToString(flags, int2ULong(seqNr), int2ULong(ackNr), payload );
    	}
    	
    	// tcp adapter input
    	else {
    		inputString = methodName.toLowerCase();
    	}
    	
    	return inputString;
    }

    private static String concreteMessageToString(String flags, long seqNr,
			long ackNr, int payloadLength) {
		StringBuilder sb = new StringBuilder();
		sb.append(flags).append(" ").append(seqNr).append(" ").append(ackNr).append(" [");
		for (int i = 0; i < payloadLength; i++) {
			sb.append("x");
		}
		sb.append("]");
		return sb.toString();
	}
    
	
	public static OutputAction stringToTomteOutput(String concreteResponse) {
    	String[] outputValues = concreteResponse.split(" ");
    	String methodName = "O" + outputValues[0].toUpperCase();
    	OutputAction output = null;
    	
    	// timeout or broken pipe
    	if (outputValues.length == 1) {
    		output = resp(methodName);
    	} else {
    		// SA 299 390 [] or AP 300 300 [x]
			Integer seqReceived = uLongString2Int(outputValues[1]);
			Integer ackReceived = uLongString2Int(outputValues[2]);
			String payload = outputValues[3];
			if (payload.length() < 2 || payload.charAt(0) != '[' || payload.charAt(payload.length() - 1) != ']') {
				throw new RuntimeException("Cannot parse packet '" + payload + "'");
			} 
			if (payload.length() > 3 ) {
				throw new RuntimeException("Warning: payload big '" + payload + "'");
			}
			Integer intPayload = payload.length() - 2;
			output = resp(methodName, seqReceived, ackReceived, intPayload);
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
    
    public static void main (String args [] ) {
    	System.out.println(Integer.toUnsignedString(-10) + " " + Integer.toUnsignedString(-30));
    }
}
