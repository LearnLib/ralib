package de.learnlib.ralib;

import java.io.File;
import java.io.IOException;

import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;

public class TestConfig {
	
    protected static ConfigurationOption.StringOption SEEDS_OPTION = new ConfigurationOption
    		.StringOption("seeds", "Coma seperated seeds used during learning experiments", "0", true);
	
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
	}

	/*
	 * Test config parameters.
	 */
	
	private long [] seeds;
	
	/**
	 * (Random) Seeds used by each learning experiment.
	 *  The number of seeds determines the number of learning experiments.  
	 * @return
	 */
	public long [] getSeeds() {
		return seeds;
	}
	
	
	
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
}
