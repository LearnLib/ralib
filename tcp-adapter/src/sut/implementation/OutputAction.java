package sut.implementation;

import java.util.List;

public class OutputAction extends Action  implements sut.interfaces.OutputAction{

	public OutputAction(String methodName, List<Parameter> parameters) {
		super(methodName, parameters);
	}

	public OutputAction(Action action) {
		super(action);
	}
		
	public OutputAction(String action) {
		super(action);
	}
	
	public OutputAction(sut.interfaces.OutputAction  action) {
		super(action);
	}		

	
}
