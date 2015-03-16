/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.*;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Map;
import java.util.Set;

@Deprecated
public abstract class SDTElseGuard extends SDTGuard {
    
    private final Set<SymbolicDataValue.Register> registers;
    
    public Set<SymbolicDataValue.Register> getRegisters() {
        return this.registers;
    }
    
    //public abstract Set<Expression<Boolean>> toExprs();
    
    private final Relation relation;
    
    public Relation getRelation() {
    //    return regrels.get(reg);
        return this.relation;
    }
    
    public SDTElseGuard(SuffixValue param, Set<Register> regs, Relation rel) {
        super(param);
        this.relation = rel;
        this.registers = regs;
    }
    
    @Override
    public abstract IfGuard toTG(Map<SymbolicDataValue, Variable> variables);
    
}
