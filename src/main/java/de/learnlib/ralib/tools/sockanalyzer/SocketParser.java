package de.learnlib.ralib.tools.sockanalyzer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.tools.SULFactory;
import de.learnlib.ralib.tools.SULParser;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.commons.util.Pair;

public class SocketParser extends SULParser{
	private static final ConfigurationOption.StringOption OPTION_TARGET_IP = new ConfigurationOption.StringOption(
			"target.ip", "SUL IP address", "localhost", true);

	protected static final ConfigurationOption.IntegerOption OPTION_TARGET_PORT = new ConfigurationOption.IntegerOption(
			"target.port", "SUL port number", 7892, true);

	private static final ConfigurationOption.StringOption OPTION_INPUTS = new ConfigurationOption.StringOption("inputs",
			"traget input signatures. format: m1(class:type,class:type) + m2() + ...", null, false);

	private static final ConfigurationOption.StringOption OPTION_OUTPUTS = new ConfigurationOption.StringOption(
			"outputs", "traget output signatures. format: m1(class:type,class:type) + m2() + ...", null, false);
	
	protected static final ConfigurationOption.IntegerOption OPTION_MAX_DEPTH = new ConfigurationOption.IntegerOption(
			"max.depth", "Maximum depth to explore", -1, true);
	
	private String systemIP;

	private Integer systemPort;

	private LinkedHashMap<String, DataType> types;

	private ParameterizedSymbol[] inputs;

	private ParameterizedSymbol[] outputs;
	
	private int maxDepth;
	
	public void parseConfig(Configuration config) throws ConfigurationException {
		this.systemIP = OPTION_TARGET_IP.parse(config);
		this.systemPort = OPTION_TARGET_PORT.parse(config);
		this.types = new LinkedHashMap<String,DataType>();
		
		String[] inpStrings = OPTION_INPUTS.parse(config).split("\\+");
		this.inputs = parseSymbols(inpStrings, types, true);
		String[] outStrings = OPTION_OUTPUTS.parse(config).split("\\+");
		this.outputs = parseSymbols(outStrings, types, false);
		this.maxDepth = OPTION_MAX_DEPTH.parse(config);
	}
	
	private ParameterizedSymbol [] parseSymbols(String [] symStrings, Map<String, DataType> types, boolean isInput) throws ConfigurationException {
		ParameterizedSymbol [] syms = new ParameterizedSymbol[symStrings.length];
		for (int i=0; i<symStrings.length; i++) {
			syms[i] = parseSymbol(symStrings[i], types, isInput);
		}
		return syms;
	}
	
	private ParameterizedSymbol parseSymbol(String config, Map<String, DataType> types, boolean isInput) throws  ConfigurationException {
		String methodName = config.substring(0, config.indexOf("(")).trim();
		String paramString = config.substring(config.indexOf("(") + 1, config.indexOf(")")).trim();
		String[] paramConfig = (paramString.length() < 1) ? new String[0]
				: config.substring(config.indexOf("(") + 1, config.indexOf(")")).trim().split(",");
		Class<?>[] pTypes = new Class<?>[paramConfig.length];
		DataType[] cTypes = new DataType[paramConfig.length];
	    int idx = 0;
		for (String pc : paramConfig) {
			Pair<Class<?>, String> parsed = parseParamConfig(pc);
			pTypes[idx] = parsed.getFirst();
			cTypes[idx] = getOrCreate(parsed.getSecond(), parsed.getFirst(), types);
			idx++;
		}
		if (isInput)
			return new InputSymbol(methodName, cTypes);
		else
			return new OutputSymbol(methodName, cTypes);
	}
	@Override
	public ParameterizedSymbol[] getInputs() {
		return this.inputs;
	}

	@Override
	public ParameterizedSymbol[] getOutput() {
		return this.outputs;
	}

	@Override
	public Map<String, DataType> getTypes() {
		return this.types;
	}
	
	public String getTargetName() {
		return "Server " + this.systemIP + ":" + this.systemPort;
	}
	
    private DataType getOrCreate(String name, Class<?> base, Map<String, DataType> map) {
        DataType ret = map.get(name);
        if (ret == null) {
            ret = new DataType(name, base); 
            map.put(name, ret);
        }
        return ret;
    }
	
    private Pair<Class<?>, String> parseParamConfig(String config) throws ConfigurationException {
        System.out.println("param config: " + config);
        String[] parts = config.trim().split(":");
        Class<?> cl;
        if (parts[0].trim().equals("boolean")) {
            cl = boolean.class;
        }
        else {
            cl = loadClass(parts[0].trim());
        }
        return new Pair<Class<?>, String>(cl, parts[1].trim());
    } 
    
    
    private Class<?> loadClass(String name) throws ConfigurationException {
    	try {
			return Class.forName(name.trim());
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(e.getMessage());
		}
    }

	@Override
	public SULFactory newSULFactory() {
		SULFactory sulFactory = new SocketAnalyzerSULFactory(systemIP, systemPort, maxDepth, Arrays.asList(this.inputs), Arrays.asList(this.outputs));
		return sulFactory;
	}

}
