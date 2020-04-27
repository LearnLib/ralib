package de.learnlib.ralib.tools.theories;

import static de.learnlib.ralib.theory.DataRelation.DEFAULT;
import static de.learnlib.ralib.theory.DataRelation.DEQ;
import static de.learnlib.ralib.theory.DataRelation.EQ;
import static de.learnlib.ralib.theory.DataRelation.LT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.mapper.Determinizer;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.inequality.InequalityGuardLogic;
import de.learnlib.ralib.theory.inequality.InequalityGuardMerger;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;

/**
 * A general classs for number inequality theories
 */
public abstract class NumberInequalityTheory<N extends Comparable<N>> extends InequalityTheoryWithEq<N> {
	protected final class Cpr implements Comparator<DataValue<N>> {

		@Override
		public int compare(DataValue<N> one, DataValue<N> other) {
			return one.getId().compareTo(other.getId());
		}
	}
	
	@Override
	public EnumSet<DataRelation> recognizedRelations() {
		return EnumSet.of(DEQ, EQ, LT, DEFAULT);
	}

	public NumberInequalityTheory(InequalityGuardMerger merger) {
		super(merger);
	}

	public NumberInequalityTheory(InequalityGuardMerger merger, DataType t) {
		this(merger);
		super.setType(t);
	}

	public List<DataValue<N>> getPotential(List<DataValue<N>> dvs) {
		// assume we can just sort the list and get the values
		List<DataValue<N>> sortedList = new ArrayList<>();
		for (DataValue<N> d : dvs)
			sortedList.add(d);

		// sortedList.addAll(dvs);
		Collections.sort(sortedList, new Cpr());

		return sortedList;
	}

	public Determinizer<N> getDeterminizer() {
		return new SumCInequalityDeterminizer(this);
	}

	@Override
	public DataValue<N> getFreshValue(List<DataValue<N>> vals) {
		if (vals.isEmpty()) {
			return DataValue.ONE(type, getDomainType());
		} 
		List<DataValue<N>> potential = getPotential(vals);
		if (potential.isEmpty()) {
			return DataValue.ONE(type, getDomainType());
		}

		// log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in
		// " + ifValues.toString() + " is " + smallest);
		DataValue<N> biggestDv = Collections.max(potential, new Cpr());
		return (DataValue<N>) DataValue.add(biggestDv, DataValue.ONE(type, getDomainType()));
	}

	@Override
	public Collection<DataValue<N>> getAllNextValues(List<DataValue<N>> vals) {
		Set<DataValue<N>> nextValues = new LinkedHashSet<>();
		nextValues.addAll(vals);
		List<DataValue<N>> distinctValList = new ArrayList<>(nextValues);

		if (distinctValList.isEmpty()) {
			DataValue<N> fv = this.getFreshValue(vals);
			nextValues.add(fv);
		} else {
			Collections.sort(distinctValList, new Cpr());
			if (distinctValList.size() > 1) {
				for (int i = 0; i < (distinctValList.size() - 1); i++) {
					IntervalDataValue<N> intVal = this.pickIntervalDataValue(distinctValList.get(i),
							distinctValList.get(i + 1));
					nextValues.add(intVal);
				}
			}
			DataValue<N> min = Collections.min(distinctValList, new Cpr());
			nextValues.add(this.pickIntervalDataValue(null, min));
			DataValue<N> max = Collections.max(distinctValList, new Cpr());
			nextValues.add(this.pickIntervalDataValue(max, null));
		}
		return nextValues;
	}

	@Override
	public List<EnumSet<DataRelation>> getRelations(List<DataValue<N>> left, DataValue<N> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		left.stream().forEach((dv) -> {
			final int c = dv.getId().compareTo(right.getId());
			switch (c) {
			case 0:
				ret.add(EnumSet.of(DataRelation.EQ));
				break;
			case 1:
				ret.add(EnumSet.of(DataRelation.LT));
				break;
			default:
				ret.add(EnumSet.of(DataRelation.DEFAULT));
				break;
			}
		});

		return ret;
	}

	public SDTGuardLogic getGuardLogic() {
		return new InequalityGuardLogic();
	}
}
