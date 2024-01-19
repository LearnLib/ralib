package de.learnlib.ralib.theory.equality;

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
import de.learnlib.ralib.theory.SuffixValueRestriction;

public class EqualRestriction extends SuffixValueRestriction {
	private final SuffixValue equalParam;

	public EqualRestriction(SuffixValue param, SuffixValue equalParam) {
		super(param);
		this.equalParam = equalParam;
	}

	public EqualRestriction(EqualRestriction other) {
		super(other);
		equalParam = new SuffixValue(other.equalParam.getType(), other.equalParam.getId());
	}

	public EqualRestriction(EqualRestriction other, int shift) {
		super(other, shift);
		equalParam = new SuffixValue(other.equalParam.getType(), other.equalParam.getId());
	}

	@Override
	public GuardExpression toGuardExpression(Map<SymbolicDataValue, DataValue<?>> vals) {
		assert vals.keySet().contains(equalParam);

		List<GuardExpression> expr = new ArrayList<>();
		expr.add(new AtomicGuardExpression<SuffixValue, SuffixValue>(parameter, Relation.EQUALS, equalParam));
		for (Map.Entry<SymbolicDataValue, DataValue<?>> e : vals.entrySet()) {
			if (!e.getKey().equals(equalParam)) {
				expr.add(new AtomicGuardExpression<SuffixValue, SymbolicDataValue>(parameter, Relation.NOT_EQUALS, e.getKey()));
			}
		}
		GuardExpression[] exprArr = new GuardExpression[expr.size()];
		expr.toArray(exprArr);
		return new Conjunction(exprArr);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new EqualRestriction(this, shiftStep);
	}

}
