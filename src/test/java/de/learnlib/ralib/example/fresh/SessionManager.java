/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.example.fresh;

import java.util.HashMap;
import java.util.Random;


public class SessionManager {

    // Initialize statemachine constants,variables and locations
    HashMap< Integer, Integer> id2sid = new HashMap<>();
    HashMap< Integer, Boolean> id2loggedin = new HashMap<>();

    private final int MAX_SESSIONS = 3;
    private final int MAX_LOGGEDIN_USERS = 1000000;
    private int loggedin_users = 0;
    private final Random random = new Random();

    //handling each Input

    /* register an uid
     * 
     * notes:
     *   - you can only register once for a specific uid
     *   - at max only MAX_REGISTERED_USERS may be registered 
     */
    public Integer ISession(Integer uid) {
    	Integer sid;
        if (!id2sid.containsKey(uid) && id2sid.keySet().size() < MAX_SESSIONS) {
        	sid = random.nextInt(10000000);
        	id2sid.put(uid, sid);
        	id2loggedin.put(uid, false);
        } else {
        	if (id2sid.containsKey(uid)) 
        		sid = id2sid.get(uid);
        	else
        		sid = random.nextInt(10000000);
        }
        return sid;
    }    
    
    /* login an user with uid
     * 
     * notes:
     *   - An user can only login, if the uid  
     *       + is registered 
     *       + and is not logged in
     *   - at max only MAX_LOGGEDIN_USERS users may be logged in 
     */   
    public boolean ILogin(Integer uid, Integer pwd) {
        if (id2sid.containsKey(uid)
                && !id2loggedin.get(uid)
                && pwd == id2sid.get(uid)
                && loggedin_users < MAX_LOGGEDIN_USERS) {
            loggedin_users++;
            id2loggedin.put(uid, true);
        	return true;
        } 
        return false;
    }
    
    /* ILogout
     * 
     * A user can only logout when logged in.
     */
    public boolean ILogout(Integer uid) {
        if (id2loggedin.containsKey(uid) && id2loggedin.get(uid)) {
            id2loggedin.put(uid, false);
            loggedin_users--;
            return true;
        } 
        return false;
    }
}
