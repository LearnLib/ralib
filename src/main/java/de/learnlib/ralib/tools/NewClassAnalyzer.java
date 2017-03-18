package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.classanalyzer.ClassAnalyzerParser;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;

public class NewClassAnalyzer extends GenericTool{
	public NewClassAnalyzer(Configuration config) throws ConfigurationException {
		super(new ClassAnalyzerParser());
	}
}
