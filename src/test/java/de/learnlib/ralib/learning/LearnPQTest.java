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

//import de.learnlib.ralib.automata.javaclasses.PriorityQueueOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import static de.learnlib.ralib.automata.javaclasses.PriorityQueueOracle.*;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import static de.learnlib.ralib.learning.LearnPQIOTest.doubleType;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
public class LearnPQTest {

    public LearnPQTest() {
    }

    @Test
    public void PQExample() {

        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINE);
//            h.setFilter(new CategoryFilter(EnumSet.of(
//                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
//                    Category.EVENT, Category.PHASE, Category.MODEL)));
        }

        Constants consts = new Constants();

        DataWordOracle dwOracle = new de.learnlib.ralib.automata.javaclasses.PriorityQueueOracle();
//        System.out.println("SYS:------------------------------------------------");
//        System.out.println(sul);
//        System.out.println("----------------------------------------------------");

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();

        class Cpr implements Comparator<DataValue<Double>> {

            public int compare(DataValue<Double> one, DataValue<Double> other) {
                return one.getId().compareTo(other.getId());
            }
        }

        teachers.put(doubleType, new DoubleInequalityTheory(doubleType));
/*                
                new InequalityTheoryWithEq<Double>() {
            Valuation val = new Valuation();
            private final ConstraintSolverFactory fact = new ConstraintSolverFactory();
            private final ConstraintSolver solver = fact.createSolver("z3");

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
                //System.out.println("INSTANTIATING: " + g.toString());
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
*/
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants());
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts);

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, new Constants());
            }
        };

        RaStar rastar = new RaStar(mto, hypFactory, slo,
                consts, OFFER, POLL);

        rastar.learn();
        RegisterAutomaton hyp = rastar.getHypothesis();
        System.out.println("HYP:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, 1.0)),
                new PSymbolInstance(OFFER,
                        new DataValue(doubleType, 1.0)),
                //                new PSymbolInstance(POLL,
                //                        new DataValue(doubleType, 1.0)),
                //                new PSymbolInstance(OFFER,
                //                        new DataValue(doubleType, 1.0)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, 1.0)));

        DefaultQuery<PSymbolInstance, Boolean> ceQuery = new DefaultQuery(ce);
        dwOracle.processQueries(Collections.singleton(ceQuery));

        rastar.addCounterexample(ceQuery);

        rastar.learn();
        hyp = rastar.getHypothesis();
        System.out.println("HYP:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

    }
}
