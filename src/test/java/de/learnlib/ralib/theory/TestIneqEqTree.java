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
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class TestIneqEqTree {

    public static final DataType doubleType = new DataType("DOUBLE", Double.class);

    @Test
    public void testIneqEqTree() {

//                Logger root = Logger.getLogger("");
//        root.setLevel(Level.ALL);
//        for (Handler h : root.getHandlers()) {
//            h.setLevel(Level.ALL);
//        }
        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});
//        final ParameterizedSymbol VOID
//                = new OutputSymbol("_void", new DataType[]{});
        final ParameterizedSymbol OUTPUT
                = new OutputSymbol("_out", new DataType[]{doubleType});
        final ParameterizedSymbol OK
                = new OutputSymbol("_ok", new DataType[]{});
        final ParameterizedSymbol NOK
                = new OutputSymbol("_not_ok", new DataType[]{});

        final ParameterizedSymbol POLL = new InputSymbol("poll", new DataType[]{});
        final ParameterizedSymbol OFFER = new InputSymbol("offer", new DataType[]{doubleType});

        List<ParameterizedSymbol> inputList = new ArrayList<ParameterizedSymbol>();
        List<ParameterizedSymbol> outputList = new ArrayList<ParameterizedSymbol>();

        inputList.add(POLL);
        inputList.add(OFFER);
        outputList.add(ERROR);
        outputList.add(OUTPUT);
        outputList.add(OK);
        outputList.add(NOK);

        final ParameterizedSymbol[] inputArray = inputList.toArray(new ParameterizedSymbol[inputList.size()]);
        final ParameterizedSymbol[] outputArray = outputList.toArray(new ParameterizedSymbol[outputList.size()]);

        List<ParameterizedSymbol> actionList = new ArrayList<ParameterizedSymbol>();
        actionList.addAll(inputList);
        actionList.addAll(outputList);
        final ParameterizedSymbol[] actionArray = actionList.toArray(new ParameterizedSymbol[actionList.size()]);

        Map<PriorityQueueSUL.Actions, ParameterizedSymbol> inputs = new LinkedHashMap<PriorityQueueSUL.Actions, ParameterizedSymbol>();
        inputs.put(PriorityQueueSUL.Actions.POLL, POLL);
        inputs.put(PriorityQueueSUL.Actions.OFFER, OFFER);

        Map<PriorityQueueSUL.Actions, ParameterizedSymbol> outputs = new LinkedHashMap<PriorityQueueSUL.Actions, ParameterizedSymbol>();
        outputs.put(PriorityQueueSUL.Actions.ERROR, ERROR);
        //outputs.put(Actions.VOID,VOID);
        outputs.put(PriorityQueueSUL.Actions.OUTPUT, OUTPUT);
        outputs.put(PriorityQueueSUL.Actions.OK, OK);
        outputs.put(PriorityQueueSUL.Actions.NOK, NOK);

        final Constants consts = new Constants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        class Cpr implements Comparator<DataValue<Double>> {

            public int compare(DataValue<Double> one, DataValue<Double> other) {
                return one.getId().compareTo(other.getId());
            }
        }
        teachers.put(doubleType, new InequalityTheoryWithEq<Double>() {
            @Override
            public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
                if (vals.isEmpty()) {
                    return new DataValue(doubleType, 1.0);
                }
                List<DataValue<Double>> potential = getPotential(vals);
                if (potential.isEmpty()) {
                    return new DataValue(doubleType, 1.0);
                }
                //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
                DataValue<Double> biggestDv = Collections.max(potential, new Cpr());
                return new DataValue(doubleType, biggestDv.getId() + 1.0);
            }
            Valuation val = new Valuation();
            private final ConstraintSolverFactory fact = new ConstraintSolverFactory();
            private final ConstraintSolver solver = fact.createSolver("z3");

            private List<Expression<Boolean>> instantiateGuard(SDTGuard g, Valuation val) {
                List<Expression<Boolean>> eList = new ArrayList<Expression<Boolean>>();
                if (g instanceof SDTIfGuard) {
                    // pick up the register
                    SymbolicDataValue si = ((SDTIfGuard) g).getRegister();
                    // get the register value from the valuation
                    DataValue<Double> sdi = new DataValue(doubleType, val.getValue(si.toVariable()));
                    // add the register value as a constant
                    gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, sdi.getId());
                    // add the constant equivalence expression to the list
                    eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, si.toVariable()));

                } else if (g instanceof IntervalGuard) {
                    IntervalGuard iGuard = (IntervalGuard) g;
                    if (!iGuard.isBiggerGuard()) {
                        SymbolicDataValue r = iGuard.getRightReg();
                        DataValue<Double> ri = new DataValue(doubleType, val.getValue(r.toVariable()));
                        gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, ri.getId());
                        // add the constant equivalence expression to the list
                        eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, r.toVariable()));

                    }
                    if (!iGuard.isSmallerGuard()) {
                        SymbolicDataValue l = iGuard.getLeftReg();
                        DataValue<Double> li = new DataValue(doubleType, val.getValue(l.toVariable()));
                        gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, li.getId());
                        // add the constant equivalence expression to the list
                        eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, l.toVariable()));

                    }
                }
                return eList;
            }

            @Override
            public DataValue<Double> instantiate(SDTGuard g, Valuation val, Constants c, Collection<DataValue<Double>> alreadyUsedValues) {
//                System.out.println("INSTANTIATING: " + g.toString() + " without " + alreadyUsedValues);
//                System.out.println("val: " + val + "; consts" + c);
                SymbolicDataValue.SuffixValue sp = g.getParameter();
                Valuation newVal = new Valuation();
                newVal.putAll(val);
                GuardExpression x = g.toExpr();
                ConstraintSolver.Result res;
                if (g instanceof EqualityGuard) {
                    //System.out.println("SOLVING: " + x);                    
                    res = solver.solve(x.toDataExpression().getExpression(), newVal);
                } else {
                    List<Expression<Boolean>> eList = new ArrayList<Expression<Boolean>>();
                    // add the guard
                    eList.add(g.toExpr().toDataExpression().getExpression());
                    eList.addAll(instantiateGuard(g, val));
                    if (g instanceof SDTOrGuard) {
                        // for all registers, pick them up
                        for (SDTGuard subg : ((SDTOrGuard) g).getGuards()) {
                            if (!(subg instanceof EqualityGuard)) {
                                eList.addAll(instantiateGuard(subg, val));
                            }
                        }
                    }

                    // add disequalities
                    for (DataValue<Double> au : alreadyUsedValues) {
                        gov.nasa.jpf.constraints.expressions.Constant w = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, au.getId());
                        Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, sp.toVariable());
                        eList.add(auExpr);
                    }

                    if (newVal.containsValueFor(sp.toVariable())) {
                        DataValue<Double> spDouble = new DataValue(doubleType, newVal.getValue(sp.toVariable()));
                        gov.nasa.jpf.constraints.expressions.Constant spw = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, spDouble.getId());
                        Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, sp.toVariable());
                        eList.add(spExpr);
                    }

                    Expression<Boolean> _x = ExpressionUtil.and(eList);
