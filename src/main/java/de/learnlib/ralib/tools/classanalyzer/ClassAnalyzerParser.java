package de.learnlib.ralib.tools.classanalyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.SULParser;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class ClassAnalyzerParser extends SULParser{
    private static final ConfigurationOption.StringOption OPTION_TARGET
    = new ConfigurationOption.StringOption("target",
            "traget class name", null, false);

	private static final ConfigurationOption.StringOption OPTION_CONFIG
	= new ConfigurationOption.StringOption("config",
	    "sets class fields to given values after instantiation, format: field:value[;field:value]* ", null, true);
	
	private static final ConfigurationOption.StringOption OPTION_METHODS
	    = new ConfigurationOption.StringOption("methods",
	            "traget method signatures. format: m1(class:type,class:type)class:type + m2() + ...", null, false);
	
    protected static final ConfigurationOption.BooleanOption OPTION_OUTPUT_ERROR
    = new ConfigurationOption.BooleanOption("output.error",
            "Include error output", true, true);

    protected static final ConfigurationOption.BooleanOption OPTION_OUTPUT_NULL
    = new ConfigurationOption.BooleanOption("output.null",
            "Include null output", true, true);
    protected static final ConfigurationOption.IntegerOption OPTION_MAX_DEPTH
    = new ConfigurationOption.IntegerOption("max.depth",
            "Maximum depth to explore", -1, true);
	
    
    private Map<String, DataType> types;
    private Map<ParameterizedSymbol, MethodConfig> methods;
	private ParameterizedSymbol[] inputSymbols;

	private ParameterizedSymbol[] outputSymbols;

	private FieldConfig fieldConfig;

	private Class<?> target;

	private Integer md;

	
	public void parseConfig(Configuration config) throws ConfigurationException{
		List<ParameterizedSymbol> inList = new ArrayList<>();
        List<ParameterizedSymbol> outList = new ArrayList<>();
        this.types = new LinkedHashMap<String, DataType>();
        this.methods = new LinkedHashMap<>();
        try {
            this.types.put("boolean", SpecialSymbols.BOOLEAN_TYPE);

            String className = OPTION_TARGET.parse(config);
          	this.target = Class.forName(className);
            boolean hasVoid = false;
            boolean hasBoolean = false;

            String[] mcStrings = OPTION_METHODS.parse(config).split("\\+");
            for (String mcs : mcStrings) {
                MethodConfig mc = new MethodConfig(mcs, target, this.types);
                this.methods.put(mc.getInput(), mc);
                inList.add(mc.getInput());
                if (!mc.isVoid() && !mc.getRetType().equals(SpecialSymbols.BOOLEAN_TYPE)) {
                    outList.add(mc.getOutput());
                }
                hasVoid = hasVoid || mc.isVoid();
                hasBoolean = hasBoolean || mc.getRetType().equals(SpecialSymbols.BOOLEAN_TYPE);
            }
            
            this.fieldConfig = null;
            String fieldConfigString = OPTION_CONFIG.parse(config);
            if (fieldConfigString != null) {
            	String[] fieldConfigSplit = fieldConfigString.replaceAll("\\s", "").split("\\;");
            	fieldConfig = new FieldConfig(target, fieldConfigSplit);
            }

            this.inputSymbols = inList.toArray(new ParameterizedSymbol[]{});
            boolean hasError = OPTION_OUTPUT_ERROR.parse(config);
            boolean hasNull = OPTION_OUTPUT_NULL.parse(config);

            if (hasError)
            	outList.add(SpecialSymbols.ERROR);
            if (hasNull)
            	outList.add(SpecialSymbols.NULL);
            if (hasVoid)
            	outList.add(SpecialSymbols.VOID);
            if (hasBoolean) {
	            outList.add(SpecialSymbols.TRUE);
	            outList.add(SpecialSymbols.FALSE);
            }
            this.md = OPTION_MAX_DEPTH.parse(config);
            if (!md.equals(-1))
            	outList.add(SpecialSymbols.DEPTH);
            this.outputSymbols = outList.toArray(new ParameterizedSymbol[]{});
        }catch(Exception e){
        	throw new ConfigurationException(e.getMessage());
        }
	}
	

	@Override
	public ParameterizedSymbol[] getInputs() {
		return this.inputSymbols;
	}

	@Override
	public ParameterizedSymbol[] getOutput() {
		return this.outputSymbols;
	}

	@Override
	public DataWordSUL newSUL() {
		return new ClasssAnalyzerDataWordSUL(this.target, this.methods, this.md, this.fieldConfig);
	}

	@Override
	public Map<String, DataType> getTypes() {
		return this.types;
	}

}
