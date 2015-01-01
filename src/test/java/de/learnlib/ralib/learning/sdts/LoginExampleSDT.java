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

package de.learnlib.ralib.learning.sdts;

import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import java.util.Set;

/**
 *
 * @author falk
 */
public class LoginExampleSDT extends SymbolicDecisionTree {
    
    public static enum SDTClass {ACCEPT, REJECT, LOGIN};
    
    private final SDTClass clazz;
    
    private final SymbolicSuffix suffix;

    private final Set<Register> registers;
    
    public LoginExampleSDT(SDTClass clazz, SymbolicSuffix suffix, Set<Register> registers) {
        super( clazz == SDTClass.ACCEPT, null, null);
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
    public boolean canUse(SymbolicDecisionTree other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String toString() {
        return "[" + clazz + "]";
    }
    

}
