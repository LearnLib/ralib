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
package de.learnlib.ralib.sul;

/**
 *
 * @author falk
 */
public class Login {
    
    public static class Password {
        private final String hash;

        public Password(String hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            int code = 7;
            code = 97 * code + (this.hash != null ? this.hash.hashCode() : 0);
            return code;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Password other = (Password) obj;
            if ((this.hash == null) ? (other.hash != null) : !this.hash.equals(other.hash)) {
                return false;
            }
            return true;
        }
        
    }
    
    public static enum State { INIT, REGISTERED, LOGGED_IN};
    
    private Password password;
    
    private String username;

    private State state = State.INIT;
            
    public boolean login(String user, Password pass) {
        if (!(state == State.REGISTERED)) {
            return output();
        }
        
        if (user.equals(username) && pass.equals(password)) {
            state = State.LOGGED_IN;
        }
        return output();
    }
    
    public boolean register(String user, Password pass) {
        if (!(state == State.INIT)) {
            return output();
        }
        
        this.username = user;
        this.password = pass;
        
        state = State.REGISTERED;
        return output();
    }

    public State logout() {
        if (!(state == State.LOGGED_IN)) {
            return state;
        }
        
        return state;
    }

    public boolean change(Password oldPW, Password newPW) {
        if (!(state == State.LOGGED_IN)) {
            return output();
        }
        
        if (oldPW.equals(password)) {
            this.password = newPW;
        }
        return output();
    }
    
    public boolean delete(String user, Password pass) {
        if (!(state == State.LOGGED_IN)) {
            return output();
        }
        
        if (user.equals(username) && pass.equals(password)) {
            state = State.INIT;
        }
        return output();
    }
    
    private boolean output() {
        return (state == State.LOGGED_IN);
    }

}
