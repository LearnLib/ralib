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

import java.math.BigDecimal;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3SolverProvider;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(DoubleInequalityTheory.class);

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
            DataValue<BigDecimal> sdi = new DoubleDataValue(type, (BigDecimal) val.getValue(toVariable(si)));
            // add the register value as a constant
            gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (sdi.getId()));
            // add the constant equivalence expression to the list
            eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(si)));

        } else if (g instanceof IntervalGuard) {
            IntervalGuard iGuard = (IntervalGuard) g;
            if (!iGuard.isBiggerGuard()) {
                SymbolicDataValue r = iGuard.getRightReg();
                DataValue<BigDecimal> ri = new DoubleDataValue(type, (BigDecimal) val.getValue(toVariable(r)));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (ri.getId()));
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(r)));

            }
            if (!iGuard.isSmallerGuard()) {
                SymbolicDataValue l = iGuard.getLeftReg();
                DataValue<BigDecimal> li = new DoubleDataValue(type, (BigDecimal) val.getValue(toVariable(l)));
                gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(BuiltinTypes.DECIMAL, (li.getId()));
                // add the constant equivalence expression to the list
                eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(l)));

            }
        }
        return eList;
    }

    @Override
    protected List<Expression<Boolean>> symbolicDataValueExpression(Set<SymbolicDataValue> expressed, SDTGuard g, Valuation val) {
    	List<Expression<Boolean>> eList = new ArrayList<>();
    	if (g instanceof SDTIfGuard) {
    		SymbolicDataValue si = ((SDTIfGuard) g).getRegister();
    		if (!expressed.contains(si)) {
    			DataValue<BigDecimal> sdi = new DoubleDataValue(si.getType(), (BigDecimal) val.getValue(toVariable(si)));
    			gov.nasa.jpf.constraints.expressions.Constant<BigDecimal> wm = new gov.nasa.jpf.constraints.expressions.Constant<>(BuiltinTypes.DECIMAL, (sdi.getId()));
    			eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(si)));
    			expressed.add(si);
    		}
    	} else if (g instanceof IntervalGuard) {
    		IntervalGuard iGuard = (IntervalGuard) g;
    		if (!iGuard.isBiggerGuard()) {
    			SymbolicDataValue r = iGuard.getRightReg();
    			if (!expressed.contains(r)) {
    				DataValue<BigDecimal> ri = new DoubleDataValue(type, (BigDecimal) val.getValue(toVariable(r)));
    				gov.nasa.jpf.constraints.expressions.Constant<BigDecimal> wm = new gov.nasa.jpf.constraints.expressions.Constant<>(BuiltinTypes.DECIMAL, (ri.getId()));
    				eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(r)));
    				expressed.add(r);
    			}
    		}
    		if (!iGuard.isSmallerGuard()) {
    			SymbolicDataValue l = iGuard.getLeftReg();
    			if (!expressed.contains(l)) {
    				DataValue<BigDecimal> li = new DoubleDataValue(type, (BigDecimal) val.getValue(toVariable(l)));
    				gov.nasa.jpf.constraints.expressions.Constant<BigDecimal> wm = new gov.nasa.jpf.constraints.expressions.Constant<>(BuiltinTypes.DECIMAL, (li.getId()));
    				eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(l)));
    				expressed.add(l);
    			}
    		}
    	} else if (g instanceof SDTMultiGuard) {
    		SDTMultiGuard mGuard = (SDTMultiGuard) g;
    		for (SDTGuard mg : mGuard.getGuards()) {
    			eList.addAll(symbolicDataValueExpression(expressed, mg, val));
    		}
    	}
    	return eList;
    }

