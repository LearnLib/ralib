package de.learnlib.ralib.theory.inequality;

import java.util.Map;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.SDTGuard;

public interface IntervalMerger<T extends Comparable<T>> {
	Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> tempGuards, Map<SDTGuard, DataValue<T>> instantiations);
}
