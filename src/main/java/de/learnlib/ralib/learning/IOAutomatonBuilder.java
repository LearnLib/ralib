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
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
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
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
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
        DataExpression<Boolean> expr = guard.getCondition().toDataExpression();
        
        Map<Expression, SymbolicDataValue> exprmap = new LinkedHashMap<>();
        for (Entry<SymbolicDataValue, Variable> e : expr.getMapping().entrySet()) {
            exprmap.put(e.getValue(), e.getKey());
        }
        
        VarMapping<Parameter, SymbolicDataValue> outmap = new VarMapping<>();        
        analyzeExpression(expr.getExpression(), outmap, exprmap);
        
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
    
    private void analyzeExpression(Expression<Boolean> expr, 
            VarMapping<Parameter, SymbolicDataValue> outmap, 
            Map<Expression, SymbolicDataValue> exprmap) {
        if (expr instanceof PropositionalCompound) {
            PropositionalCompound pc = (PropositionalCompound) expr;
            analyzeExpression(pc.getLeft(), outmap, exprmap);
            analyzeExpression(pc.getRight(), outmap, exprmap);
        }
        else if (expr instanceof NumericBooleanExpression) {
            NumericBooleanExpression nbe = (NumericBooleanExpression) expr;
            if (nbe.getComparator() == NumericComparator.EQ) {
                SymbolicDataValue left = parseValue(nbe.getLeft(), exprmap);
                SymbolicDataValue right = parseValue(nbe.getRight(), exprmap);
                
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

    private SymbolicDataValue parseValue(Expression<?> expr, 
            Map<Expression, SymbolicDataValue> exprmap) {
        
        if (expr instanceof gov.nasa.jpf.constraints.expressions.Constant) {
            gov.nasa.jpf.constraints.expressions.Constant c =
                    (gov.nasa.jpf.constraints.expressions.Constant)expr;
            
            return reverseConsts.get(c.getValue());
        }
        
        return exprmap.get(expr);
    }
    
    
}