//                    System.out.println("SOLVING: " + _x + " with " + newVal);
                    res = solver.solve(_x, newVal);
//                    System.out.println("SOLVING:: " + res + "  " + eList + "  " + newVal);
                }
//                System.out.println("VAL: " + newVal);
//                System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and vals " + newVal.toString() + " and param-variable " + sp.toVariable().toString());
//                System.out.println("x is " + x.toString());
                if (res == ConstraintSolver.Result.SAT) {
//                    System.out.println("SAT!!");
//                    System.out.println(newVal.getValue(sp.toVariable()) + "   " + newVal.getValue(sp.toVariable()).getClass());
                    DataValue<Double> d = new DataValue(doubleType, (newVal.getValue(sp.toVariable())));
//                    System.out.println("return d: " + d.toString());
                    return d;//new DataValue<Double>(doubleType, d);
                } else {
//                    System.out.println("UNSAT: " + _x + " with " + newVal);
                    return null;
                }
            }

            @Override
            public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
                //assume we can just sort the list and get the values
                List<DataValue<Double>> sortedList = new ArrayList<DataValue<Double>>();
                for (DataValue<Double> d : dvs) {
//                    if (d.getId() instanceof Integer) {
//                        sortedList.add(new DataValue(d.getType(), ((Integer) d.getId()).doubleValue()));
//                    } else if (d.getId() instanceof Double) {
                    sortedList.add(d);
//                    } else {
//                        throw new IllegalStateException("not supposed to happen");
//                    }
                }

                //sortedList.addAll(dvs);
                Collections.sort(sortedList, new Cpr());

                //System.out.println("I'm sorted!  " + sortedList.toString());
                return sortedList;
            }

        }
        );

        DataWordSUL sul = new PriorityQueueSUL(teachers, consts, inputs, outputs);

        //SimulatorOracle oracle = new SimulatorOracle(model);
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputArray);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts);

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts);
            }
        };
//        final Word<PSymbolInstance> prefix = Word.fromSymbols(
//                new PSymbolInstance(I_REGISTER, 
//                    new DataValue(T_UID, 1),
//                    new DataValue(T_PWD, 1)),
//                new PSymbolInstance(I_LOGIN, 
//                    new DataValue(T_UID, 2),
//                    new DataValue(T_PWD, 2)));           
//        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(POLL),
                new PSymbolInstance(OUTPUT,
                        new DataValue(doubleType, 4.0)),
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, 5.0)),
                new PSymbolInstance(OK),
                new PSymbolInstance(POLL),
                new PSymbolInstance(OUTPUT,
                        new DataValue(doubleType, 6.0)));
        
        

        

//        Prefix: offer[2.0[DOUBLE]] ok[] offer[1.0[DOUBLE]] ok[]
//SymSuffix: [s2]((offer[s1] ok[] poll[] out[s2]))
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, 1.0)),
                new PSymbolInstance(OK),
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, 2.0)),
                new PSymbolInstance(OK),
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, 3.0)),
                new PSymbolInstance(OK));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + symSuffix);

        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
//        System.out.println(res.getSdt().isAccepting());
        System.out.println("final SDT: \n" + sdt.toString());

        Parameter p1 = new Parameter(doubleType, 1);
        Parameter p2 = new Parameter(doubleType, 2);
        Parameter p3 = new Parameter(doubleType, 3);
        DataValue d1 = new DataValue(doubleType, 1.0);
        DataValue d2 = new DataValue(doubleType, 2.0);
        DataValue d3 = new DataValue(doubleType, 3.0);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(doubleType, 1));
        testPiv.put(p2, new Register(doubleType, 2));
        testPiv.put(p3, new Register(doubleType, 3));

        ParValuation testPval = new ParValuation();
        testPval.put(p1, d1);
        testPval.put(p2, d2);
        testPval.put(p3, d3);
        
        

        
        Branching branching = mto.getInitialBranching(prefix, OFFER, testPiv, testPval, sdt);
        System.out.println("Branching: \n" + branching.toString());
        System.out.println("Get initial branching: \n" + branching.getBranches().toString());
    }
//    

}
