package sut.implementation;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;


public class Main implements Runnable {
	private TCPAdapterSut sut;
	private BufferedReader sockinLL;
	private PrintWriter sockoutLL;
	private Socket sockLL;
	private static String PROPERTIES_FILE = "tcp.properties";

	private static boolean verbose=false;
	private static int tomtePortNumber=7892;
	private static int senderPortNumber;
	private static String senderAddress;

	public Main(TCPAdapterSut sut, Socket sockLL, BufferedReader sockinLL, PrintWriter sockoutLL) {
		this.sut = sut;
		this.sockLL = sockLL;
		this.sockinLL = sockinLL;
		this.sockoutLL = sockoutLL;
	}
	
	public static void globalOut(String s) {
		//System.err.println("myout : " + s);
	}

	public static void handleArgs(String[] args) {

		for (int i = 0; i < args.length; i++) {
			if ("--verbose".equals(args[i])) {
        verbose=true;
				continue;
			}
			if ("-v".equals(args[i])) {
        verbose=true;
				continue;
			}
			if ("--port".equals(args[i])) {
				if (i == args.length - 1) {
					System.err.println("Missing argument for --port.");
					printUsage();
					System.exit(-1);
				}
				try {
					tomtePortNumber = new Integer(args[++i]);
				} catch (NumberFormatException ex) {
					System.err.println("Error parsing argument for --port. Must be integer. " + args[i]);
					System.exit(-1);
				}
				continue;
			}
    }
  }
	

	private static void handleProperies(String propFile) throws Exception{
    	Properties simProperties = new Properties();
    	simProperties.load(new FileInputStream(propFile));
    	senderPortNumber = Integer.valueOf(getProperty(simProperties, "senderPort"));
    	senderAddress = getProperty(simProperties, "senderAddress");
	}
	
    private static String getProperty(Properties propFile, String prop) {
    	if (propFile.containsKey(prop)) {
    		return propFile.getProperty(prop);
    	} else throw new RuntimeException("Missing property " + prop);
    }
    

	public static void printUsage() {
  		System.out.println(" options:");
	  	System.out.println("    --port n         use tcp port n to listen on for incoming connections");
		  System.out.println("    -v|--verbose     verbose mode");
	}

	public static void main(String[] args) throws Exception{
		handleProperies(PROPERTIES_FILE);
    handleArgs(args);
    String msg;

    msg="\n";
    msg=msg+"\nSUT simulation socketserver";
    msg=msg+"\n-> listening at port : " + tomtePortNumber;
    if (verbose) {
       msg=msg+"\n-> verbose mode : ON";
    } else {
       msg=msg+"\n-> verbose mode : OFF";
    }
    msg=msg+"\n-> the server has a timeout of 30 seconds";
    msg=msg+"\n   note: to prevent unnecessary servers to keep on running";
    System.out.println(msg +"\n");

		ServerSocket servsockLL;
		try {
			// port used for communication with LearnLib
			int portNo = tomtePortNumber;

			// instantiate a socket for accepting a connection
			servsockLL = new ServerSocket(portNo);
			servsockLL.setSoTimeout(300000000); // accept waits 10 seconds for connection
			//servsockLL.setSoTimeout(30000); // accept waits 10 seconds for connection
			while (true) {
				// wait to accept a connection request
				// then a data socket is created
				Socket sockLL = null;
				try {
				    sockLL = servsockLL.accept();
				} catch  (java.net.SocketTimeoutException e) {
				    servsockLL.close();	
		           // break;  		
				}
				// get an input stream for reading from the data socket
				InputStream inStreamLL = sockLL.getInputStream();
				// create a BufferedReader object for text line input
				BufferedReader sockinLL = new BufferedReader(
						new InputStreamReader(inStreamLL));

				// get an output stream for writing to the data socket
				OutputStream outStreamLL = sockLL.getOutputStream();
				// create a PrinterWriter object for character-mode output
				PrintWriter sockoutLL = new PrintWriter(new OutputStreamWriter(
						outStreamLL));

				System.out.println("New client...");
				Main client = new Main(new TCPAdapterSut(new SocketWrapper(senderAddress, senderPortNumber)), sockLL, sockinLL, sockoutLL);
				//new Thread(client).start();
				client.run();
			}
			
		} catch (IOException e) {
			
			System.out.println("IOException in main ...");
			e.printStackTrace();
		}

		msg="\n";
    msg=msg+"\nSUT socket server has stopped listening for new SUT clients...";
    msg=msg+"\n-> however any already started SUT clients keep on running!";
    msg=msg+"\n-> the server has a timeout of 30 seconds";
    msg=msg+"\n   note: to prevent unnecessary servers to keep on running";
    System.out.println(msg+"\n\n");
  }


	public void run() {
		System.out.println("Starting client...");
		try {
			String inputString,outputString;
      while ((inputString = sockinLL.readLine()) != null) {

				if (verbose) System.out.println("input: " + inputString);

				if (inputString.equals("reset")) {
					if (verbose) System.out.println("reset sut");
					sut.sendReset();
					continue;
				}
				
				InputAction inputAction = new InputAction(inputString);
				OutputAction outputAction = sut.sendInput(inputAction);
        outputString=outputAction.getValuesAsString();
        if (verbose) System.out.println("output: " + outputString);

				sockoutLL.println(outputString);
				sockoutLL.flush();
			}
		} catch (SocketException e) {
			System.out.println("Server closed connection");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("reset...");
		} 
		
		try {
			sockinLL.close();
			sockoutLL.close();
			sockLL.close();
			this.sut.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		System.out.println("Closing client...");
	}
}
