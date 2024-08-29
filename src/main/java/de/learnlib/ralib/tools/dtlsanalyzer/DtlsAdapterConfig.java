package de.learnlib.ralib.tools.dtlsanalyzer;

public class DtlsAdapterConfig {


	private Integer fuzzerPort;
	private String fuzzerAddress;
	private Integer analyzerPort;
	private String analyzerAddress;
	private String dtlsFuzzerCommand;
	private String dtlsFuzzerDirectory;
	private boolean dtlsFuzzerOutputEnabled;

	public DtlsAdapterConfig(String fuzzerAddress, Integer fuzzerPort, String dtlsFuzzerCommand, String dtlsFuzzerDirectory, boolean dtlsFuzzerOutputEnabled, String analyzerAddress, Integer analyzerPort) {
		this.fuzzerAddress = fuzzerAddress;
		this.fuzzerPort = fuzzerPort;
		this.analyzerAddress = analyzerAddress;
		this.analyzerPort = analyzerPort;
		this.dtlsFuzzerCommand = dtlsFuzzerCommand;
		this.dtlsFuzzerDirectory = dtlsFuzzerDirectory;
		this.dtlsFuzzerOutputEnabled = dtlsFuzzerOutputEnabled;
	}

	public Integer getFuzzerPort() {
		return fuzzerPort;
	}

	public String getFuzzerAddress() {
		return fuzzerAddress;
	}

	public Integer getAnalyzerPort() {
		return analyzerPort;
	}


	public String getAnalyzerAddress() {
		return analyzerAddress;
	}

	public String getDTLSFuzzerCommand() {
		return dtlsFuzzerCommand;
	}

	public String getDtlsFuzzerDirectory() {
		return dtlsFuzzerDirectory;
	}

	public boolean isDTLSFuzzerOutputEnabled() {
		return dtlsFuzzerOutputEnabled;
	}
}
