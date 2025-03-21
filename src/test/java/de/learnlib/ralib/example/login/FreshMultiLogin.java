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
package de.learnlib.ralib.example.login;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class FreshMultiLogin {

    // Initialize statemachine constants,variables and locations
    HashMap< java.math.BigDecimal, java.math.BigDecimal> id2pwd = new HashMap<>();
    HashMap< java.math.BigDecimal, Boolean> id2loggedin = new HashMap<>();

    private final int MAX_REGISTERED_USERS = 3;
    private final int MAX_LOGGEDIN_USERS = 1000000;
    private int loggedin_users = 0;
    private final Random random = new Random();

    //handling each Input

    /* register a uid
     *
     * notes:
     *   - you can only register once for a specific uid
     *   - at most MAX_REGISTERED_USERS may be registered
     */
    public java.math.BigDecimal IRegister(java.math.BigDecimal uid) {
        java.math.BigDecimal pwd = new BigDecimal(random.nextInt(10000000));
        if (!id2pwd.containsKey(uid) && id2pwd.keySet().size() < MAX_REGISTERED_USERS) {
            id2pwd.put(uid, pwd);
            id2loggedin.put(uid, false);
        }
        return pwd;
    }

    /* login a user with uid
     *
     * notes:
     *   - A user can only login, if the uid
     *       + is registered
     *       + and is not logged in
     *   - at most MAX_LOGGEDIN_USERS users may be logged in
     */
    public boolean ILogin(java.math.BigDecimal uid, java.math.BigDecimal pwd) {
        if (id2pwd.containsKey(uid)
                && !id2loggedin.get(uid)
                && pwd == id2pwd.get(uid)
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
    public boolean ILogout(java.math.BigDecimal uid) {
        if (id2loggedin.containsKey(uid) && id2loggedin.get(uid)) {
            id2loggedin.put(uid, false);
            loggedin_users--;
            return true;
        }
        return false;
    }

    /* IChangePassword
     *
     * A user can only change password when logged in
     */
    public boolean IChangePassword(java.math.BigDecimal uid, java.math.BigDecimal pwd) {

        if (id2loggedin.containsKey(uid) && id2loggedin.get(uid)) {
            id2pwd.put(uid, pwd);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        Class<?> cls = FreshMultiLogin.class;
        for (Method meth : cls.getMethods()) {
            System.out.println(Arrays.asList(meth.getParameterTypes()));
        }
    }
}
