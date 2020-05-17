package de.learnlib.ralib.example.login;

public class MultiLogin extends AbstractMultiLogin{

	//handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public boolean IRegister(Integer uid, Integer pwd) {
        if ( ! id2pwd.containsKey(uid)  && id2pwd.keySet().size() < MAX_REGISTERED_USERS ) {
        	id2pwd.put(uid, pwd);
        	id2loggedin.put(uid, false);
        	return true;
        } 
        return false;
    }
}
