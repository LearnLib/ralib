/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 */
public abstract class SDTGuard {
    
    //TODO: this should probably be a special sdtparameter
    private final SuffixValue parameter;
    
    //TODO: this should be either a register or a special sdtregister
    private final Map<Register, Relation> regrels;
    
//    private final Register register;
//    private final Relation relation;
    
    
    public SuffixValue getParameter() {
        return this.parameter;
    }

    public Set<Register> getRegisters() {
        return regrels.keySet();
    }
    
    public Relation getRelation(Register reg) {
        return regrels.get(reg);
    }
    
    public Map<Register, Relation> getRegsAndRels() {
        return this.regrels;
    }
    
//    public Register getRegister() {
//        return this.register;        
//    }
    
//    public Relation getRelation() {
//        return this.relation;
//    }
    
    public SDTGuard(SuffixValue param, Register reg, Relation rel) {
        
//        this.parameter = param;
//        this.register = reg;
//        this.relation = rel;
//  
        this.regrels = new HashMap<>();
        this.parameter = param;
        this.regrels.put(reg,rel);
    }
    
//    public Guard createCopy(VarMapping renaming) {
//        return this;
//    }
    
    public SDTGuard[] or(SDTGuard... guards) {
    //        Guard[] guardList = new Guard[guards.length];
    //        for (int i=0; i<guards.length; i++) {
    //            guardList[i] = guards[i];
    //        }
        
        // check feasibility here...
        
            return guards;
        }
    
    public boolean equals(SDTGuard other) {
        return (this.parameter == other.getParameter() &&
//                this.register == other.getRegister() &&
//                this.relation == other.getRelation());
                this.regrels == other.getRegsAndRels());
    }
    
    //TODO: this method should not be in this class unless it applies to every kind of guard...
    // Fix this method for inequality
//    public boolean contradicts(SDTGuard other) {
//        boolean samePR = (this.parameter.getId() == other.getParameter().getId() && 
//     //           this.register.getVC() == other.getRegister().getVC() &&
//                this.register.getId() == other.getRegister().getId());
//        //System.out.println("params " + this.parameter.toString() + " and " + other.getParameter().toString() + "\nregs " + this.register.toString() + " and " + other.getRegister().toString() + "? : " + samePR);
//        boolean contRels = ((this.relation.equals(Relation.SMALLER) && 
//                (other.getRelation().equals(Relation.BIGGER))) ||
//                (this.relation.equals(Relation.BIGGER) &&
//                (other.getRelation().equals(Relation.SMALLER))));
//        //System.out.println("rels contradict: " + contRels);
//        return samePR && contRels;
//    }
    
    @Override
    public String toString() {
        String ret = "";
        for (Register r : this.regrels.keySet()) {
            ret = ret + " " + r + " " + regrels.get(r) + " " + this.parameter;
        }
        return ret;
//        return this.register + " " + this.relation + " " + this.parameter;
    }

    /**
     * 
     * @param variables
     * @return 
     */
    public abstract Expression<Boolean> getGuardExpression(
            Map<SymbolicDataValue, Variable> variables);
    
}
