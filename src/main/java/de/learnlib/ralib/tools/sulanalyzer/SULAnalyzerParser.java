package de.learnlib.ralib.tools.sulanalyzer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.SULParser;
import de.learnlib.ralib.tools.classanalyzer.FieldConfig;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.commons.util.Pair;

public class SULAnalyzerParser extends SULParser {

	private static final ConfigurationOption.StringOption OPTION_TARGET = new ConfigurationOption.StringOption("target",
			"traget SUL class name", null, false);

	private static final ConfigurationOption.StringOption OPTION_CONFIG = new ConfigurationOption.StringOption("config",
			"sets class fields to given values after instantiation, format: field:value[;field:value]* ", null, true);

	private static final ConfigurationOption.StringOption OPTION_INPUTS = new ConfigurationOption.StringOption("inputs",
			"the input param symbols. : input1(class11:type11,class12:type12...)+input2(class21:type21...) + ...", null,
			false);

	private static final ConfigurationOption.StringOption OPTION_OUTPUTS = new ConfigurationOption.StringOption(
			"outputs",
			"the output param symbols. : output1(class11:type11,class12:type12...)+output2(class21:type21...) + ...",
			null, false);
	
	private Class<? extends ConcreteSUL> sulTarget;

	private Map<String, DataType> types;

	private ParameterizedSymbol[] inputs;

	private ParameterizedSymbol[] outputs;

	private FieldConfig fieldConfig;

	public void parseConfig(Configuration config) throws ConfigurationException {
		Class<?> target = loadClass(OPTION_TARGET.parse(config));
		if (!ConcreteSUL.class.isAssignableFrom(target)) {
			throw new ConfigurationException("Target " + target.getName() + " is not of type " + ConcreteSUL.class.getName());
		}
		this.sulTarget = (Class<? extends ConcreteSUL>) target;
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
		 this.fieldConfig = null;
         String fieldConfigString = OPTION_CONFIG.parse(config);
         if (fieldConfigString != null) {
         	String[] fieldConfigSplit = fieldConfigString.replaceAll("\\s", "").split("\\;");
         	fieldConfig = new FieldConfig(target, fieldConfigSplit);
         }
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
		return this.sulTarget.getSimpleName();
	}

	@Override
	public DataWordSUL newSUL() {
		return new ConcreteDataWordWrapper(this.sulTarget, this.getOutput(), fieldConfig);
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
}
