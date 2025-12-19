package de.learnlib.ralib.automata;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.smt.VarsValuationVisitor;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 * Data structure containing the locations, register valuations, symbol instances
 * and transitions at each step of a run of a hypothesis over a data word.
 *
 * @author fredrik
 */
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

	/**
	 * Get the guard of the {@code Transition} at index {@code i}. If the {@code Transition}
	 * is an {@code OutputTransition}, the guard is computed from the transition's
	 * {@code OutputMapping}.
	 *
	 * @param i
	 * @return
	 */
	public Expression<Boolean> getGuard(int i) {
		Transition transition = getRATransition(i);
		if (transition == null) {
			return null;
		}
		return transition instanceof OutputTransition ?
				outputGuard((OutputTransition) transition) :
					transition.getGuard();
	}

	/**
	 * Get the guard of the {@code Transition} at index {@code i}. If the {@code Transition}
	 * is an {@code OutputTransition}, the guard is computed from the transition's
	 * {@code OutputMapping}. Registers of the guard will be evaluated according to  the
	 * data values from the register valuation at index {@code i}, and constants will be
	 * evaluated according to {@code consts}.
	 *
	 * @param i
	 * @return
	 */
	public Expression<Boolean> getGuard(int i, Constants consts) {
		Expression<Boolean> guard = getGuard(i);
		VarsValuationVisitor vvv = new VarsValuationVisitor();
		Mapping<SymbolicDataValue, DataValue> vals = new Mapping<>();
		vals.putAll(getValuation(i));
		vals.putAll(consts);
		return vvv.apply(guard, vals);
	}

	private Expression<Boolean> outputGuard(OutputTransition t) {
		OutputMapping out = t.getOutput();

		Set<Parameter> params = new LinkedHashSet<>();
		params.addAll(out.getFreshParameters());
		params.addAll(out.getOutput().keySet());
		Set<SymbolicDataValue> regs = new LinkedHashSet<>();
		regs.addAll(out.getOutput().values());

		Expression[] expressions = new Expression[params.size()];
		int index = 0;

		// fresh parameters
		List<Parameter> prior = new ArrayList<>();
		List<Parameter> fresh = new ArrayList<>(out.getFreshParameters());
		Collections.sort(fresh, (a,b) -> Integer.compare(a.getId(), b.getId()));
		for (Parameter p : fresh) {
			Expression[] diseq = new Expression[prior.size() + regs.size()];
			int i = 0;
			for (Parameter prev : prior) {
				diseq[i++] = new NumericBooleanExpression(p, NumericComparator.NE, prev);
			}
			for (SymbolicDataValue s : regs) {
				diseq[i++] = new NumericBooleanExpression(p, NumericComparator.NE, s);
			}
			expressions[index++] = ExpressionUtil.and(diseq);
			prior.add(p);
		}

		// mapped parameters
		for (Map.Entry<Parameter, SymbolicDataValue> e : out.getOutput().entrySet()) {
			Parameter p = e.getKey();
			SymbolicDataValue s = e.getValue();
			expressions[index++] = new NumericBooleanExpression(p, NumericComparator.EQ, s);
		}

		return ExpressionUtil.and(expressions);
	}

	public Word<PSymbolInstance> getPrefix(int id) {
		return Word.fromArray(symbols, 0, id);
	}

	public Word<PSymbolInstance> getSuffix(int id) {
		return Word.fromArray(symbols, id, symbols.length - id);
	}

	public Word<PSymbolInstance> getWord() {
		return getPrefix(symbols.length);
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
