package sut.interfaces;



public interface SutInterface {


	
	// process input to an output 
	public OutputAction sendInput(InputAction inputAction);

	// reset SUT
	public void sendReset();


	
}
