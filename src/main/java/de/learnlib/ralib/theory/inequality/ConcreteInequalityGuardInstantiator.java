package de.learnlib.ralib.theory.inequality;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toExpression;
import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class ConcreteInequalityGuardInstantiator<T extends Comparable<T>> implements InequalityGuardInstantiator<T> {

	private static final Map<Class<?>, Type<?>> typeMap = new LinkedHashMap<>();
	static {
		typeMap.put(Integer.class, BuiltinTypes.INTEGER);
		typeMap.put(Double.class, BuiltinTypes.DOUBLE);
		typeMap.put(Float.class, BuiltinTypes.FLOAT);
	}

	@SuppressWarnings("unchecked")
	public static <T> Type<T> getJCType(Class<T> cls) {
		if (!typeMap.containsKey(cls))
			throw new RuntimeException("No JConstraints type defined for " + cls);
		return (Type<T>) typeMap.get(cls);
	}

	private final DataType<T> type;
	private final Type<T> jcType;
	private final ConstraintSolver solver;

	public ConcreteInequalityGuardInstantiator(DataType<T> type, gov.nasa.jpf.constraints.types.Type<T> jcType, ConstraintSolver solver) {
		this.type = type;
		this.jcType = jcType;
		this.solver = solver;
	}

	public ConcreteInequalityGuardInstantiator(DataType<T> type, ConstraintSolver solver) {
		this(type, getJCType(type.getBase()), solver);
	}

	private DataType<T> getType() {
		return type;
	}

	private Type<T> getJCType() {
		return jcType;
	}

	/* (non-Javadoc)
	 * @see de.learnlib.ralib.tools.theories.InequalityGuardInstantiator#instantiateGuard(de.learnlib.ralib.theory.SDTGuard, gov.nasa.jpf.constraints.api.Valuation)
	 */
	private List<Expression<Boolean>> instantiateGuard(SDTGuard g, Valuation val) {
		List<Expression<Boolean>> eList = new ArrayList<Expression<Boolean>>();
		if (g instanceof SDTIfGuard) {
			// pick up the register
			SymbolicDataValue si = ((SDTIfGuard) g).getRegister();
			// get the register value from the valuation
			DataValue<Double> sdi = new DataValue(getType(), val.getValue(toVariable(si)));
			// add the register value as a constant
			gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(
					getJCType(), sdi.getId());
			// add the constant equivalence expression to the list
			eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(si)));

		} else if (g instanceof IntervalGuard) {
			IntervalGuard iGuard = (IntervalGuard) g;
			if (!iGuard.isBiggerGuard()) {
				SymbolicDataValue r = (SymbolicDataValue) iGuard.getRightSDV();
				DataValue<Double> ri = new DataValue(getType(), val.getValue(toVariable(r)));
				gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(
						getJCType(), ri.getId());
				// add the constant equivalence expression to the list
				eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(r)));

			}
			if (!iGuard.isSmallerGuard()) {
				SymbolicDataValue l = (SymbolicDataValue) iGuard.getLeftSDV();
				DataValue<Double> li = new DataValue(getType(), val.getValue(toVariable(l)));
				gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(
						getJCType(), li.getId());
				// add the constant equivalence expression to the list
				eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(l)));

			}
		}
		return eList;
	}

	/* (non-Javadoc)
	 * @see de.learnlib.ralib.tools.theories.InequalityGuardInstantiator#instantiateGuard(de.learnlib.ralib.theory.SDTGuard, gov.nasa.jpf.constraints.api.Valuation, de.learnlib.ralib.data.Constants, java.util.Collection)
	 */
	@Override
	public DataValue<T> instantiateGuard(SDTGuard g, Valuation val, Constants c,
			Collection<DataValue<T>> alreadyUsedValues) {
		// System.out.println("INSTANTIATING: " + g.toString());
		SymbolicDataValue.SuffixValue sp = g.getParameter();
		Valuation newVal = new Valuation();
		newVal.putAll(val);
		GuardExpression x = g.toExpr();
		Result res;
		if (g instanceof EqualityGuard) {
			// System.out.println("SOLVING: " + x);
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
			for (DataValue<T> au : alreadyUsedValues) {
				gov.nasa.jpf.constraints.expressions.Constant w = new gov.nasa.jpf.constraints.expressions.Constant(
						getJCType(), au.getId());
				Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, toVariable(sp));
				eList.add(auExpr);
			}

			if (newVal.containsValueFor(toVariable(sp))) {
				DataValue<T> spDouble = new DataValue<T>(getType(), (T) newVal.getValue(toVariable(sp)));
				gov.nasa.jpf.constraints.expressions.Constant spw = new gov.nasa.jpf.constraints.expressions.Constant(
						getJCType(), spDouble.getId());
				Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, toVariable(sp));
				eList.add(spExpr);
			}

			for (Variable var : newVal.getVariables()) {
				DataValue<T> spDouble = new DataValue<T>(getType(), (T)newVal.getValue(var));
				gov.nasa.jpf.constraints.expressions.Constant<T> spw = new gov.nasa.jpf.constraints.expressions.Constant(
						getJCType(), spDouble.getId());
				Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, var);
				eList.add(spExpr);
			}

			Expression<Boolean> _x = ExpressionUtil.and(eList);
			// System.out.println("SOLVING: " + _x + " with " + newVal);
			res = solver.solve(_x, newVal);
			// System.out.println("SOLVING:: " + res + " " + eList + " " +
			// newVal);
		}
		// System.out.println("VAL: " + newVal);
		// System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and
		// vals " + newVal.toString() + " and param-variable " +
		// sp.toVariable().toString());
		// System.out.println("x is " + x.toString());
		if (res == Result.SAT) {
			// System.out.println("SAT!!");
			// System.out.println(newVal.getValue(sp.toVariable()) + " " +
			// newVal.getValue(sp.toVariable()).getClass());
			DataValue<T> d = new DataValue<T>(getType(), DataValue.cast(newVal.getValue(toVariable(sp)), getType()));
			// System.out.println("return d: " + d.toString());
			return d;// new DataValue<Double>(doubleType, d);
		} else {
			// System.out.println("UNSAT: " + _x + " with " + newVal);
			return null;
		}
	}

}
