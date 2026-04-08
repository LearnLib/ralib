package de.learnlib.ralib.tools.dtlsanalyzer;

import java.io.File;

import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class DtlsAdapterSULFactory {

	private DtlsAdapterSUL dtlsAdapterDataWordSul;
	private DtlsAdapterConfig dtlsAdapterConfig;
	private ParameterizedSymbol[] inputs;
	private ParameterizedSymbol[] outputs;

	public DtlsAdapterSULFactory(ParameterizedSymbol [] inputs, ParameterizedSymbol[] outputs, DtlsAdapterConfig dtlsAdapterConfig) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.dtlsAdapterConfig = dtlsAdapterConfig;
	}


	public DataWordSUL newSUL() {
		if (dtlsAdapterDataWordSul == null) {
			if (dtlsAdapterConfig.getDTLSFuzzerCommand() != null) {
				ProcessHandler handler = new ProcessHandler(resolveVariables(dtlsAdapterConfig.getDTLSFuzzerCommand()), 100);
				if (!dtlsAdapterConfig.isDTLSFuzzerOutputEnabled()) {
					handler.redirectErrorToNull();
					handler.redirectOutputToNull();
				}
				if (dtlsAdapterConfig.getDtlsFuzzerDirectory() != null) {
					String resolvedPath = resolveVariables(dtlsAdapterConfig.getDtlsFuzzerDirectory());
					File file = new File(resolvedPath);
					if (!file.isDirectory()) {
						throw new RuntimeException(String.format("Fuzzer directory is invalid. Directory given: %s", resolvedPath));
					}
					handler.setDirectory(file);
				}
				handler.launchProcess();
				if (!handler.isAlive()) {
					throw new RuntimeException("DTLS-Fuzzer process terminated prematurely");
				}
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						if (handler.isAlive()) {
							handler.terminateProcess();
						}
					}
				}));
			}
			SocketWrapper dtlsFuzzerSocket = new SocketWrapper(dtlsAdapterConfig);
			dtlsAdapterDataWordSul = new DtlsAdapterSUL(dtlsFuzzerSocket, new DtlsSerializer(inputs, outputs));
		}
		return new DtlsDataWordSUL(dtlsAdapterDataWordSul);
	}

	private String resolveVariables(String command) {
		String resolvedStr = command;
		for (String propName : System.getProperties().stringPropertyNames()) {
			String propValue = System.getProperties().getProperty(propName);
			resolvedStr = resolvedStr.replaceAll("\\$\\{"+propName+"\\}", propValue);
		}
		return resolvedStr;
	}

	static class DtlsDataWordSUL extends DataWordSUL {
	    private DtlsAdapterSUL adapter;

        public DtlsDataWordSUL(DtlsAdapterSUL adapter) {
	        this.adapter = adapter;
	    }

        @Override
        public void pre() {
            adapter.pre();

        }

        @Override
        public void post() {
            adapter.post();
        }

        @Override
        public PSymbolInstance step(PSymbolInstance in) {
            return adapter.step(in);
        }
	}
}
