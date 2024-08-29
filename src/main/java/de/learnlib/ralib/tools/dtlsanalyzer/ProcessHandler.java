package de.learnlib.ralib.tools.dtlsanalyzer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import de.learnlib.api.logging.LearnLogger;


/**
 * Allows one to start/stop processes launched by executing a given command. At
 * most one process can be executing at a time.
 *
 * @author Paul
 */
public class ProcessHandler {

	private static final LearnLogger log = LearnLogger.getLogger(ProcessHandler.class);


	private final ProcessBuilder pb;
	private Process currentProcess;
	private String terminateCommand;
	private OutputStream output;
	private OutputStream error;
	private long runWait;
	private boolean hasLaunched;

	ProcessHandler(String command, long runWait) {
		// '+' after \\s takes care of multiple consecutive spaces so that they
		// don't result in empty arguments
		pb = new ProcessBuilder(command.split("\\s+"));
		this.runWait = runWait;
		output = System.out;
		error = System.err;
		log.info(String.format("Command to launch DTLS-Fuzzer: %s", command));
	}

	public void redirectOutputToNull() {
		redirectOutput(new NullOutputStream());
	}

	public void redirectOutput(OutputStream toOutput) {
		output = toOutput;
	}

	public void redirectErrorToNull() {
		redirectError(new NullOutputStream());
	}

	public void redirectError(OutputStream toOutput) {
		error = toOutput;
	}

	public void setDirectory(File procDir) {
		pb.directory(procDir);
	}

	/**
	 * Launches a process which executes the handler's command. Does nothing if
	 * the process has been launched already.
	 *
	 * Sets {@link ProcessHandler#hasLaunched} to true on successful launch of
	 * the process, making {@link ProcessHandler#hasLaunched()} return true
	 * thereafter.
	 *
	 * After launching, it sleeps for {@link ProcessHandler#runWait}
	 * milliseconds.
	 */
	public void launchProcess() {
		try {
			if (currentProcess == null) {
				hasLaunched = true;
				currentProcess = pb.start();
				if (output != null)
					inheritIO(currentProcess.getInputStream(), new PrintStream(
							output));
				if (error != null)
					inheritIO(currentProcess.getErrorStream(), new PrintStream(
							error));
				if (runWait > 0)
					Thread.sleep(runWait);
			} else {
				log.warn("Process has already been started");
			}

		} catch (IOException | InterruptedException E) {
			log.error("Couldn't start process due to exec: " + E.getMessage());
			throw new RuntimeException(E);
		}
	}

	/**
	 * Terminates the process executing the handler's command. Does nothing if
	 * the process has been terminated already.
	 */
	public void terminateProcess() {
		if (currentProcess != null) {
			if (terminateCommand != null) {
				try {
					Runtime.getRuntime().exec(terminateCommand);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				currentProcess.destroyForcibly();
			}
			currentProcess = null;
		} else {
			log.warn("Process has already been ended");
		}
	}

	public boolean isAlive() {
		return currentProcess != null && currentProcess.isAlive();
	}

	/**
	 * Returns true if the process has been launched successfully at least once,
	 * irrespective of whether it has terminated since first execution.
	 */
	public boolean hasLaunched() {
		return hasLaunched;
	}

	private void inheritIO(final InputStream src, final PrintStream dest) {
		new Thread(new Runnable() {
			public void run() {
				Scanner sc = new Scanner(src);
				while (sc.hasNextLine()) {
					dest.println(sc.nextLine());
				}
				sc.close();
			}
		}).start();
	}

	private static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
		}
	}
}
