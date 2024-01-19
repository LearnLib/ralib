package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;

public class FreshSuffixValue extends SuffixValueRestriction {
	public FreshSuffixValue(SuffixValue param) {
		super(param);
	}

	public FreshSuffixValue(FreshSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public GuardExpression toGuardExpression(Map<SymbolicDataValue, DataValue<?>> val) {
		List<GuardExpression> expr = new ArrayList<>();
		for (Map.Entry<SymbolicDataValue, DataValue<?>> e : val.entrySet()) {
			GuardExpression g = new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter, Relation.NOT_EQUALS, e.getKey());
			expr.add(g);
		}
		GuardExpression[] exprArr = new GuardExpression[expr.size()];
		expr.toArray(exprArr);
		return new Conjunction(exprArr);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new FreshSuffixValue(this, shiftStep);
	}
}
