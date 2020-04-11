package de.learnlib.ralib.tools.sulanalyzer;

import java.util.Arrays;
import java.util.LinkedHashMap;

import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.SULFactory;
import de.learnlib.ralib.tools.classanalyzer.FieldConfig;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class SULAnalyzerFactory implements SULFactory{
	private Class<? extends ConcreteSUL> sulClass;
	private LinkedHashMap<String, ParameterizedSymbol> outputLookup;
	private FieldConfig fieldConfigurator;

	public SULAnalyzerFactory(Class<? extends ConcreteSUL> sulClass, ParameterizedSymbol [] outputs, FieldConfig fieldConfiguration) {
		this.sulClass = sulClass;
        this.outputLookup = new LinkedHashMap<>();
        Arrays.asList(outputs).forEach(out ->  
        this.outputLookup.put(out.getName(), out));
        this.fieldConfigurator = fieldConfiguration;
	}
	

	public DataWordSUL newSUL() {
		ConcreteDataWordWrapper sul = new ConcreteDataWordWrapper(this.sulClass, this.outputLookup, this.fieldConfigurator);
		return sul;
	}
}
