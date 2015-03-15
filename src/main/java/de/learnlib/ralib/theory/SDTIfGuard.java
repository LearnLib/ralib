/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Map;
import java.util.Objects;


public abstract class SDTIfGuard extends SDTGuard {
    
    private final SymbolicDataValue register;
    private final Relation relation;
    
    
    public SymbolicDataValue getRegister() {
        return this.register;
    }
    
    public Relation getRelation() {
    //    return regrels.get(reg);
        return this.relation;
    }
    
    public SDTIfGuard(SuffixValue param, SymbolicDataValue reg, Relation rel) {
        super(param);
        this.relation = rel;
        this.register = reg;
    }   

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 97 * hash + Objects.hashCode(this.register);
        hash = 97 * hash + Objects.hashCode(this.relation);
        return hash;
    }

   
   @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SDTIfGuard other = (SDTIfGuard) obj;
        if (!Objects.equals(this.register, other.register)) {
            return false;
        }
        if (!Objects.equals(this.relation, other.relation)) {
            return false;
        }
        return super.equals(obj) && true;
    } 

    
    @Override
    public abstract TransitionGuard toTG(Map<SymbolicDataValue, Variable> variables);
    
    @Override
    public abstract Expression<Boolean> toExpr();
        
    @Override
    public abstract SDTIfGuard relabel(VarMapping relabelling);
    
}