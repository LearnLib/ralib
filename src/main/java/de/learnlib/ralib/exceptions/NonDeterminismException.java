package de.learnlib.ralib.exceptions;

/**
 * Exception throw if the output in the Cache does not correspond with that received from the SUL.
 */
public class NonDeterminismException extends DecoratedRuntimeException{
	private static final long serialVersionUID = 1L;

	public NonDeterminismException() {
		super();
	}
	
	public NonDeterminismException(String message) {
		super(message);
	}
}
