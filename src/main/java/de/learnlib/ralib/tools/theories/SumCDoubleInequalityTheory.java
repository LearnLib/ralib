package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
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
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory {
	private static int smBgFactor = 10; // the number of times maxSumC is deducted/added from/to the minimum/maximum to instantiate a smaller/bigger data value 
	// used to compute smBgStep
	private static int freshFactor = 100; // the number of times maxSumC is added to the maximum to instantiate a fresh data value
	// used to compute freshStep
	
	private List<DataValue<Double>> sumConstants;
	private List<DataValue<Double>> regularConstants;
	private Double freshStep;
	private DataValue<Double> smBgStep;
	
	private DataValue<Double> maxSumC;
	
	
	public SumCDoubleInequalityTheory() {
	}
	
	// the idea is that a fresh value should be far enough away s.t. it is always bigger than all previous values.
	
	public SumCDoubleInequalityTheory(DataType dataType, List<DataValue<Double>> sumConstants,
			List<DataValue<Double>> regularConstants) {
		super(dataType);
		setConstants(sumConstants, regularConstants);
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
				.filter(dv -> !canRemove(dv));
		
		List<DataValue<Double>> valAndSumsAndConsts = Stream.concat(valAndSums, regularConstants.stream())
				.collect(Collectors.toList()); 

		return valAndSumsAndConsts;
	}
	
	// if a value is already a SumCDv, it can only be operand in another SumCDv if its sum constant is a value other than 1.
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
		return new DataValue<Double>(fv.getType(), fv.getId() + this.freshStep);
	}
	
	
	protected IntervalDataValue<Double> pickIntervalDVManually(DataValue<Double> left, DataValue<Double> right) {
		return IntervalDataValue.instantiateNew(left, right, smBgStep);
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

	public void setConstants(Constants constants) {
		setConstants (constants.getSumCs(this.getType()), new ArrayList<>(constants.values(this.getType())));
	}
	
	private void setConstants (List<DataValue<Double>> sumConstants,
			List<DataValue<Double>> regularConstants) {
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
		this.maxSumC = this.sumConstants.isEmpty() ? new DataValue<Double>(this.getType(), 1.0)
				: Collections.max(this.sumConstants, new Cpr());
		this.freshStep = maxSumC.getId() * freshFactor;
		this.smBgStep = new DataValue<Double>(type, maxSumC.getId() * smBgFactor);
	}

	public List<EnumSet<DataRelation>> getRelations(List<DataValue<Double>> left, DataValue<Double> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		LOOP: 
			for (DataValue<Double> dv : left) {
				if (!this.sumConstants.isEmpty()) {
					for (int ind = 0; ind < this.sumConstants.size(); ind++)
						if (Double.valueOf((this.sumConstants.get(ind).getId() + dv.getId()))
								.compareTo(right.getId()) == 0) {
							if (ind == 0)
								ret.add(EnumSet.of(DataRelation.EQ_SUMC1));
							else if (ind == 1)
								ret.add(EnumSet.of(DataRelation.EQ_SUMC2));
							else
								throw new DecoratedRuntimeException("Over 2 sumcs not supported");
							continue LOOP;
						}
				}

				final int c = dv.getId().compareTo(right.getId());
				if (c == 0)
					ret.add(EnumSet.of(DataRelation.EQ));
				else if (c > 0)
					ret.add(EnumSet.of(DataRelation.LT));
				else
					ret.add(EnumSet.of(DataRelation.DEFAULT));
			}
		return ret;
	}
}
