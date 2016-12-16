package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory {
	// default constants
	private static Double[] defaultSumConst = new Double[] { 1.0, 1000.0
			// 10000.0
	};
	private static Double[] defaultRegularConst = new Double[] {
			// 0.0
	};

	private List<DataValue<Double>> sumConstants;
	private List<DataValue<Double>> regularConstants;

	public SumCDoubleInequalityTheory() {
		super();
	}

	public SumCDoubleInequalityTheory(DataType dataType) {
		// the constants have to be introduced manually
		super(dataType);
		setupDefaultConstants(dataType);
	}

	public SumCDoubleInequalityTheory(DataType dataType, List<DataValue<Double>> sumConstants,
			List<DataValue<Double>> regularConstants) {
		super(dataType);
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
	}

	public void setType(DataType dataType) {
		super.setType(dataType);
		setupDefaultConstants(dataType);
	}

	private void setupDefaultConstants(DataType<Double> dataType) {
		this.sumConstants = Arrays.asList(defaultSumConst).stream().map(c -> new DataValue<Double>(dataType, c))
				.collect(Collectors.toList());
		this.regularConstants = Arrays.asList(defaultRegularConst).stream().map(c -> new DataValue<Double>(dataType, c))
				.collect(Collectors.toList());
	}

	public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
		// assume we can just sort the list and get the values
		List<DataValue<Double>> sortedList = makeNewPotsWithSumC(dvs);

		// sortedList.addAll(dvs);
		Collections.sort(sortedList, new Cpr());

		// System.out.println("I'm sorted! " + sortedList.toString());
		return sortedList;
	}

	/**
	 * Creates a list of values comprising the data values supplied, plus all
	 * values obtained by adding each of the sum constants to each of the data
	 * values supplied.
	 * 
	 */
	private List<DataValue<Double>> makeNewPotsWithSumC(List<DataValue<Double>> dvs) {
		Stream<DataValue<Double>> dvWithoutConsts = 
				dvs.stream().filter(dv -> !regularConstants.contains(dv));
		Stream<DataValue<Double>> valAndSums = dvWithoutConsts
				.map(val -> Stream.concat(Stream.of(val), 
					sumConstants.stream()
						.map(sum -> new SumCDataValue<Double>(val, sum))
						.filter(sumc -> !dvs.contains(sumc)))
				).flatMap(s -> s).distinct()
				.filter(dv -> !canRemove(dv))
				;
		
		List<DataValue<Double>> valAndSumsAndConsts = Stream.concat(valAndSums, regularConstants.stream())
				.collect(Collectors.toList()); 

		return valAndSumsAndConsts;
	}
	
	private boolean canRemove(DataValue<Double> dv) {
		Set<Object> sumCsOtherThanOne = new HashSet<Object>();
		while (dv instanceof SumCDataValue) {
			SumCDataValue<Double> sum = ((SumCDataValue<Double>) dv);
			if (!DataValue.ONE(this.getType()).equals(sum.getConstant())) {
				if (sumCsOtherThanOne.contains(sum.getConstant()))
					return true;
				sumCsOtherThanOne.add(sum.getConstant());
			}
			dv = sum.getOperand();
		}
		return false;
	}

	public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
		List<DataValue<Double>> valsWithConsts = new ArrayList<>(vals);
		valsWithConsts.addAll(this.regularConstants);

		// we add regular constants
		DataValue<Double> fv = super.getFreshValue(valsWithConsts);
		DataValue<Double> maxSumC = this.sumConstants.isEmpty() ? new DataValue<Double>(this.getType(), 1.0)
				: Collections.max(this.sumConstants, new Cpr());
		return new DataValue<Double>(fv.getType(), fv.getId() + maxSumC.getId() * 100);
	}

	public ValueMapper<Double> getValueMapper() {
		return new SumCInequalityValueMapper<Double>(this, this.sumConstants);
	}

	public Collection<DataValue<Double>> getAllNextValues(List<DataValue<Double>> vals) {
		// adds sumc constants to interesting values
		List<DataValue<Double>> potential = getPotential(vals);

		// the superclass should complete this list with in-between values.
		return super.getAllNextValues(potential);
	}

	public void setSumcConstants(SumConstants constants) {
		this.sumConstants = new ArrayList<>(constants.values(this.getType()));
	}

	public void setConstants(Constants constants) {
		this.regularConstants = new ArrayList<>(constants.values(this.getType()));
	}

	@Override
	public List<EnumSet<DataRelation>> getRelations(List<DataValue<Double>> left, DataValue<Double> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		LOOP: for (DataValue<Double> dv : left) {
			if (!this.sumConstants.isEmpty()) {
				for (int ind = 0; ind < this.sumConstants.size(); ind++)
					if (Double.valueOf((this.sumConstants.get(ind).getId() + dv.getId()))
							.compareTo(right.getId()) == 0) {
						switch (ind) {
						case 0:
							ret.add(EnumSet.of(DataRelation.EQ_SUMC1, DataRelation.DEQ_SUMC1));
							break;
						case 1:
							ret.add(EnumSet.of(DataRelation.EQ_SUMC2, DataRelation.DEQ_SUMC2));
							break;
						default:
							throw new DecoratedRuntimeException("Over 2 sumcs not supported");
						}
						continue LOOP;
					}
			}

			final int c = dv.getId().compareTo(right.getId());
			if (c == 0)
				ret.add(EnumSet.of(DataRelation.EQ, DataRelation.DEQ));
			else if (c > 0)
				ret.add(EnumSet.of(DataRelation.LT));
			else
				ret.add(EnumSet.noneOf(DataRelation.class));
		}

		return ret;
	}
}
