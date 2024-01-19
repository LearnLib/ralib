package de.learnlib.ralib.theory;

import java.util.Map;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;

public class UnrestrictedSuffixValue extends SuffixValueRestriction {

	public UnrestrictedSuffixValue(SuffixValue parameter) {
		super(parameter);
	}

	public UnrestrictedSuffixValue(UnrestrictedSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public GuardExpression toGuardExpression(Map<SymbolicDataValue, DataValue<?>> vals) {
		return new TrueGuardExpression();
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new UnrestrictedSuffixValue(this, shiftStep);
	}

}
