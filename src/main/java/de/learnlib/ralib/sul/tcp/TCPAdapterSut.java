package de.learnlib.ralib.sul.tcp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.learnlib.api.SUL;
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
	private Integer senderPortNumber;
	private String senderAddress;
	// we don't want to be sending needless resets, having a port based reset system
	// ensures that we also handle concurrent cases
	private volatile static Map<Integer, Boolean> needsReset = new LinkedHashMap<>();
	private static long maxNum =  4201380001L; //4231380001L;
	private static long minNum =  201380001L;
	
	
	public TCPAdapterSut() throws FileNotFoundException, IOException {
    	Properties simProperties = new Properties();
    	simProperties.load(new FileInputStream(PROPERTIES_FILE));
    	senderPortNumber = Integer.valueOf(getProperty(simProperties, "senderPort"));
    	senderAddress = getProperty(simProperties, "senderAddress");
    	this.senderSocket = new SocketWrapper(senderAddress, senderPortNumber);
    	needsReset.putIfAbsent(this.senderPortNumber, true);
	}
	
	public TCPAdapterSut(String address, Integer port) throws IOException {
		senderPortNumber = port;
    	senderAddress = address;
    	this.senderSocket = new SocketWrapper(senderAddress, senderPortNumber);
    	needsReset.putIfAbsent(this.senderPortNumber, true);
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
		//needsReset = true;
		needsReset.put(this.senderPortNumber, true);
		ConcreteOutput oa = this.sendOneInput(ia);
		updateSeqNums(oa);
		if (isSeqCloseToEdge()) {
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
	
	public boolean canFork() {
		return true;
	}
	
	public SUL<ConcreteInput, ConcreteOutput> fork() {
		try {
			return new TCPAdapterSut(this.senderAddress, this.senderPortNumber+1);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


    private void sendReset() {
    	if (needsReset.get(this.senderPortNumber)) {
	    	sendResetBurst(this.sutSeqNums);
	        this.senderSocket.writeInput("reset");
		    this.sutSeqNums = new HashSet<Long>();
		    needsReset.put(this.senderPortNumber, false);
    	}
	}
    
    private boolean isSeqCloseToEdge() {
    	return !sutSeqNums.isEmpty() && ( Collections.max(sutSeqNums) > maxNum ||  Collections.min(sutSeqNums) < minNum);
    }
    
    private void sendResetBurst(Set<Long> seqNums) {
    	for (Long seqNum : seqNums) {
    		ConcreteInput resetInput = new ConcreteInput("AR", seqNum, seqNum);
    		this.sendOneInput(resetInput);
        }
    }
    
}