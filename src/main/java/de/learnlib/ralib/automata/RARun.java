package de.learnlib.ralib.automata;


import java.util.Map;

import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.smt.VarsValuationVisitor;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class RARun {

	private final RALocation[] locations;
	private final RegisterValuation[] valuations;
	private final PSymbolInstance[] symbols;
	private final Transition[] transitions;

	public RARun(RALocation[] locations, RegisterValuation[] valuations, PSymbolInstance[] symbols, Transition[] transitions) {
		this.locations = locations;
		this.valuations = valuations;
		this.symbols = symbols;
		this.transitions = transitions;
	}

	public RALocation getLocation(int i) {
		return locations[i];
	}

	public RegisterValuation getValuation(int i) {
		return valuations[i];
	}

	public PSymbolInstance getTransitionSymbol(int i) {
		return symbols[i-1];
	}

	public Transition getRATransition(int i) {
		return transitions[i-1];
	}

	public Expression<Boolean> getGuard(int i) {
		Transition transition = getRATransition(i);
		if (transition == null) {
			return null;
		}
		if (transition instanceof OutputTransition) {
			return outputGuard((OutputTransition) transition, getTransitionSymbol(i));
		}
		VarsValuationVisitor vvv = new VarsValuationVisitor();
		Expression<Boolean> guard = transition.getGuard();
		RegisterValuation val = getValuation(i);
		return vvv.apply(guard, val);
	}

	private Expression<Boolean> outputGuard(OutputTransition t, PSymbolInstance symbol) {
		Expression<Boolean> guard = t.getGuard();
		DataValue[] vals = symbol.getParameterValues();
		for (Map.Entry<Parameter, SymbolicDataValue> e : t.getOutput().getOutput().entrySet()) {
			Parameter p = e.getKey();
			SymbolicDataValue s = e.getValue();
			DataValue d = vals[p.getId()-1];
			Expression<Boolean> eq = new NumericBooleanExpression(s, NumericComparator.EQ, d);
			guard = ExpressionUtil.and(guard, eq);
		}
		return guard;
	}

	@Override
	public String toString() {
		if (locations.length == 0) {
			return "ε";
		}

		String str = "<" + locations[0] + ", " + valuations[0] + ">";
		for (int i = 1; i < locations.length; i++) {
			str = str +
					" -- " +
					symbols[i-1] +
					" -- <" +
					locations[i] +
					", " +
					valuations[i] +
					">";
		}

		return str;
	}
}