//    @Override
//    public DoubleDataValue representativeDataValue(Mapping<SymbolicDataValue, DataValue<?>> valuation, SDTGuard guard, DataType type) {
//    	SymbolicDataValue.SuffixValue sv = guard.getParameter();
//    	Valuation newVal = new Valuation();
//    	GuardExpression x = guard.toExpr();
//    	Result res;
//    	if (guard instanceof EqualityGuard) {
//    		res = solver.solve(toExpression(x), newVal);
//    	} else {
//    		List<Expression<Boolean>> eList = new ArrayList<>();
//    		eList.add(toExpression(guard.toExpr()));
//    	}
//    }

    @Override
    public DoubleDataValue instantiate(SDTGuard g, Valuation val, Constants c, Collection<DataValue<BigDecimal>> alreadyUsedValues) {
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
                DoubleDataValue spDouble = new DoubleDataValue(type, (BigDecimal)newVal.getValue(toVariable(sp)));
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
            DoubleDataValue d = new DoubleDataValue(type, (BigDecimal)(newVal.getValue(toVariable(sp))));
            //System.out.println("return d: " + d.toString());
            return d;//new DataValue<Double>(doubleType, d);
        } else {
//                    System.out.println("UNSAT: " + _x + " with " + newVal);
            return null;
        }
    }

    @Override
    protected DoubleDataValue instantiate(SuffixValue sp, List<Expression<Boolean>> expList, Valuation val, Collection<DataValue<BigDecimal>> alreadyUsedValues) {
    	List<Expression<Boolean>> eList = new ArrayList<>();
    	eList.addAll(expList);
    	Valuation newVal = new Valuation();
    	newVal.putAll(val);
    	Variable<BigDecimal> spVar = toVariable(sp);

    	for (DataValue<BigDecimal> au : alreadyUsedValues) {
    		gov.nasa.jpf.constraints.expressions.Constant<BigDecimal> w = new gov.nasa.jpf.constraints.expressions.Constant<>(BuiltinTypes.DECIMAL, (au.getId()));
    		Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, spVar);
    		eList.add(auExpr);
    	}

    	if (newVal.containsValueFor(toVariable(sp))) {
    		DoubleDataValue spDouble = new DoubleDataValue(sp.getType(), (BigDecimal) newVal.getValue(spVar));
    		gov.nasa.jpf.constraints.expressions.Constant<BigDecimal> spw = new gov.nasa.jpf.constraints.expressions.Constant<>(BuiltinTypes.DECIMAL, (spDouble.getId()));
    		Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, spVar);
    	}

    	Expression<Boolean> expr = ExpressionUtil.and(eList);
    	Result res = solver.solve(expr, newVal);

    	if (res == Result.SAT) {
    		DoubleDataValue d = new DoubleDataValue(sp.getType(), (BigDecimal)(newVal.getValue(spVar)));
    		return d;
    	}
    	return null;
    }

    @Override
    public DoubleDataValue getFreshValue(List<DataValue<BigDecimal>> vals) {
        if (vals.isEmpty()) {
            return new DoubleDataValue(type, BigDecimal.ONE);
        }
        List<DataValue<BigDecimal>> potential = getPotential(vals);
        if (potential.isEmpty()) {
            return new DoubleDataValue(type, BigDecimal.ONE);
        }
        //LOGGER.trace("smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
        DataValue<BigDecimal> biggestDv = Collections.max(potential, new Cpr());
        return new DoubleDataValue(type, biggestDv.getId().add(BigDecimal.ONE));
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
    public Collection<DataValue<BigDecimal>> getAllNextValues(
            List<DataValue<BigDecimal>> vals) {
        Set<DataValue<BigDecimal>> nextValues = new LinkedHashSet<>();
        nextValues.addAll(vals);
        if (vals.isEmpty()) {
            nextValues.add(new DoubleDataValue(type, BigDecimal.ONE));
        } else {
            Collections.sort(vals, new Cpr());
            if (vals.size() > 1) {
                for (int i = 0; i < (vals.size() - 1); i++) {
                    BigDecimal d1 = vals.get(i).getId();
                    BigDecimal d2 = vals.get(i + 1).getId();
                    nextValues.add(new DoubleDataValue(type,
                            d2.subtract(d1).divide(BigDecimal.valueOf(2.0)).add(d1)));
                            //(d1 + ((d2 - d1) / 2))));
                }
            }
            nextValues.add(new DoubleDataValue(type, (Collections.min(vals, new Cpr()).getId().subtract(BigDecimal.ONE))));
            nextValues.add(new DoubleDataValue(type, (Collections.max(vals, new Cpr()).getId().add(BigDecimal.ONE))));
        }
        return nextValues;
    }

	@Override
	protected Comparator<DataValue<BigDecimal>> getComparator() {
		return new Comparator<DataValue<BigDecimal>>() {
			@Override
			public int compare(DataValue<BigDecimal> d1, DataValue<BigDecimal> d2) {
				return d1.getId().compareTo(d2.getId());
			}
		};
	}

	@Override
	protected DataValue<BigDecimal> safeCast(DataValue<?> dv) {
		if (dv.getId() instanceof BigDecimal) {
			return new DataValue<BigDecimal>(dv.getType(), (BigDecimal) dv.getId());
		}
		return null;
	}

//	@Override
//	public SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix,
//			Word<PSymbolInstance> suffix, Constants consts) {
//		return new UnrestrictedSuffixValue(suffixValue);
//	}
//
//	@Override
//	public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
//		return new UnrestrictedSuffixValue(guard.getParameter());
//	}

	@Override
	public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
		// not yet implemented for inequality theory
		return false;
	}
}
