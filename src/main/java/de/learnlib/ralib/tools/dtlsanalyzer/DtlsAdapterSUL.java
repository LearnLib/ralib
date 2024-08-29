package de.learnlib.ralib.tools.dtlsanalyzer;

import de.learnlib.api.SUL;
import de.learnlib.api.exception.SULException;
import de.learnlib.ralib.words.PSymbolInstance;

public class DtlsAdapterSUL implements SUL<PSymbolInstance, PSymbolInstance> {

	private SocketWrapper dtlsFuzzerSocket;
	private DtlsSerializer dtlsSerializer;
	private boolean needsReset;

	public DtlsAdapterSUL(SocketWrapper dtlsFuzzerSocket, DtlsSerializer serializer) {
		this.dtlsFuzzerSocket = dtlsFuzzerSocket;
		this.dtlsSerializer = serializer;
		this.needsReset = false;
	}

	public void pre() {
	}

	@Override
	public void post() {
		if (needsReset) {
			sendReset();
			needsReset = false;
		}
	}

	public void close() {
		dtlsFuzzerSocket.close();
	}

	public boolean canFork() {
		return false;
	}

    private void sendReset() {
    	dtlsFuzzerSocket.writeInput("reset");
    	// need some confirmation that the reset was successful
    	String conf = dtlsFuzzerSocket.readOutput();
    	if (conf == null) {
    		throw new RuntimeException("Adapter did not confirm reset");
    	}
	}

	@Override
	public PSymbolInstance step(PSymbolInstance in) throws SULException {
		String inputString = dtlsSerializer.serializeSymbolInstance(in);
		dtlsFuzzerSocket.writeInput(inputString);
		String outputString = dtlsFuzzerSocket.readOutput();

		// always need to read output, otherwise risk buffer overflow
		PSymbolInstance oa = dtlsSerializer.deserializeSymbolInstance(outputString, true);
		needsReset = true;
		return oa;
	}

}
