package sut.implementation;

import java.util.List;

public class Parameter implements sut.interfaces.Parameter {
	private int value;
	private int parameterIndex;
	private Action action;

	public Parameter(int value, 
					 int parameterIndex) {
		this.value = value;
		this.parameterIndex = parameterIndex;
	}

	public Parameter(int value, 
			 int parameterIndex, Action action) {
		this.value = value;
		this.parameterIndex = parameterIndex;
		this.action = action;
	}

	public int getValue() {
		return value;
	}

	public int getParameterIndex() {
		return parameterIndex;
	}
	
	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Parameter)) {
			return false;
		}
		
		Parameter that = (Parameter)obj;
		
		if (value != that.value) {
			return false;
		}
		
		if (parameterIndex != that.parameterIndex) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return parameterIndex;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}	
}
