package de.learnlib.ralib.tools.classanalyzer;

import java.util.Map;

import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.SULFactory;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class ClassAnalyzerSULFactory implements SULFactory{
	private Class<?> sulClass;
	private Map<ParameterizedSymbol, MethodConfig> methods; 
	private int depth;
	
	public ClassAnalyzerSULFactory(Class<?> sulClass, Map<ParameterizedSymbol, MethodConfig> methods, int depth,
			FieldConfig fieldConfiguration) {
		super();
		this.sulClass = sulClass;
		this.methods = methods;
		this.depth = depth;
		this.fieldConfiguration = fieldConfiguration;
	}


	private FieldConfig fieldConfiguration = null;
	

	public DataWordSUL newSUL() {
		ClassAnalyzerDataWordSUL classSUL = new ClassAnalyzerDataWordSUL(sulClass, methods, depth, fieldConfiguration);
		return classSUL;
	}
}
