package sut.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import sut.implementation.InputAction;
import sut.implementation.OutputAction;
import sut.implementation.Parameter;

// this should serve just 
public class SutSocketWrapper {
	private Socket sock;
	private PrintWriter sockout;
	private BufferedReader sockin;
	private int run;
	
	public SutSocketWrapper(String IP, int portNumber) {
		
		try {
			//sock = new Socket("localhost", portNumber);
			sock = new Socket(InetAddress.getByName(IP), portNumber);
			
			
			/*
			  PROBLEM :
			       a not alternating communication order as  write-write-read (at learner) read-read-write(at sut)
			       gives extra blocking for an 500ms for both learner and sut  (timeout of delayed sending of ACK)
			  SOLUTIONS:
			   1. system level solution : disable nagle algoritm on learner side:  sock.setTcpNoDelay(true); 
			   2. user application level solution : add sending of extra dummy mesage so that communication
			                                        keeps alternating  write-read write-read ...
			                                        Then no bad delay happen!!
		    	
			  see : techdocs/slow_socket_communication_caused_by_bad_interaction_between_the_NagleAlgorithm_and_delayedACKsAlgoritm__TCP_NODELAY.txt 
			*/ 
			// applying solution 2: disable nagle algoritm on learner side
			//                      note: doesn't need any changes on sut side, 
			//                            thus when disabling next line we get back our 
			//                            old slow socket implementation but still using 
			//                            new standalone sut implementation (from new independent sut project)
			sock.setTcpNoDelay(true);  // remove unnecesarry delay in socket communication!
			
			sockout = new PrintWriter(sock.getOutputStream(), true);
			sockin = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			
			
			run=1;			
		} catch (IOException e) {
			// e.printStackTrace();
			System.err.println("");
			System.err.println("\n\nPROBLEM: problem with connecting with SUT:\n\n   " + e.getMessage() +"\n\n");
			System.exit(1);
		}
	}

	public OutputAction sendInput(InputAction concreteInput) {
		try {	
			String output = this.serialize(concreteInput); 
			if (output.equals("_CONSTANTS_")) {
				assert false : "CONSTANTS in SUT error";
			}
			
			// Send input to SUT
			sockout.println(output);
			sockout.flush();
			
			// Receive output from SUT
			OutputAction concreteOutput = this.deserialize(sockin.readLine());
			return concreteOutput;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void sendReset()  {
		// send reset to SUT
		sockout.println("reset");
		sockout.flush();

		/*
	      PROBLEM :
		     a not alternating communication order as  write-write-read (at learner) read-read-write(at sut)
		     gives extra blocking for an 500ms for both learner and sut  (timeout of delayed sending of ACK)
		  SOLUTIONS:
		   1. system level solution : disable nagle algoritm on teacher sid :  sock.setTcpNoDelay(true); 
		   2. user application level solution : add sending of extra dummy mesage so that communication
		                                        keeps alternating  write-read write-read ...
		                                        Then no bad delay happen!!
	    	
		  see : techdocs/slow_socket_communication_caused_by_bad_interaction_between_the_NagleAlgorithm_and_delayedACKsAlgoritm__TCP_NODELAY.txt 
		*/ 
		
		/*		
		  Applying Solution 2: 
		   add DUMMY read to fix at user application level the extra delay problem when 
		   bad interaction between the "Nagle algorithm" and "delayed ACKs algoritm" happens : 
		       write-write-read  -> write-DUMMYread-write-read
		*//*
		try {
			sockin.readLine();              		 
		} catch (Exception e)  {            
			throw new ExceptionAdapter(e);  
		}                                   
		*/          
		
		
		run=run+1;
		
	}
	
	

	
	public void close() {
		/*
		try {
			sockin.close();
			sockout.close();
			sock.close();
		} catch (IOException ex) {
			
		}
		*/
	}

	/* SutSocketWrapper has its own specialized method to serialize an input action */
    public String serialize(InputAction action) {
        String result = action.getMethodName();
        List<Parameter> params=action.getParameters();
        if (params.size() > 0) {
            for (Parameter parameter : params) {
                    result += "_" + parameter.getValue();
            }
        }
        return result;
    }

    /* SutSocketWrapper has its own specialized method to deserialize a serialized output action */
    public OutputAction deserialize(String actionString) {
        String[] action = actionString.split("_");

        if (action.length < 1) {
            System.out.println("Error deserializing concrete output from string: " + actionString);
            throw new RuntimeException("Error deserializing concrete output from string: " + actionString);
        }

        String methodName = action[0];
        
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        if (action.length > 1) {
            for (int i = 1; i < action.length; i++) {
                String args = action[i];

                Integer value;
                try {
                    value = new Integer(args);
                } catch (NumberFormatException ex) {
                	String msg="Error deserializing concrete output from string: "  + actionString + " problem with converting args to string; args: " + args;
                    throw new RuntimeException(msg);
                }

                parameters.add(new Parameter(new Integer(value), parameters.size()));
            }
        }
        // use concrete constructor of OutputAction
        return new OutputAction(methodName, parameters);

    }
	
}
