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
package de.learnlib.ralib.tools.theories;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
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
import java.util.List;

/**
 *
 * @author falk
 */
public class DoubleInequalityTheory extends InequalityTheoryWithEq<Double> implements TypedTheory<Double> {

    private static final class Cpr implements Comparator<DataValue<Double>> {

        @Override
        public int compare(DataValue<Double> one, DataValue<Double> other) {
            return one.getId().compareTo(other.getId());
        }
    }

    private final ConstraintSolverFactory fact = new ConstraintSolverFactory();

    private final ConstraintSolver solver = fact.createSolver("z3");

    private DataType type = null;

    @Override
    public List<DataValue<Double>> getPotential(List<DataValue<Double>> vals) {
        //assume we can just sort the list and get the values
        List<DataValue<Double>> sortedList = new ArrayList<DataValue<Double>>();
        for (DataValue d : vals) {
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

    @Override
    public DataValue instantiate(SDTGuard g, Valuation val, Constants constants, 
            Collection<DataValue<Double>> alreadyUsedValues) {
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
            solver.solve(_x, newVal);
        }
        //System.out.println("VAL: " + newVal);
//                System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and vals " + newVal.toString() + " and param-variable " + sp.toVariable().toString());
//                System.out.println("x is " + x.toString());
        Double d = (Double) newVal.getValue(sp.toVariable());
        //System.out.println("return d: " + d.toString());
        return new DataValue<Double>(type, d);
    }

    @Override
    public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
        if (vals.isEmpty()) {
            return new DataValue(type, 1.0);
        }
        List<DataValue<Double>> potential = getPotential(vals);
        //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
        DataValue<Double> biggestDv = Collections.max(potential, new Cpr());
        return new DataValue(type, biggestDv.getId() + 1.0);
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
    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setUseSuffixOpt(boolean useit) {
        System.err.println("Optimized suffixes are currently not supported for theory "
                + DoubleInequalityTheory.class.getName());
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit, IOOracle oracle) {
        System.err.println("Fresh vlaues are currently not supported for theory "
                + DoubleInequalityTheory.class.getName());
    }

}
