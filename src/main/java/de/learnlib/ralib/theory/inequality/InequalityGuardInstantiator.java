package de.learnlib.ralib.theory.inequality;

import java.util.Collection;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.SDTGuard;
import gov.nasa.jpf.constraints.api.Valuation;

public interface InequalityGuardInstantiator<T extends Comparable<T>> {

	//TODO We should replace valuation with a mapping from SDV to DVs. That way we can decouple from the JConstraints API.
	DataValue<T> instantiateGuard(SDTGuard g, Valuation val, Constants c, Collection<DataValue<T>> alreadyUsedValues);

}