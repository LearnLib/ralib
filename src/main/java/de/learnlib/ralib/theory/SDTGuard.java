/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Map;

/**
 *
 * @author falk
 */
public abstract class SDTGuard {
    
    //TODO: this should probably be a special sdtparameter
    private final SuffixValue parameter;
    
    //TODO: this should be either a register or a special sdtregister
    //private final Register register;
    
//    private final Register register;
//    private final Relation relation;
//    
    
    public SuffixValue getParameter() {
        return this.parameter;
    }

//    public Register getRegister() {
//        return this.register;
//    }
    
    
    //public Map<Register, Relation> getRegsAndRels() {
    //    return this.regrels;
    //}
    
//    public Register getRegister() {
//        return this.register;        
//    }
    
//    public Relation getRelation() {
//        return this.relation;
//    }
    
    public SDTGuard(SuffixValue param) {
        
        this.parameter = param;
    
//        this.regrels = new LinkedHashMap<>();
//        this.parameter = param;
//        this.regrels.put(reg,rel);
    }
    
//    public Guard createCopy(VarMapping renaming) {
//        return this;
//    }
    
    
    
    
    
    //TODO: this method should not be in this class unless it applies to every kind of guard...
    // Fix this method for inequality
    

    
    /**
     * 
     * @param variables
     * @return 
     */
    //public abstract Expression<Boolean> getGuardExpression(
    //        Map<SymbolicDataValue, Variable> variables);
    
    public abstract TransitionGuard toTG(Map<SymbolicDataValue, Variable> variables);
    
    public abstract Expression<Boolean> toExpr();
}
