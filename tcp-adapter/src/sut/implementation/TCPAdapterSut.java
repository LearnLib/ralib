/*
 * sut is generated from model  : /home/dummy6/workspace/AbsLearnRGB_tree/models/FWGC2/model.xml
 */

package sut.implementation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import util.Log;

public class TCPAdapterSut implements sut.interfaces.SutInterface{

	private static String PROPERTIES_FILE = "tcp.properties";
	private SocketWrapper senderSocket;
	private Set<Integer> sutSeqNum = new HashSet<Integer>();
	
	public TCPAdapterSut() throws FileNotFoundException, IOException {
    	Properties simProperties = new Properties();
    	simProperties.load(new FileInputStream(PROPERTIES_FILE));
    	Integer senderPortNumber = Integer.valueOf(getProperty(simProperties, "senderPort"));
    	String senderAddress = getProperty(simProperties, "senderAddress");
    	this.senderSocket = new SocketWrapper(senderAddress, senderPortNumber);
	}
	

    private static String getProperty(Properties propFile, String prop) {
    	if (propFile.containsKey(prop)) {
    		return propFile.getProperty(prop);
    	} else throw new RuntimeException("Missing property " + prop);
    }
    
	
	public TCPAdapterSut(SocketWrapper sender) {
		this.senderSocket = sender;
	}

	public OutputAction sendInput( sut.interfaces.InputAction action) {
		InputAction ia = new InputAction(action);
		Log.fatal("Input from Tomte: " + ia.getValuesAsString());
		String inputString = Serializer.tomteInputToString(ia);
		this.senderSocket.writeInput(inputString);
		String outputString = this.senderSocket.readOutput();
		OutputAction oa = Serializer.stringToTomteOutput(outputString);
		updateSeqNums(oa);
		Log.fatal("Output for Tomte: " + oa.getValuesAsString());
		return oa;  //oa implements sut.interfaces.OutputAction interface
	}	

	private void updateSeqNums(OutputAction oa) {
	    if (oa.getParameters().size() > 1) {
            String sutFlags = oa.getMethodName();
            if (!sutFlags.contains("R")) {
                int nextSeq = oa.getParam(1).getValue();
                if (nextSeq != 0) {
                    sutSeqNum.remove(nextSeq-1);
                    sutSeqNum.add(nextSeq);
                }
            }
	    }
        
    }


    public void sendReset() {
        for (Integer seqNum : sutSeqNum) {
            this.senderSocket.writeInput("R "+ Serializer.int2ULong(seqNum) + " 0 []");
            this.senderSocket.readOutput();
        }
        this.senderSocket.writeInput("reset");
        this.senderSocket.readOutput();
	    this.sutSeqNum = new HashSet<Integer>();
	}
	
	public void close() {
		this.senderSocket.close();
	}

}