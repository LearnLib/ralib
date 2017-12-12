package sut.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import util.Log;


// Wrapper around the java Socket so we have clear segmentation of inputs and outputs
public class SocketWrapper {
	private static Map<Integer, Socket> socketMap = new HashMap<Integer, Socket>();
	protected Socket sock;
	protected PrintWriter sockout;
	protected BufferedReader sockin;
	

	public SocketWrapper(String sutIP, int sutPort) {
		try {
			if(socketMap.containsKey(sutPort) ) { //&& !socketMap.get(sutPort).isClosed()) {
				sock = socketMap.get(sutPort);
			} else {
				sock = new Socket(sutIP, sutPort);
				sock.setReuseAddress(true);
				socketMap.put(sutPort, sock);
			}
			sockout = new PrintWriter(sock.getOutputStream(), true);
			sockin = new BufferedReader(new InputStreamReader(
					sock.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public SocketWrapper(int sutPort) {
		this("localhost", sutPort);
	}

	public void writeInput(String input) {
	    if (sockout != null) {
		Log.info("IN: "+ input);
		sockout.println(input);
		sockout.flush();
	    }
	}

	public String readOutput() {
		String output = null;
		try {
			output = sockin.readLine();
			if (output == null) {
				throw new RuntimeException("socket closed!");
			}
			Log.info("OUT: "+ output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	public void close() {
	    if (sockout != null) {
		sockout.write("exit");
		try {
			sock.close();
		} catch (IOException ex) {

		}
	    }
		/*sockout.close();
		try {
			sockin.close();
		} catch (IOException ex) {

		}*/
	}
}
