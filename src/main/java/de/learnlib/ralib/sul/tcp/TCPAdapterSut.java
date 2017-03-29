package de.learnlib.ralib.sul.tcp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import de.learnlib.api.SULException;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.exceptions.SULRestartException;
import de.learnlib.ralib.tools.sockanalyzer.SocketWrapper;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteInput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteOutput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteSUL;

public class TCPAdapterSut extends ConcreteSUL{

	private static String PROPERTIES_FILE = "tcp.properties";
	private SocketWrapper senderSocket;
	private Set<Long> sutSeqNums = new HashSet<Long>();
	// we don't want to be sending needless resets
	private static boolean needsReset = false;
	private static long maxNum =  4031380001L; //4231380001L;
	
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
		this.sendReset();
	}


	@Override
	public ConcreteOutput step(ConcreteInput in) throws SULException {
		ConcreteOutput out = this.sendInput(in);
		return out;
	}
    
	
	public TCPAdapterSut(SocketWrapper sender) {
		this.senderSocket = sender;
	}

	private ConcreteOutput sendInput( ConcreteInput ia) {
		//System.out.println("Input from RaLib: " + action);
		needsReset = true;
		ConcreteOutput oa = this.sendOneInput(ia);
		updateSeqNums(oa);
		if (isMaxSeqCloseToEdge()) {
			this.sendReset();
			throw new SULRestartException();
		}
		
		//System.out.println("Output for RaLIB: " + oa);
		return oa;  //oa implements sut.interfaces.OutputAction interface
	}	
	
	// plain send method, without the max seq checks. No recursion concerns attached.
	private ConcreteOutput sendOneInput(ConcreteInput ia) {
		checkInput(ia);
		String inputString = Serializer.concreteInputToString(ia);
		this.senderSocket.writeInput(inputString);
		String outputString = this.senderSocket.readOutput();

		// always need to read output, otherwise risk buffer overflow
		ConcreteOutput oa = Serializer.stringToConcreteOutput(outputString); 
		return oa;
	}

	private void checkInput(ConcreteInput ia) {
		for (Object val : ia.getParameterValues()) 
			if (((long) val) < 0L)  {
				this.sendReset();
				throw new DecoratedRuntimeException("Cannot send negative values").addDecoration("value", val);
			}
	}


	private void updateSeqNums(ConcreteOutput oa) {
	    if (oa.getParameterValues().length > 1) {
            String sutFlags = oa.getMethodName();
            if (!sutFlags.contains("R")) {
                long [] seqs = {
                		(long) oa.getParameterValues()[1],
                		(long) oa.getParameterValues()[0] };
                for (long seq : seqs) 
	                if (seq != 0) {
	                    sutSeqNums.add(seq);
	                }
            }
	    }
//	    System.out.println(sutSeqNums);
    }

	public void close() {
		this.senderSocket.close();
	}


    private void sendReset() {
    	if (needsReset) {
	    	sendResetBurst(this.sutSeqNums);
	        this.senderSocket.writeInput("reset");
		    this.sutSeqNums = new HashSet<Long>();
			needsReset = false;
    	}
	}
    
    private boolean isMaxSeqCloseToEdge() {
    	return !sutSeqNums.isEmpty() && Collections.max(sutSeqNums) > maxNum;
    }
    
    private void sendResetBurst(Set<Long> seqNums) {
    	for (Long seqNum : seqNums) {
    		ConcreteInput resetInput = new ConcreteInput("R", seqNum, seqNum);
    		this.sendOneInput(resetInput);
        }
    }
}