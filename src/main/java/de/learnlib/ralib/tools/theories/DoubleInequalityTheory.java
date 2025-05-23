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

import java.math.BigDecimal;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3Solver;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3SolverProvider;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

/**
 *
 * @author falk
 */
public class DoubleInequalityTheory extends InequalityTheoryWithEq implements TypedTheory {

    private static final class Cpr implements Comparator<DataValue> {

        @Override
        public int compare(DataValue one, DataValue other) {
            return one.getValue().compareTo(other.getValue());
        }
    }

    private final ConstraintSolver solver = (new NativeZ3SolverProvider()).createSolver(new Properties());

    private static final Logger LOGGER = LoggerFactory.getLogger(DoubleInequalityTheory.class);

    private DataType type = null;

    public DoubleInequalityTheory() {
    }

    public DoubleInequalityTheory(DataType t) {
        this.type = t;
    }

    public NativeZ3Solver getSolver() {
    	return (NativeZ3Solver) solver;
    }

    @Override
    public List<DataValue> getPotential(List<DataValue> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue> sortedList = new ArrayList<>();
        for (DataValue d : dvs) {
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
        if (g instanceof SDTGuard.EqualityGuard eualityGuard) {
            // pick up the register
            SymbolicDataValue si = (SymbolicDataValue) eualityGuard.register();
            // get the register value from the valuation
            DataValue sdi = new DataValue(type, val.getValue(si));
            // add the register value as a constant
            gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (sdi.getValue()));
            // add the constant equivalence expression to the list
            eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, si));

        } else if (g instanceof SDTGuard.DisequalityGuard disequalityGuard) {
            // pick up the register
            SymbolicDataValue si = (SymbolicDataValue) disequalityGuard.register();
            // get the register value from the valuation
            DataValue sdi = new DataValue(type, val.getValue(si));
            // add the register value as a constant
            gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (sdi.getValue()));
            // add the constant equivalence expression to the list
            eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, si));
            throw new RuntimeException("this seems to be wrong ...");

        } else if (g instanceof SDTGuard.IntervalGuard iGuard) {
            if (!iGuard.isBiggerGuard()) {
                SDTGuardElement r =  iGuard.greaterElement();
                assert r != null;
                DataValue ri = (r instanceof DataValue) ? (DataValue) r :
                        new DataValue(type, (BigDecimal) val.getValue( (Variable) r));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (ri.getValue()));
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, r.asExpression()));
            }
            if (!iGuard.isSmallerGuard()) {
                SDTGuardElement l = iGuard.smallerElement();
                assert l != null;
                DataValue li = (l instanceof DataValue) ? (DataValue) l :
                        new DataValue(type, (BigDecimal) val.getValue( (Variable) l));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (li.getValue()));
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, l.asExpression()));
            }
        }
        return eList;
    }

    @Override
    public DataValue instantiate(SDTGuard g, Valuation val, Constants c, Collection<DataValue> alreadyUsedValues) {
        //System.out.println("INSTANTIATING: " + g.toString());
        SymbolicDataValue.SuffixValue sp = g.getParameter();
        Valuation newVal = new Valuation();
        newVal.putAll(val);
        Expression<Boolean> x = SDTGuard.toExpr(g);
        Result res;
        if (g instanceof SDTGuard.EqualityGuard) {
            //System.out.println("SOLVING: " + x);
            res = solver.solve(x, newVal);
        } else {
            List<Expression<Boolean>> eList = new ArrayList<>();
            // add the guard
            eList.add(SDTGuard.toExpr(g));
            eList.addAll(instantiateGuard(g, val));
            if (g instanceof SDTGuard.SDTOrGuard og) {
                // for all registers, pick them up
                for (SDTGuard subg : og.disjuncts()) {
                    if (!(subg instanceof SDTGuard.EqualityGuard)) {
                        eList.addAll(instantiateGuard(subg, val));
                    }
                }
            }

            // add disequalities
            for (DataValue au : alreadyUsedValues) {
                gov.nasa.jpf.constraints.expressions.Constant w = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (au.getValue()));
                Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, sp);
                eList.add(auExpr);
            }

            if (newVal.containsValueFor(sp)) {
                DataValue spDouble = new DataValue(type, newVal.getValue(sp));
                gov.nasa.jpf.constraints.expressions.Constant spw = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (spDouble.getValue()));
                Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, sp);
                eList.add(spExpr);
            }

            Expression<Boolean> _x = ExpressionUtil.and(eList);
                    //System.out.println("SOLVING: " + _x + " with " + newVal);
            res = solver.solve(_x, newVal);
