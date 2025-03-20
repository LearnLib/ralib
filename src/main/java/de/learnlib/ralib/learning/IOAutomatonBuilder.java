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
package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
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
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class IOAutomatonBuilder extends AutomatonBuilder {

    private final Map<Object, Constant> reverseConsts;

    public IOAutomatonBuilder(Map<Word<PSymbolInstance>, LocationComponent> components,
            Constants consts) {
        super(components, consts);

        this.reverseConsts = new LinkedHashMap<>();
        for (Entry<Constant, DataValue> c : consts) {
            reverseConsts.put(c.getValue().getValue(), c.getKey());
        }
    }

    public IOAutomatonBuilder(Map<Word<PSymbolInstance>, LocationComponent> components,
            Constants consts, DT dt) {
    	super(components, consts, dt);

        this.reverseConsts = new LinkedHashMap<>();
        for (Entry<Constant, DataValue> c : consts) {
            reverseConsts.put(c.getValue().getValue(), c.getKey());
        }
    }

    @Override
    protected Transition createTransition(ParameterizedSymbol action,
                                          Expression<Boolean> guard, RALocation src_loc, RALocation dest_loc,
                                          Assignment assign) {

        if (!dest_loc.isAccepting()) {
            return null;
        }

        if (!(action instanceof OutputSymbol)) {
            return super.createTransition(action, guard, src_loc, dest_loc, assign);
        }

        //IfGuard _guard = (IfGuard) guard;
        Expression<Boolean> expr = guard;

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

        return new OutputTransition(ExpressionUtil.TRUE,
                outMap, (OutputSymbol) action, src_loc, dest_loc, assign);
    }

    private void analyzeExpression(Expression<Boolean> expr,
            VarMapping<Parameter, SymbolicDataValue> outmap) {

        if (expr instanceof PropositionalCompound pc) {
            analyzeExpression(pc.getLeft(), outmap);
            analyzeExpression(pc.getRight(), outmap);
        }
        else if (expr instanceof NumericBooleanExpression nbe) {
            if (nbe.getComparator() == NumericComparator.EQ) {
                // FIXME: this is unchecked!
                //System.out.println(expr);
                SymbolicDataValue left = (SymbolicDataValue) nbe.getLeft();
                SymbolicDataValue right = (SymbolicDataValue) nbe.getRight();

                Parameter p = null;
                SymbolicDataValue sv = null;

                if (left instanceof Parameter) {
                    if (right instanceof Parameter) {
                        throw new UnsupportedOperationException("not implemented yet.");
                    }
                    else {
                        p = (Parameter) left;
                        sv = (SymbolicDataValue) right;
                    }
                }
                else {
                    p = (Parameter) right;
                    sv = (SymbolicDataValue) left;
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
