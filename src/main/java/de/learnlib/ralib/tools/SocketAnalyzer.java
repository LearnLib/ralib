package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.sockanalyzer.SocketParser;

public class SocketAnalyzer extends ToolTemplate {

	public SocketAnalyzer() throws ConfigurationException {
		super(new SocketParser());
	}

	@Override
	public String description() {
		return "Analyzes the behavior of a SUL over a socket connection.";
	}

}
