package de.learnlib.ralib.example.login;

import java.util.Random;


public class FreshMultiLogin extends AbstractMultiLogin {
	
	private Random random = new Random();

	//handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public Integer IRegister(Integer uid) {
        Integer pwd = random.nextInt(10000000);
        if ( ! id2pwd.containsKey(uid)  && id2pwd.keySet().size() < MAX_REGISTERED_USERS ) {
        	id2pwd.put(uid, pwd);
        	id2loggedin.put(uid, false);
        } 
        return pwd;
    }    
}
