/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
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
