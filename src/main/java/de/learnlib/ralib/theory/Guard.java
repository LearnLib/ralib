/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import java.util.ArrayList;

/**
 *
 * @author falk
 */
public class Guard {
    
    private final SymbolicDataValue parameter;
    private final SymbolicDataValue register;
    
    public SymbolicDataValue getParameter() {
        return this.parameter;
    }
    
    public SymbolicDataValue getRegister() {
        return this.register;        
    }
    
    public Guard(SymbolicDataValue param, SymbolicDataValue reg) {
        this.parameter = param;
        this.register = reg;
    }
    
    public Guard createCopy(VarMapping renaming) {
        return this;
    }
    
    public Guard[] or(Guard... guards) {
    //        Guard[] guardList = new Guard[guards.length];
    //        for (int i=0; i<guards.length; i++) {
    //            guardList[i] = guards[i];
    //        }
        
        // check feasibility here...
        
            return guards;
        }
    
    }
