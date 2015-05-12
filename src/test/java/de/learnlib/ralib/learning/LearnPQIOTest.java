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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import static de.learnlib.ralib.automata.javaclasses.PriorityQueueOracle.doubleType;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.example.priority.PriorityQueueSUL.Actions;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
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
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class LearnPQIOTest {

    public static final DataType doubleType = new DataType("DOUBLE", Double.class);

    public LearnPQIOTest() {
    }
    
    

    @Test
    public void learnLoginExampleIO() {

        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.INFO);
//            h.setFilter(new CategoryFilter(EnumSet.of(
//                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
//                    Category.EVENT, Category.PHASE, Category.MODEL)));
        }
        
        

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
        
        //RegisterAutomatonLoader loader = new RegisterAutomatonLoader(
        //        RegisterAutomatonLoaderTest.class.getResourceAsStream(
        //                "/de/learnlib/ralib/automata/xml/abp.output.xml"));
        //RegisterAutomaton model = loader.getRegisterAutomaton();
        //System.out.println("SYS:------------------------------------------------");
        //System.out.println(model);
        //System.out.println("----------------------------------------------------");
        Map<PriorityQueueSUL.Actions,ParameterizedSymbol> inputs = new LinkedHashMap<PriorityQueueSUL.Actions,ParameterizedSymbol>();
        inputs.put(Actions.POLL,POLL);
        inputs.put(Actions.OFFER,OFFER);
        
        Map<PriorityQueueSUL.Actions,ParameterizedSymbol> outputs = new LinkedHashMap<PriorityQueueSUL.Actions,ParameterizedSymbol>();
        outputs.put(Actions.ERROR,ERROR);
        //outputs.put(Actions.VOID,VOID);
        outputs.put(Actions.OUTPUT,OUTPUT);
        outputs.put(Actions.OK,OK);
        outputs.put(Actions.NOK,NOK);
        
        final Constants consts = new Constants();

        long seed = -1386796323025681754L;
        //long seed = (new Random()).nextLong();
        System.out.println("SEED=" + seed);
        final Random random = new Random(seed);

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        class Cpr implements Comparator<DataValue<Double>> {

            public int compare(DataValue<Double> one, DataValue<Double> other) {
                return one.getId().compareTo(other.getId());
            }
        }
            teachers.put(doubleType, new InequalityTheoryWithEq<Double>() {
            Valuation val = new Valuation();
            private final ConstraintSolverFactory fact = new ConstraintSolverFactory();
            private final ConstraintSolver solver = fact.createSolver("z3");

            @Override
            public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
                if (vals.isEmpty()) {
                    return new DataValue(doubleType, 1.0);
                }
                List<DataValue<Double>> potential = getPotential(vals);
                //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
                DataValue<Double> biggestDv = Collections.max(potential, new Cpr());
                return new DataValue(doubleType, biggestDv.getId() + 1.0);
            }

            @Override
            public DataValue<Double> instantiate(SDTGuard g, Valuation val, Constants c, Collection<DataValue<Double>> alreadyUsedValues) {
                //System.out.println("INSTANTIATING: " + g.toString());
                SymbolicDataValue.SuffixValue sp = g.getParameter();
                Valuation newVal = new Valuation();
                newVal.putAll(val);
                GuardExpression x = g.toExpr();
                if (g instanceof EqualityGuard) {                    
                    //System.out.println("SOLVING: " + x);                    
                    solver.solve(x.toDataExpression().getExpression(), newVal);
                } else {
                    List<Expression<Boolean>> eList = new ArrayList<Expression<Boolean>>();
                    // add the guard
                    eList.add(g.toExpr().toDataExpression().getExpression());
                    if (g instanceof SDTMultiGuard) {
                        // for all registers, pick them up
                        for (SymbolicDataValue s : ((SDTMultiGuard) g).getAllRegs()) {
                            // get register value from valuation
                            DataValue<Double> sdv = (DataValue<Double>) val.getValue(s.toVariable());
                            // add register value as a constant
                            gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, sdv.getId());
                            // add constant equivalence expression to the list
                            Expression<Boolean> multiExpr = new NumericBooleanExpression(wm, NumericComparator.EQ, s.toVariable());
                            eList.add(multiExpr);
                        }

                    } else if (g instanceof SDTIfGuard) {
                        // pick up the register
                        SymbolicDataValue si = ((SDTIfGuard) g).getRegister();
                        // get the register value from the valuation
                        DataValue<Double> sdi = (DataValue<Double>) val.getValue(si.toVariable());
                        // add the register value as a constant
                        gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, sdi.getId());
                        // add the constant equivalence expression to the list
                        Expression<Boolean> ifExpr = new NumericBooleanExpression(wm, NumericComparator.EQ, si.toVariable());
                        eList.add(ifExpr);
                    }
                    // add disequalities
                    for (DataValue<Double> au : alreadyUsedValues) {
                        gov.nasa.jpf.constraints.expressions.Constant w = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, au.getId());
                        Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, sp.toVariable());
                        eList.add(auExpr);
                    }
                    Expression<Boolean> _x = ExpressionUtil.and(eList);
                    //System.out.println("SOLVING: " + _x);
                    solver.solve(_x,newVal);
                }
                 //System.out.println("VAL: " + newVal);