//                    System.out.println("SOLVING:: " + res + "  " + eList + "  " + newVal);
        }
//                System.out.println("VAL: " + newVal);
//                System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and vals " + newVal.toString() + " and param-variable " + sp.toVariable().toString());
//                System.out.println("x is " + x.toString());
        if (res == Result.SAT) {
//                    System.out.println("SAT!!");
//                    System.out.println(newVal.getValue(sp.toVariable()) + "   " + newVal.getValue(sp.toVariable()).getClass());
            DataValue d = new DataValue(type, newVal.getValue(sp));
            //System.out.println("return d: " + d.toString());
            return d;//new DataValue<Double>(doubleType, d);
        } else {
//                    System.out.println("UNSAT: " + _x + " with " + newVal);
            return null;
        }
    }

    @Override
    public DataValue getFreshValue(List<DataValue> vals) {
        if (vals.isEmpty()) {
            return new DataValue(type, BigDecimal.ONE);
        }
        List<DataValue> potential = getPotential(vals);
        if (potential.isEmpty()) {
            return new DataValue(type, BigDecimal.ONE);
        }
        //LOGGER.trace("smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
        DataValue biggestDv = Collections.max(potential, new Cpr());
        return new DataValue(type, biggestDv.getValue().add(BigDecimal.ONE));
    }

    @Override
    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setUseSuffixOpt(boolean useit) {
        LOGGER.info(Category.SYSTEM,
                    "Optimized suffixes are currently not supported for theory {}",
                    DoubleInequalityTheory.class.getName());
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit, IOOracle oracle) {
        LOGGER.info(Category.SYSTEM,
                    "Fresh values are currently not supported for theory {}",
                    DoubleInequalityTheory.class.getName());
    }

    @Override
    public Collection<DataValue> getAllNextValues(
            List<DataValue> vals) {
        Set<DataValue> nextValues = new LinkedHashSet<>();
        nextValues.addAll(vals);
        if (vals.isEmpty()) {
            nextValues.add(new DataValue(type, BigDecimal.ONE));
        } else {
            Collections.sort(vals, new Cpr());
            if (vals.size() > 1) {
                for (int i = 0; i < (vals.size() - 1); i++) {
                    BigDecimal d1 = vals.get(i).getValue();
                    BigDecimal d2 = vals.get(i + 1).getValue();
                    nextValues.add(new DataValue(type,
                            d2.subtract(d1).divide(BigDecimal.valueOf(2.0)).add(d1)));
                            //(d1 + ((d2 - d1) / 2))));
                }
            }
            nextValues.add(new DataValue(type, (Collections.min(vals, new Cpr()).getValue().subtract(BigDecimal.ONE))));
            nextValues.add(new DataValue(type, (Collections.max(vals, new Cpr()).getValue().add(BigDecimal.ONE))));
        }
        return nextValues;
    }

	@Override
	protected Comparator<DataValue> getComparator() {
		return new Comparator<DataValue>() {
			@Override
			public int compare(DataValue d1, DataValue d2) {
				return d1.getValue().compareTo(d2.getValue());
			}
		};
	}

	@Override
	protected DataValue safeCast(DataValue dv) {
		if (dv.getValue() instanceof BigDecimal) {
			return new DataValue(dv.getDataType(), (BigDecimal) dv.getValue());
		}
		return null;
	}
}
