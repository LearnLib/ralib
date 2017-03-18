package de.learnlib.ralib.tools.sulanalyzer;

import java.util.Arrays;

public abstract class ConcreteMessage {
	private String methodName;
	private Object [] parameters;
	
	public ConcreteMessage(String methodName, Object[] parameters) {
		super();
		this.methodName = methodName;
		this.parameters = parameters;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() +" [methodName=" + methodName + ", parameters=" + Arrays.toString(parameters) + "]";
	}
	

	public String getMethodName() {
		return methodName;
	}

	public Object[] getParameterValues() {
		return parameters;
	}

}
