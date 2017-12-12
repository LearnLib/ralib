package sut.implementation;

import java.util.ArrayList;
import java.util.List;



public abstract class Action implements sut.interfaces.Action {
	private String methodName;
	private ArrayList<Parameter> parameters;

	public String getMethodName() {
		return methodName;
	}

	public List<Parameter> getParameters() {
		return new ArrayList<Parameter>(parameters);
	}
	
	public List<sut.interfaces.Parameter> getParams() {
		 ArrayList<sut.interfaces.Parameter> iparams = new ArrayList<sut.interfaces.Parameter> ();  
		 for (Parameter parameter : parameters) {
			 iparams.add(parameter);
		 }
		 return iparams ;
	}	

	public Parameter getParam(int index) {
		return this.parameters.get(index);
	}
	
	public Parameter getParam(Integer index){
		return this.parameters.get(index.intValue());
	}

	public Action(String methodName, List<Parameter> parameters) {
		this.methodName = methodName.intern();
		this.parameters = new ArrayList<Parameter>();
		for (Parameter parameter : parameters) {
			Parameter newParam = new Parameter(parameter.getValue(), parameter.getParameterIndex());
			newParam.setAction(this);
			this.parameters.add(newParam);
		}
	}
	
	public Action(Action action) {
		this(action.getMethodName(), action.getParameters());
	}
	
	public Action(sut.interfaces.Action action) {
		this.methodName = action.getMethodName().intern();
		this.parameters = new ArrayList<Parameter>();
		for ( sut.interfaces.Parameter p: action.getParams() ) {
			Parameter newParam=new Parameter(p.getValue(), p.getParameterIndex());
			newParam.setAction(this);
			this.parameters.add(newParam);							
		}			
	}		
	
	public Action(String actionString) {
		String[] action = actionString.split("_");

		if (action.length < 1) {
			System.out.println("Error handling abstract input: " + actionString);
			throw new RuntimeException("Error handling abstract input: " + actionString);
		}
		
		methodName = action[0].intern();
		parameters = new ArrayList<Parameter>();

		if (action.length > 1) {
			int paramIndex = 0;
			for (int i = 1; i < action.length; i++) {
				String args = action[i];
				
				Integer value;
				try {
					value = new Integer(args);
				} catch (NumberFormatException ex) {
					System.out.println("Error parsing abstract input value: " + args + " in action: " + actionString);
					throw new RuntimeException("Error parsing abstract input value: " + args + " in action: " + actionString);
				}
		
				parameters.add(new Parameter(value, paramIndex, this));

				paramIndex++;
			}
		}
	}

	public String toString() {
		StringBuilder result = new StringBuilder(methodName);

		if (parameters.size() != 0) {
			result.append(" ");
			for (Parameter p : parameters) {
				result.append(p.toString()).append(" ");
			}
		}

		return result.toString().trim();
	}		

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof Action))
			return false;

		Action that = (Action) obj;

		if (!methodName.equals(that.methodName)) {
			return false;
		}

		if (!parameters.equals(that.parameters)) {
			return false;
		}

		return true;
	}
	
	@Override
	public int hashCode() {
		return methodName.hashCode() + parameters.hashCode();
	}
	
	public String getValuesAsString() {
		String result = getMethodName();
		
		if (getParameters().size() > 0) {
			for (Parameter parameter : getParameters()) {
					result += "_" + parameter.getValue();
			}
		}
		return result;
	}
}
