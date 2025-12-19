package de.learnlib.ralib.theory.equality;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class UnmappedEqualityRestriction extends AbstractSuffixValueRestriction {

	public UnmappedEqualityRestriction(SuffixValue parameter) {
		super(parameter);
	}

	public UnmappedEqualityRestriction(UnmappedEqualityRestriction other, int shift) {
		super(other, shift);
	}

	@Override
	public AbstractSuffixValueRestriction shift(int shiftStep) {
		return new UnmappedEqualityRestriction(this, shiftStep);
	}

	@Override
	public AbstractSuffixValueRestriction concretize(Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		Set<DataValue> params = new LinkedHashSet<>();
		mapping.forEach((s,d) -> {if (s.isParameter()) params.add(d);});
		mapping.forEach((s,d) -> {if (!s.isParameter()) params.remove(d);});
		return SuffixValueRestriction.equalityRestriction(parameter, params);
//		Set<DataValue> unmappedVals = new LinkedHashSet<>(mapping.values());
//		Set<DataValue> mappedVals = new LinkedHashSet<>();
//		for (Map.Entry<? extends SymbolicDataValue, DataValue> entry : mapping.entrySet()) {
//			SymbolicDataValue sdv = entry.getKey();
//			if (sdv.isConstant() || sdv.isRegister() || sdv.isSuffixValue()) {
//				mappedVals.add(entry.getValue());
//			}
//		}
//		unmappedVals.removeAll(mappedVals);
//		return SuffixValueRestriction.disequalityRestriction(parameter, mappedVals);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue> vals) {
		Set<SymbolicDataValue> nonParams = new LinkedHashSet<>();
		vals.stream().filter(s -> !s.isParameter()).forEach(s-> nonParams.add(s));
		Expression[] diseqs = new Expression[nonParams.size()];
		int i = 0;
		for (SymbolicDataValue sdv : nonParams) {
			diseqs[i++] = new NumericBooleanExpression(parameter, NumericComparator.NE, sdv);
		}
		return ExpressionUtil.and(diseqs);
	}

	@Override
	public AbstractSuffixValueRestriction merge(AbstractSuffixValueRestriction other,
			Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
		throw new RuntimeException("Not supported for this type of restriction");
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public String toString() {
		return "Unmapped(" + parameter + ")";
	}
}
