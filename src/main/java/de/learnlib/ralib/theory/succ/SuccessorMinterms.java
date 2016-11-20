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
package de.learnlib.ralib.theory.succ;

/**
 *
 * @author falk
 */
public enum SuccessorMinterms {    
    EQUAL, // to some value         (=> !succ  /\ in_window /\ in_halfspace /\ !other)
    SUCC,  // succ to some value    (=> !equal /\ in_window /\ in_halfspace /\ !other)
    IN_WINDOW, // in front of val   (=> !equal /\ !succ     /\ in_halfspace /\ !other)
    IN_HALFSPACE, //                (=> !equal /\ !succ     /\ !in_window   /\ !other)
    OTHER; //                        (=> !equal /\ !succ     /\ !in_window   /\ !in_halfspace)
    
    
    public static SuccessorMinterms forInt(int i) {
        switch (i) {
            case 0: return OTHER;
            case 1: return EQUAL;
            case 2: return SUCC;
            case 3: return IN_WINDOW;
            case 4: return IN_HALFSPACE;
        }
        throw new IllegalStateException();
    }
}
