package de.learnlib.ralib.sul.tcp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class TCPConfig {
	private static String PROPERTIES_FILE = "tcp.properties";
	
	public static TCPConfig readPropFile() throws FileNotFoundException, IOException {
		Properties simProperties = new Properties();
    	simProperties.load(new FileInputStream(PROPERTIES_FILE));
		Integer senderPortNumber = Integer.valueOf(getProperty(simProperties, "senderPort"));
		String senderAddress = getProperty(simProperties, "senderAddress");
		boolean resetHalfSpace = Boolean.valueOf((String)simProperties.getOrDefault("resetHalfSpace", "false"));
		return new TCPConfig(senderAddress, senderPortNumber, resetHalfSpace);
	}
	
	public TCPConfig(String senderAddress, int senderPortNumber) {
		this(senderAddress, senderPortNumber, false);
	}

	public TCPConfig(String senderAddress, Integer senderPortNumber, boolean resetHalfSpace) {
		this.senderPortNumber = senderPortNumber;
		this.senderAddress = senderAddress;
		this.resetHalfSpace = resetHalfSpace;
	}

	public final int senderPortNumber;
	public final String senderAddress;
	// reset if outside of half space
	public final boolean resetHalfSpace;
	
	private static String getProperty(Properties propFile, String prop) {
	  	if (propFile.containsKey(prop)) {
	   		return propFile.getProperty(prop);
	   	} else throw new RuntimeException("Missing property " + prop);
	}
	    

}
