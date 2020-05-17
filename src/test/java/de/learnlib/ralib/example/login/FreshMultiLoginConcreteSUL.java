package de.learnlib.ralib.example.login;

import de.learnlib.ralib.utils.ConcreteSULWrapper;

public class FreshMultiLoginConcreteSUL extends ConcreteSULWrapper<FreshMultiLogin>{
	private static Integer MAX_REGISTERED_USERS = 2;
	
	public FreshMultiLoginConcreteSUL() {
		super(FreshMultiLogin.class, 
			() -> {
				FreshMultiLogin sut =  new FreshMultiLogin();
				sut.setMaxRegUsers(MAX_REGISTERED_USERS);
				return sut;
			});
	}
}	
