package de.learnlib.ralib;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;

public class TestConfig {
	
    protected static ConfigurationOption.StringOption SEEDS_OPTION = new ConfigurationOption
    		.StringOption("seeds", "Coma seperated seeds used during learning experiments", "0", true);
    protected static ConfigurationOption.StringOption LOGGER_LEVEL_OPTION = new ConfigurationOption
    		.StringOption("logger.level", "Coma seperated seeds used during learning experiments", "FINE", true);
    
	static TestConfig parseTestConfig() throws IOException, ConfigurationException {
		Configuration config;
		if (System.getProperty("config") != null) {
			String location = System.getProperty("config");
			config = new Configuration(new File(location));
		} else {
			config = new Configuration(RaLibTestSuite.class.getResourceAsStream("/test.prop"));
		}
		return new TestConfig(config);
	}
	
	private TestConfig(Configuration config) throws ConfigurationException {
		this.seeds = readSeeds(config);
		this.loggingLevel = readLevel(config);
	}

	/*
	 * Test config parameters.
	 */
	
	// learning seeds
	private long [] seeds; 
	
	// logging level
	private Level loggingLevel;
	
	
	
	/**
	 * (Random) Seeds used by each learning experiment.
	 *  The number of seeds determines the number of learning experiments.  
	 * @return
	 */
	public long [] getSeeds() {
		return seeds;
	}
	
	/**
	 * Logging level for unit tests.
	 */
	Level getLoggingLevel() {
		return loggingLevel;
	}
	
	
	/*
	 * Parsing functions
	 */
	private static long [] readSeeds(Configuration config) throws ConfigurationException {
		String seedsString = SEEDS_OPTION.parse(config);
		String[] seedSplit = seedsString.split("\\,");
		long [] seeds  = new long [seedsString.length()];
		int i=0;
		for (String seedStr : seedSplit) {
			try {
				seeds[i++] = Long.valueOf(seedStr);
			} catch(NumberFormatException e) {
				throw new ConfigurationException("Expected coma separated list of long values");
			}
		}
		return seeds;
	}	
	
	private Level readLevel(Configuration config) throws ConfigurationException {
		String levelString = LOGGER_LEVEL_OPTION.parse(config);
		Level level = Level.parse(levelString);
		return level;
	}
}
