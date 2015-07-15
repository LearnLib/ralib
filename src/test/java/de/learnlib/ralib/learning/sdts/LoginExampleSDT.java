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
package de.learnlib.ralib.learning.sdts;

import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;

/**
 *
 * @author falk
 */
public class LoginExampleSDT implements SymbolicDecisionTree {

    public static enum SDTClass {ACCEPT, REJECT, LOGIN};
    
    private final SDTClass clazz;
    
    private final SymbolicSuffix suffix;

    private final Set<Register> registers;
    
    public LoginExampleSDT(SDTClass clazz, SymbolicSuffix suffix, Set<Register> registers) {
        this.clazz = clazz;
        this.suffix = suffix;
        this.registers = registers;
    }

    @Override
    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming) {
        if (!(other.getClass().equals(this.getClass()))) {
            return false;
        }
        
        LoginExampleSDT sdt = (LoginExampleSDT) other;
        return (clazz == sdt.clazz) &&
                (registers.equals(sdt.registers)) &&
                (suffix.equals(sdt.suffix));
    }
    
    @Override
    public LoginExampleSDT relabel(VarMapping relabelling) {
        return this;
    }    

    @Override
    public String toString() {
        return "[" + clazz + "]";
    }
    
    @Override
    public boolean isAccepting() {
        return clazz == SDTClass.ACCEPT;
    }
    
    @Override
    public Set<Register> getRegisters() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
        
}
