package de.learnlib.ralib.example.login;

import java.util.HashMap;

public class AbstractMultiLogin {
	
// Initialize statemachine constants,variables and locations
    
    HashMap < Integer,Integer > id2pwd = new HashMap < Integer,Integer > ();
    HashMap < Integer,Boolean > id2loggedin = new HashMap < Integer,Boolean > ();
	
    int MAX_REGISTERED_USERS=2;
	int MAX_LOGGEDIN_USERS=1000000;
	int loggedin_users=0;
	
    
    
    /* login an user with uid
     * 
     * notes:
     *   - An user can only login, if the uid  
     *       + is registered 
     *       + and is not logged in
     *   - at max only MAX_LOGGEDIN_USERS users may be logged in 
     */   
    public boolean ILogin(Integer uid,Integer pwd) {
        if ( id2pwd.containsKey(uid) 
        		&& ! id2loggedin.get(uid) 
        		&& pwd == id2pwd.get(uid)
        		&& loggedin_users < MAX_LOGGEDIN_USERS 
            ) {
            loggedin_users++;
        	id2loggedin.put(uid, true );
        	return true;
        } 
        return false;
    }
    
    /* ILogout
     * 
     * A user can only logout when logged in.
     */
    public boolean ILogout(Integer uid) {
        if ( id2loggedin.containsKey(uid) && id2loggedin.get(uid) ) {
        	id2loggedin.put(uid, false );
            loggedin_users--;
            return true;
        } 
        return false;
    }
    
 
    /* IChangePassword
     * 
     * a  user can only change password when logged in
     */
    public boolean IChangePassword(Integer uid, Integer pwd) {

        if (  id2loggedin.containsKey(uid) &&  id2loggedin.get(uid) ) {
        	id2pwd.put(uid, pwd);
            return true;        	
        } 
        return false;
    }

	public void setMaxRegUsers(int maxRegUsers) {
		MAX_REGISTERED_USERS = maxRegUsers;
	}  
}
