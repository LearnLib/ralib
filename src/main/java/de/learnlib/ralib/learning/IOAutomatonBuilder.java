/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.learning;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
class IOAutomatonBuilder extends AutomatonBuilder {

    private final Map<Object, Constant> reverseConsts;
    
    public IOAutomatonBuilder(Map<Word<PSymbolInstance>, Component> components, 
            Constants consts) {
        super(components, consts);
        
        this.reverseConsts = new LinkedHashMap<>();
        for (Entry<Constant, DataValue<?>> c : consts) {
            reverseConsts.put(c.getValue().getId(), c.getKey());
        }
    }

    @Override
    protected Transition createTransition(ParameterizedSymbol action, 
            TransitionGuard guard, RALocation src_loc, RALocation dest_loc, 
            Assignment assign) {
        
        if (!dest_loc.isAccepting()) {
            return null;
        }
        
        if (!(action instanceof OutputSymbol)) {
            return super.createTransition(action, guard, src_loc, dest_loc, assign);
        }       
        
        //IfGuard _guard = (IfGuard) guard;        
        GuardExpression expr = guard.getCondition();
        
        VarMapping<Parameter, SymbolicDataValue> outmap = new VarMapping<>();        
        analyzeExpression(expr, outmap);
        
        Set<Parameter> fresh = new LinkedHashSet<>();
        ParameterGenerator pgen = new ParameterGenerator();
        for (DataType t : action.getPtypes()) {
            Parameter p = pgen.next(t);
            if (!outmap.containsKey(p)) {
                fresh.add(p);
            }
        }
                
        OutputMapping outMap = new OutputMapping(fresh, outmap);
        
        return new OutputTransition(new TransitionGuard(), 
                outMap, (OutputSymbol) action, src_loc, dest_loc, assign);
    }
    
    private void analyzeExpression(GuardExpression expr, 
            VarMapping<Parameter, SymbolicDataValue> outmap) {
        
        if (expr instanceof Conjunction) {
            Conjunction pc = (Conjunction) expr;
            for (GuardExpression e : pc.getConjuncts()) {
                analyzeExpression(e, outmap);
            }
        }
        else if (expr instanceof AtomicGuardExpression) {
            AtomicGuardExpression nbe = (AtomicGuardExpression) expr;
            if (nbe.getRelation() == Relation.EQUALS) {
                SymbolicDataValue left = nbe.getLeft();
                SymbolicDataValue right = nbe.getRight();
                
                Parameter p = null;
                SymbolicDataValue sv = null;
                
                if (left instanceof Parameter) {
                    if (right instanceof Parameter) {
                        throw new UnsupportedOperationException("not implemented yet.");
                    }
                    else {
                        p = (Parameter) left;
                        sv = right;
                    }
                }
                else {
                    p = (Parameter) right;
                    sv = left;
                }
                
                outmap.put(p, sv);
            }
        }
        else {
            // true and false ...
            //throw new IllegalStateException("Unsupported: " + expr.getClass());
        }
    } 
    
}