//                System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and vals " + newVal.toString() + " and param-variable " + sp.toVariable().toString());
//                System.out.println("x is " + x.toString());
                Double d = (Double) newVal.getValue(sp.toVariable());
                //System.out.println("return d: " + d.toString());
                return new DataValue<Double>(doubleType, d);
            }

            private Expression<Boolean> toExpr(List<Expression<Boolean>> eqList, int i) {
                //assert !eqList.isEmpty();
                if (eqList.size() == i + 1) {
                    return eqList.get(i);
                } else {
//            System.out.println("here is the xpr: " + eqList.toString());
                    return new PropositionalCompound(eqList.get(i), LogicalOperator.AND, toExpr(eqList, i + 1));
                }
            }

            @Override
            public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
                //assume we can just sort the list and get the values
                List<DataValue<Double>> sortedList = new ArrayList<DataValue<Double>>();
                for (DataValue d : dvs) {
                    if (d.getId() instanceof Integer) {
                        sortedList.add(new DataValue(d.getType(), ((Integer) d.getId()).doubleValue()));
                    } else if (d.getId() instanceof Double) {
                        sortedList.add(d);
                    } else {
                        throw new IllegalStateException("not supposed to happen");
                    }
                }

                //sortedList.addAll(dvs);
                Collections.sort(sortedList, new Cpr());

                //System.out.println("I'm sorted!  " + sortedList.toString());
                return sortedList;
            }

        }
        );
        

        DataWordSUL sul = new PriorityQueueSUL(teachers, consts, inputs, outputs, 3);

        //SimulatorOracle oracle = new SimulatorOracle(model);
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache,inputArray);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts);

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts);
            }
        };

        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, actionArray);

        IORandomWalk iowalk = new IORandomWalk(random,
                sul,
                false, // do not draw symbols uniformly 
                0.1, // reset probability 
                0.8, // prob. of choosing a fresh data value
                10000, // 1000 runs 
                100, // max depth
                consts,
                false, // reset runs 
                teachers,
                inputArray);

        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);

        int check = 0;
        while (true && check < 100) {

            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();
            System.out.println("HYP:------------------------------------------------");
            System.out.println(hyp);
            System.out.println("----------------------------------------------------");

            DefaultQuery<PSymbolInstance, Boolean> ce
                    = iowalk.findCounterExample(hyp, null);

            System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }

            ce = loops.optimizeCE(ce.getInput(), hyp);
            System.out.println("Shorter CE: " + ce);
            ce = asrep.optimizeCE(ce.getInput(), hyp);
            System.out.println("New Prefix CE: " + ce);
            ce = pref.optimizeCE(ce.getInput(), hyp);
            System.out.println("Prefix of CE is CE: " + ce);

//            assert model.accepts(ce.getInput());
//            assert !hyp.accepts(ce.getInput());

            rastar.addCounterexample(ce);

        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        System.out.println("LAST:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

        System.out.println("Seed:" + seed);
        System.out.println("IO-Oracle MQ: " + ioOracle.getQueryCount());
        System.out.println("SUL resets: " + sul.getResets());
        System.out.println("SUL inputs: " + sul.getInputs());
        System.out.println("Rounds: " + check);

    }
}
