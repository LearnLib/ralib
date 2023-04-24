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
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3SolverProvider;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

import java.math.BigDecimal;
import java.util.*;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;
import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toExpression;

/**
 *
 * @author falk
 */
public class DoubleInequalityTheory extends InequalityTheoryWithEq<BigDecimal> implements TypedTheory<BigDecimal> {

    private static final class Cpr implements Comparator<DataValue<BigDecimal>> {

        @Override
        public int compare(DataValue<BigDecimal> one, DataValue<BigDecimal> other) {
            return one.getId().compareTo(other.getId());
        }
    }

    private final ConstraintSolver solver = (new NativeZ3SolverProvider()).createSolver(new Properties());

    private DataType type = null;

    public DoubleInequalityTheory() {
    }

    public DoubleInequalityTheory(DataType t) {
        this.type = t;
    }

    @Override
    public List<DataValue<BigDecimal>> getPotential(List<DataValue<BigDecimal>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<BigDecimal>> sortedList = new ArrayList<>();
        for (DataValue<BigDecimal> d : dvs) {
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

    private List<Expression<Boolean>> instantiateGuard(SDTGuard g, Valuation val) {
        List<Expression<Boolean>> eList = new ArrayList<Expression<Boolean>>();
        if (g instanceof SDTIfGuard) {
            // pick up the register
            SymbolicDataValue si = ((SDTIfGuard) g).getRegister();
            // get the register value from the valuation
            DataValue<BigDecimal> sdi = new DataValue(type, (BigDecimal) val.getValue(toVariable(si)));
            // add the register value as a constant
            gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (sdi.getId()));
            // add the constant equivalence expression to the list
            eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(si)));

        } else if (g instanceof IntervalGuard) {
            IntervalGuard iGuard = (IntervalGuard) g;
            if (!iGuard.isBiggerGuard()) {
                SymbolicDataValue r = iGuard.getRightReg();
                DataValue<BigDecimal> ri = new DataValue(type, (BigDecimal) val.getValue(toVariable(r)));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (ri.getId()));
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(r)));

            }
            if (!iGuard.isSmallerGuard()) {
                SymbolicDataValue l = iGuard.getLeftReg();
                DataValue<BigDecimal> li = new DataValue(type, (BigDecimal) val.getValue(toVariable(l)));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (li.getId()));
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(l)));

            }
        }
        return eList;
    }

    @Override
    public DataValue<BigDecimal> instantiate(SDTGuard g, Valuation val, Constants c, Collection<DataValue<BigDecimal>> alreadyUsedValues) {
        //System.out.println("INSTANTIATING: " + g.toString());
        SymbolicDataValue.SuffixValue sp = g.getParameter();
        Valuation newVal = new Valuation();
        newVal.putAll(val);
        GuardExpression x = g.toExpr();
        Result res;
        if (g instanceof EqualityGuard) {
            //System.out.println("SOLVING: " + x);
            res = solver.solve(toExpression(x), newVal);
        } else {
            List<Expression<Boolean>> eList = new ArrayList<>();
            // add the guard
            eList.add(toExpression(g.toExpr()));
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
            for (DataValue<BigDecimal> au : alreadyUsedValues) {
                gov.nasa.jpf.constraints.expressions.Constant w = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (au.getId()));
                Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, toVariable(sp));
                eList.add(auExpr);
            }

            if (newVal.containsValueFor(toVariable(sp))) {
                DataValue<BigDecimal> spDouble = new DataValue(type, newVal.getValue(toVariable(sp)));
                gov.nasa.jpf.constraints.expressions.Constant spw = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (spDouble.getId()));
                Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, toVariable(sp));
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
        if (res == Result.SAT) {
//                    System.out.println("SAT!!");
//                    System.out.println(newVal.getValue(sp.toVariable()) + "   " + newVal.getValue(sp.toVariable()).getClass());
            DataValue<BigDecimal> d = new DataValue(type, (newVal.getValue(toVariable(sp))));
            //System.out.println("return d: " + d.toString());
            return d;//new DataValue<Double>(doubleType, d);
        } else {
//                    System.out.println("UNSAT: " + _x + " with " + newVal);
            return null;
        }
    }

    @Override
    public DataValue<BigDecimal> getFreshValue(List<DataValue<BigDecimal>> vals) {
        if (vals.isEmpty()) {
            return new DataValue(type, BigDecimal.ONE);
        }
        List<DataValue<BigDecimal>> potential = getPotential(vals);
        if (potential.isEmpty()) {
            return new DataValue(type, BigDecimal.ONE);
        }
        //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
        DataValue<BigDecimal> biggestDv = Collections.max(potential, new Cpr());
        return new DataValue(type, biggestDv.getId().add(BigDecimal.ONE));
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
        System.err.println("Fresh values are currently not supported for theory "
                + DoubleInequalityTheory.class.getName());
    }

    @Override
    public Collection<DataValue<BigDecimal>> getAllNextValues(
            List<DataValue<BigDecimal>> vals) {
        Set<DataValue<BigDecimal>> nextValues = new LinkedHashSet<>();
        nextValues.addAll(vals);
        if (vals.isEmpty()) {
            nextValues.add(new DataValue(type, BigDecimal.ONE));
        } else {
            Collections.sort(vals, new Cpr());
            if (vals.size() > 1) {
                for (int i = 0; i < (vals.size() - 1); i++) {
                    BigDecimal d1 = vals.get(i).getId();
                    BigDecimal d2 = vals.get(i + 1).getId();
                    nextValues.add(new DataValue(type,
                            d2.subtract(d1).divide(BigDecimal.valueOf(2.0)).add(d1)));
                            //(d1 + ((d2 - d1) / 2))));
                }
            }
            nextValues.add(new DataValue(type, (Collections.min(vals, new Cpr()).getId().subtract(BigDecimal.ONE))));
            nextValues.add(new DataValue(type, (Collections.max(vals, new Cpr()).getId().add(BigDecimal.ONE))));
        }
        return nextValues;
    }

}
