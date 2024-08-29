package de.learnlib.ralib.tools.dtlsanalyzer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.commons.util.Pair;

public class DtlsAnalyzerParser {

	private static final ConfigurationOption.StringOption OPTION_INPUTS = new ConfigurationOption.StringOption("inputs",
			"the input param symbols. : input1(class11:type11,class12:type12...)+input2(class21:type21...) + ...", null,
			false);

	private static final ConfigurationOption.StringOption OPTION_OUTPUTS = new ConfigurationOption.StringOption(
			"outputs",
			"the output param symbols. : output1(class11:type11,class12:type12...)+output2(class21:type21...) + ...",
			null, false);


	private static final ConfigurationOption.StringOption OPTION_DTLSFUZZER_ADDRESS = new ConfigurationOption.StringOption(
			"dtlsfuzzer.address",
			"The listening address of DTLS-Fuzzer",
			"localhost", true);

	private static final ConfigurationOption.IntegerOption OPTION_DTLSFUZZER_PORT = new ConfigurationOption.IntegerOption(
			"dtlsfuzzer.port",
			"The listening port of DTLS-Fuzzer",
			null, false);

	private static final ConfigurationOption.StringOption OPTION_DTLSFUZZER_COMMAND = new ConfigurationOption.StringOption(
			"dtlsfuzzer.command",
			"Command to launch DTLS-Fuzzer prior to starting learning",
			null, true);

	private static final ConfigurationOption.StringOption OPTION_DTLSFUZZER_DIRECTORY = new ConfigurationOption.StringOption(
			"dtlsfuzzer.directory",
			"DTLS-Fuzzer's directory",
			null, true);

	private static final ConfigurationOption.BooleanOption OPTION_DTLSFUZZER_OUTPUT = new ConfigurationOption.BooleanOption(
			"dtlsfuzzer.output",
			"Do not redirect DTLS-Fuzzer stdout/stderr to a NULL OutputStream",
			Boolean.FALSE, true);

	private static final ConfigurationOption.StringOption OPTION_ANALYZER_ADDRESS = new ConfigurationOption.StringOption(
			"dtlsanalyzer.address",
			"The address by which the DTLS message string is sent to DTLS-Fuzzer",
			"0.0.0.0", true);

	private static final ConfigurationOption.IntegerOption OPTION_ANALYZER_PORT = new ConfigurationOption.IntegerOption(
			"dtlsanalyzer.address",
			"The address by which the DTLS message string is sent to DTLS-Fuzzer",
			null, true);

	private static final ConfigurationOption.BooleanOption OPTION_ANALYZER_LEARN_CONCRETIZATION= new ConfigurationOption.BooleanOption(
			"dtlsanalyzer.learn.concretization",
			"Learn the concretization of the FSM one would generate with DTLS-Fuzzer using traditional learning.",
			false, true);


	private Map<String, DataType> types;

	private ParameterizedSymbol[] inputs;

	private ParameterizedSymbol[] outputs;

	private DtlsAdapterConfig dtlsAdapterConfig;

	private boolean analyzerLearnConcretization;

	public void parseConfig(Configuration config) throws ConfigurationException {
		this.types = new LinkedHashMap<String,DataType>();

		Function<String[], String[]> commentFilter = (msgArray) -> Arrays.stream(msgArray)
				.filter(msgStr -> !msgStr.trim().startsWith("!"))
				.toArray(String[]::new); // filters out messages commented by pre-pending the '!' character
		String[] inpStrings = OPTION_INPUTS.parse(config).split("\\+");
		inpStrings = commentFilter.apply(inpStrings);
		this.inputs = parseSymbols(inpStrings, types, true);
		String[] outStrings = OPTION_OUTPUTS.parse(config).split("\\+");
		outStrings = commentFilter.apply(outStrings);
		this.outputs = parseSymbols(outStrings, types, false);
        this.dtlsAdapterConfig = parseDTLSAdapterConfig(config);
	}

	private DtlsAdapterConfig parseDTLSAdapterConfig(Configuration config) throws ConfigurationException {
		String fuzzerAddress = OPTION_DTLSFUZZER_ADDRESS.parse(config);
		Integer fuzzerPort = OPTION_DTLSFUZZER_PORT.parse(config);
		String fuzzerCommand = OPTION_DTLSFUZZER_COMMAND.parse(config);
		String fuzzerDirectory = OPTION_DTLSFUZZER_DIRECTORY.parse(config);
		Boolean outputEnabled = OPTION_DTLSFUZZER_OUTPUT.parse(config);
		String analyzerAddress = OPTION_ANALYZER_ADDRESS.parse(config);
		Integer analyzerPort = OPTION_ANALYZER_PORT.parse(config);
		DtlsAdapterConfig adapter =  new DtlsAdapterConfig(fuzzerAddress, fuzzerPort, fuzzerCommand, fuzzerDirectory, outputEnabled, analyzerAddress, analyzerPort);
		analyzerLearnConcretization = OPTION_ANALYZER_LEARN_CONCRETIZATION.parse(config);
		return adapter;
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

	public ParameterizedSymbol[] getInputs() {
		return this.inputs;
	}

	public ParameterizedSymbol[] getOutput() {
		return this.outputs;
	}

	public Map<String, DataType> getTypes() {
		return this.types;
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
        return Pair.of(cl, parts[1].trim());
    }

    private DataType getOrCreate(String name, Class<?> base, Map<String, DataType> map) {
        DataType ret = map.get(name);
        if (ret == null) {
            ret = new DataType(name, base);
            map.put(name, ret);
        }
        return ret;
    }


    private Class<?> loadClass(String name) throws ConfigurationException {
    	try {
			return Class.forName(name.trim());
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(e.getMessage());
		}
    }

	public DtlsAdapterSULFactory newSULFactory() {
	    DtlsAdapterSULFactory sulFactory = new DtlsAdapterSULFactory(inputs, outputs, dtlsAdapterConfig);
		return sulFactory;
	}
}
