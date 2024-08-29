package de.learnlib.ralib.tools.dtlsanalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

// Wrapper around the java Socket so we have clear segmentation of inputs and outputs
public class SocketWrapper {
	private static int MAX_ATTEMPTS = 40;
	private static int MS_ATTEMPT = 100;
	protected Socket sock;
	protected PrintWriter sockout;
	protected BufferedReader sockin;

	public SocketWrapper(DtlsAdapterConfig config) {
		try {
			int attempts=0;
			while (attempts<MAX_ATTEMPTS) {
				try {
					sock = buildSocket(config);
					sock.connect(
						new InetSocketAddress(InetAddress.getByName(config.getFuzzerAddress()), config.getFuzzerPort()));
					if (sock.isConnected()) {
						break;
					}
				} catch(ConnectException e) {
					attempts ++;
					try {
						Thread.sleep(MS_ATTEMPT);
					} catch (InterruptedException e1) {
					}
				}
			}
			if (!sock.isConnected()) {
				throw new RuntimeException(String.format("Failed to connect to fuzzer after %s milliseconds", MAX_ATTEMPTS*MS_ATTEMPT));
			}

			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					System.err.println("close " + Thread.currentThread().getName());
					SocketWrapper.this.close();
				}
			}));
			sockout = new PrintWriter(sock.getOutputStream(), true);
			sockin = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private Socket buildSocket(DtlsAdapterConfig config) throws UnknownHostException, IOException {
		Socket sock = new Socket();
		sock.setReuseAddress(true);
		if (config.getAnalyzerPort() != null) {
			sock.bind(new InetSocketAddress(InetAddress.getByName(config.getAnalyzerAddress()),
					config.getAnalyzerPort()));
		}
		return sock;
	}

	public void writeInput(String input) {
		// System.err.println("write" + this.sock + " "+
		// Thread.currentThread().getName());
		if (sockout != null) {
			sockout.println(input);
			sockout.flush();
		}
	}

	public String readOutput() {
		// System.err.println("read " + this.sock + " "+
		// Thread.currentThread().getName());
		String output = null;
		try {
			output = sockin.readLine();
			if (output == null) {
				throw new RuntimeException("socket closed!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	public void close() {
		// System.err.println("close "+ this.sock + " " +
		// Thread.currentThread().getName());
		if (sockout != null) {
			// sockout.write("exit");
			try {
				if (!sock.isClosed())
					sock.close();
			} catch (IOException ex) {

			}
		}
	}
}
