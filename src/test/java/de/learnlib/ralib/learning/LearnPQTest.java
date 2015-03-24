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
//import de.learnlib.ralib.automata.javaclasses.PriorityQueueOracle;
import static de.learnlib.ralib.automata.javaclasses.PriorityQueueOracle.*;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
public class LearnPQTest {

    public LearnPQTest() {
    }

    @Test
    public void PQExample() {

        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINEST);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINEST);
//            h.setFilter(new CategoryFilter(EnumSet.of(
//                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
//                    Category.EVENT, Category.PHASE, Category.MODEL)));
        }

        Constants consts = new Constants();

        DataWordOracle dwOracle = new de.learnlib.ralib.automata.javaclasses.PriorityQueueOracle();
//        System.out.println("SYS:------------------------------------------------");
//        System.out.println(sul);
//        System.out.println("----------------------------------------------------");

        final Map<DataType, Theory> teachers = new HashMap<DataType, Theory>();

        teachers.put(doubleType, new InequalityTheoryWithEq<Double>() {
            Valuation val = new Valuation();
            private final ConstraintSolverFactory fact = new ConstraintSolverFactory();
            private final ConstraintSolver solver = fact.createSolver("z3");

            @Override
            public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
                return new DataValue(doubleType, vals.size() + 1.0);
            }

            @Override
            public DataValue<Double> instantiate(SDTGuard g, Valuation val, Parameter p, Constants c) {
                System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and vals " + val.toString());
                solver.solve(g.toExpr(c), val);
                Double d = (Double) val.getValue(p.toVariable());
                return new DataValue<Double>(doubleType, d);
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
                Collections.sort(sortedList, new Comparator<DataValue<Double>>() {
                    public int compare(DataValue<Double> one, DataValue<Double> other) {
                        return one.getId().compareTo(other.getId());
                    }
                });

                //System.out.println("I'm sorted!  " + sortedList.toString());
                return sortedList;
            }

        }
        );

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
                        new DataValue(doubleType, 2.0)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, 1.0)),
                new PSymbolInstance(POLL,
                        new DataValue(doubleType, 2.0)));

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
