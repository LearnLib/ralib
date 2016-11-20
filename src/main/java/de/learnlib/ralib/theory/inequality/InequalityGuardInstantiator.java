package de.learnlib.ralib.theory.inequality;

import java.util.Collection;
import java.util.List;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.SDTGuard;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;

public interface InequalityGuardInstantiator<T extends Comparable<T>> {

	List<Expression<Boolean>> instantiateGuard(SDTGuard g, Valuation val);

	DataValue<T> instantiateGuard(SDTGuard g, Valuation val, Constants c, Collection<DataValue<T>> alreadyUsedValues);

}