package sut.implementation;

import java.util.List;

public class InputAction extends Action  implements sut.interfaces.InputAction{

	public InputAction(String methodName, List<Parameter> parameters) {
		super(methodName, parameters);
	}

	public InputAction(Action action) {
		super(action);
	}
	
	public InputAction(String action) {
		super(action);
	}

	public InputAction(sut.interfaces.InputAction  action) {
		super(action);
	}	

}
