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
		return new TCPConfig(senderAddress, senderPortNumber);
	}
	
	public TCPConfig(String senderAddress, int senderPortNumber) {
		super();
		this.senderPortNumber = senderPortNumber;
		this.senderAddress = senderAddress;
	}

	public final int senderPortNumber;
	public final String senderAddress;
	
	private static String getProperty(Properties propFile, String prop) {
	  	if (propFile.containsKey(prop)) {
	   		return propFile.getProperty(prop);
	   	} else throw new RuntimeException("Missing property " + prop);
	}
	    

}
