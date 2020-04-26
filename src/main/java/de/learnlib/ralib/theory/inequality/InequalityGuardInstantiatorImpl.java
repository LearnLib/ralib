package de.learnlib.ralib.theory.inequality;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toExpression;
import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.solver.jconstraints.JContraintsUtil;
import de.learnlib.ralib.theory.SDTGuard;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class InequalityGuardInstantiatorImpl<T extends Comparable<T>> implements InequalityGuardInstantiator<T> {

	private final DataType<T> type;
	private final Type<T> jcType;
	private final ConstraintSolver solver;

	public InequalityGuardInstantiatorImpl(DataType<T> type, gov.nasa.jpf.constraints.types.Type<T> jcType, ConstraintSolver solver) {
		this.type = type;
		this.jcType = jcType;
		this.solver = solver;
	}

	public InequalityGuardInstantiatorImpl(DataType<T> type, ConstraintSolver solver) {
		this(type, JContraintsUtil.getJCType(type.getBase()), solver);
	}

	private DataType<T> getType() {
		return type;
	}

	private Type<T> getJCType() {
		return jcType;
	}

	
	@Override
	public DataValue<T> instantiateGuard(SDTGuard g, Valuation val, Constants c,
			Collection<DataValue<T>> alreadyUsedValues) {
		// System.out.println("INSTANTIATING: " + g.toString());
		SymbolicDataValue.SuffixValue sp = g.getParameter();
		Valuation newVal = new Valuation();
		newVal.putAll(val);
		Result res;

		List<Expression<Boolean>> eList = new ArrayList<>();
		// add equalities tying sdv's to concrete values
		for (SymbolicDataValue sdv : g.getAllSDVsFormingGuard()) {
			DataValue<Double> sdvVal = new DataValue(getType(), val.getValue(toVariable(sdv)));
			gov.nasa.jpf.constraints.expressions.Constant wm = new gov.nasa.jpf.constraints.expressions.Constant(
					getJCType(), sdvVal.getId());
			// add the constant equivalence expression to the list
			eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, toVariable(sdv)));
		}
			
		// add the guard
		eList.add(toExpression(g.toExpr()));
	
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

		// System.out.println("VAL: " + newVal);
		// System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and
		// vals " + newVal.toString() + " and param-variable " +
		// sp.toVariable().toString());
		// System.out.println("x is " + x.toString());
		if (res == Result.SAT) {
			// System.out.println("SAT!!");
			// System.out.println(newVal.getValue(sp.toVariable()) + " " +
			// newVal.getValue(sp.toVariable()).getClass());
			DataValue<T> d = new DataValue(getType(), DataValue.cast(newVal.getValue(toVariable(sp)), getType()));
			// System.out.println("return d: " + d.toString());
			return d;
		} else {
			// System.out.println("UNSAT: " + _x + " with " + newVal);
			return null;
		}
	}
	
	

	public static void main(String [] args) {
		gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory fact = new gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory();
		gov.nasa.jpf.constraints.api.ConstraintSolver solver = fact.createSolver("z3");
		
		gov.nasa.jpf.constraints.expressions.Constant<Double> cst = new gov.nasa.jpf.constraints.expressions.Constant<Double>(
				BuiltinTypes.DOUBLE, 1.0);
		
		Variable<Double> a = new Variable<Double>(BuiltinTypes.DOUBLE, "a");
		NumericBooleanExpression equExpression = new NumericBooleanExpression(cst, NumericComparator.EQ, a);
		Valuation ctx = new Valuation();
		
		Result res = solver.solve(equExpression, ctx);
		System.out.println("Result: " + res + " Context: "  + ctx);
		
		NumericCompound<Double> incOfA = gov.nasa.jpf.constraints.expressions.NumericCompound.create(a, 
	        		NumericOperator.PLUS, cst);
		
		equExpression = new NumericBooleanExpression(cst, NumericComparator.EQ, incOfA);
		ctx = new Valuation();
		
		res = solver.solve(equExpression, ctx);
		System.out.println("Result: " + res + " Context: "  + ctx);
		
		ctx = new Valuation();
		NumericBooleanExpression impExpression = new NumericBooleanExpression(a, NumericComparator.EQ, incOfA);
		
		res = solver.solve(impExpression, ctx);
		System.out.println("Result: " + res + " Context: "  + ctx);
		
		
	}

}
