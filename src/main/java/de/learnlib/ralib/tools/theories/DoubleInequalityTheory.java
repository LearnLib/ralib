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

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toExpression;
import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

/**
 *
 * @author falk
 */
public class DoubleInequalityTheory extends InequalityTheoryWithEq<Double> implements TypedTheory<Double> {

    protected static final class Cpr implements Comparator<DataValue<Double>> {

        @Override
        public int compare(DataValue<Double> one, DataValue<Double> other) {
            return one.getId().compareTo(other.getId());
        }
    }

    private final ConstraintSolverFactory fact = new ConstraintSolverFactory();
    private final ConstraintSolver solver = fact.createSolver("z3");

    private DataType<Double> type = null;

    public DoubleInequalityTheory() {
    }

    public DoubleInequalityTheory(DataType t) {
        this.setType(t);
    }

    @Override
    public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<Double>> sortedList = new ArrayList<>();
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

    private List<Expression<Boolean>> instantiateGuard(SDTGuard g, Valuation val) {
        List<Expression<Boolean>> eList = new ArrayList<Expression<Boolean>>();
        if (g instanceof SDTIfGuard) {
            // pick up the register
            SymbolicDataValue si = ((SDTIfGuard) g).getRegister();
            // get the register value from the valuation
            DataValue<Double> sdi = new DataValue(getType(), val.getValue(toVariable(si)));
            // add the register value as a constant
            gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, sdi.getId());
            // add the constant equivalence expression to the list
            eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(si)));

        } else if (g instanceof IntervalGuard) {
            IntervalGuard iGuard = (IntervalGuard) g;
            if (!iGuard.isBiggerGuard()) {
                SymbolicDataValue r = (SymbolicDataValue) iGuard.getRightSDV();
                DataValue<Double> ri = new DataValue(getType(), val.getValue(toVariable(r)));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, ri.getId());
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(r)));

            }
            if (!iGuard.isSmallerGuard()) {
                SymbolicDataValue l = (SymbolicDataValue) iGuard.getLeftSDV();
                DataValue<Double> li = new DataValue(getType(), val.getValue(toVariable(l)));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, li.getId());
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(l)));

            }
        }
        return eList;
    }
    

    public ValueMapper<Double> getValueMapper() {
    	return new SumCDoubleInequalityValueMapper(this);
    }

    @Override
    public DataValue<Double> instantiate(SDTGuard g, Valuation val, Constants c, Collection<DataValue<Double>> alreadyUsedValues) {
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
            for (DataValue<Double> au : alreadyUsedValues) {
                gov.nasa.jpf.constraints.expressions.Constant w = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, au.getId());
                Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, toVariable(sp));
                eList.add(auExpr);
            }

            if (newVal.containsValueFor(toVariable(sp))) {
                DataValue<Double> spDouble = new DataValue(getType(), newVal.getValue(toVariable(sp)));
                gov.nasa.jpf.constraints.expressions.Constant spw = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, spDouble.getId());
                Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, toVariable(sp));
                eList.add(spExpr);
            }
            
            for (Variable var : newVal.getVariables()) {
            	DataValue<Double> spDouble = new DataValue(getType(), newVal.getValue(var));
            	gov.nasa.jpf.constraints.expressions.Constant spw = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DOUBLE, spDouble.getId());
            	Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, var);
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
            DataValue<Double> d = new DataValue(getType(), (newVal.getValue(toVariable(sp))));
            //System.out.println("return d: " + d.toString());
            return d;//new DataValue<Double>(doubleType, d);
        } else {
//                    System.out.println("UNSAT: " + _x + " with " + newVal);
            return null;
        }
    }

    @Override
    public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
        if (vals.isEmpty()) {
            return new DataValue<Double>(getType(), 1.0);
        }
        List<DataValue<Double>> potential = getPotential(vals);
        if (potential.isEmpty()) {
            return new DataValue(getType(), 1.0);
        }
        //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
        DataValue<Double> biggestDv = Collections.max(potential, new Cpr());
        return new DataValue(getType(), biggestDv.getId() + 1.0);
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
    public Collection<DataValue<Double>> getAllNextValues(
            List<DataValue<Double>> vals) {
        Set<DataValue<Double>> nextValues = new LinkedHashSet<>();
        nextValues.addAll(vals);
        List<DataValue<Double>> distinctValList = new ArrayList<>(nextValues);
        
        if (distinctValList .isEmpty()) {
            nextValues.add(new FreshValue<Double>(getType(), 1.0));
        } else {
            Collections.sort(distinctValList , new Cpr());
            if (distinctValList.size() > 1) {
                for (int i = 0; i < (distinctValList.size() - 1); i++) {
                    IntervalDataValue<Double> intVal = IntervalDataValue.instantiateNew(distinctValList.get(i), distinctValList .get(i + 1));
                    nextValues.add(intVal);
                }
            }
            DataValue<Double> min = Collections.min(distinctValList, new Cpr());
            nextValues.add(IntervalDataValue.instantiateNew(null, min));
            DataValue<Double> max = Collections.max(distinctValList, new Cpr());
            nextValues.add(IntervalDataValue.instantiateNew(max, null));
        }
        return nextValues;
    }

	public DataType<Double> getType() {
		return type;
	}
    
}
