/*
 * sut is generated from model  : /home/dummy6/workspace/AbsLearnRGB_tree/models/FWGC2/model.xml
 */

package de.learnlib.ralib.sul.tcp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import de.learnlib.api.SULException;
import de.learnlib.ralib.tools.sockanalyzer.SocketWrapper;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteInput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteOutput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteSUL;

public class TCPAdapterSut extends ConcreteSUL{

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
    
    

	public void pre() {
		this.sendReset();
	}


	@Override
	public void post() {
	}


	@Override
	public ConcreteOutput step(ConcreteInput in) throws SULException {
		ConcreteOutput out = this.sendInput(in);
		return out;
	}
    
	
	public TCPAdapterSut(SocketWrapper sender) {
		this.senderSocket = sender;
	}

	private ConcreteOutput sendInput( ConcreteInput action) {
		//System.out.println("Input from RaLib: " + action);
		String inputString = Serializer.concreteInputToString(action);
		this.senderSocket.writeInput(inputString);
		String outputString = this.senderSocket.readOutput();
		ConcreteOutput oa = Serializer.stringToConcreteOutput(outputString);
		updateSeqNums(oa);
		System.out.println("Output for RaLIB: " + oa);
		return oa;  //oa implements sut.interfaces.OutputAction interface
	}	

	private void updateSeqNums(ConcreteOutput oa) {
	    if (oa.getParameterValues().length > 1) {
            String sutFlags = oa.getMethodName();
            if (!sutFlags.contains("R")) {
                int nextSeq = (int) oa.getParameterValues()[1];
                if (nextSeq != 0) {
                    sutSeqNum.remove(nextSeq-1);
                    sutSeqNum.add(nextSeq);
                }
            }
	    }
    }

	public void close() {
		this.senderSocket.close();
	}


    private void sendReset() {
        for (Integer seqNum : sutSeqNum) {
            this.senderSocket.writeInput("R "+ Serializer.int2ULong(seqNum) + " 0 []");
            this.senderSocket.readOutput();
        }
        this.senderSocket.writeInput("reset");
	    this.sutSeqNum = new HashSet<Integer>();
	}
}