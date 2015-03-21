/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Map;


public abstract class SDTIfGuard extends SDTGuard {
    
    protected final SymbolicDataValue register;
    protected final Relation relation;
    
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
    public abstract TransitionGuard toTG(Map<SymbolicDataValue, Variable> variables, Constants consts);
    
    @Override
    public abstract Expression<Boolean> toExpr(Constants consts);
        
    @Override
    public abstract SDTIfGuard relabel(VarMapping relabelling);
    
    @Override
    public abstract SDTIfGuard relabelLoosely(VarMapping relabelling);
    
    public abstract SDTIfGuard toDeqGuard();
}