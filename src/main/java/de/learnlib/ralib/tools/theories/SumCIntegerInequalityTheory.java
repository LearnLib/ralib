package de.learnlib.ralib.tools.theories;

import static de.learnlib.ralib.theory.DataRelation.DEFAULT;
import static de.learnlib.ralib.theory.DataRelation.DEQ;
import static de.learnlib.ralib.theory.DataRelation.DEQ_SUMC1;
import static de.learnlib.ralib.theory.DataRelation.DEQ_SUMC2;
import static de.learnlib.ralib.theory.DataRelation.EQ;
import static de.learnlib.ralib.theory.DataRelation.EQ_SUMC1;
import static de.learnlib.ralib.theory.DataRelation.EQ_SUMC2;
import static de.learnlib.ralib.theory.DataRelation.LT;
import static de.learnlib.ralib.theory.DataRelation.LT_SUMC1;
import static de.learnlib.ralib.theory.DataRelation.LT_SUMC2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.mapper.ValueMapper;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCIntegerInequalityTheory extends IntegerInequalityTheory implements SumCTheory{

	private static int smBgFactor = 10;  
	
	// maxSumC multiplied this factor results in the fresh step. Fresh values at a fresh step distance, 
	// in increasing order, starting from 0.
	private static int freshFactor = 100; 

	
	private List<DataValue<Integer>> sortedSumConsts;
	private List<DataValue<Integer>> regularConstants;
	private int freshStep;
	private DataValue<Integer> smBgStep;

	public SumCIntegerInequalityTheory() {
		super();
	}

	public SumCIntegerInequalityTheory(DataType<Integer> dataType) {
		// the constants have to be introduced manually
		this(dataType, Collections.emptyList(), Collections.emptyList());
	}
	
	public SumCIntegerInequalityTheory(DataType<Integer> dataType, List<DataValue<Integer>> sumConstants, List<DataValue<Integer>> regularConstants) {
		super(dataType);
		setConstants(sumConstants, regularConstants);
	}

	public void setSumcConstants(SumConstants constants) {
		this.sortedSumConsts = new ArrayList<>(constants.values(this.type));
	}
	
	public void setConstants(Constants constants) {
		setConstants (constants.getSumCs(this.getType()), new ArrayList<>(constants.values(this.getType())));
	}
	
	private void setConstants (List<DataValue<Integer>> sumConstants,
			List<DataValue<Integer>> regularConstants) {
		this.sortedSumConsts = new ArrayList<>(sumConstants);
		Collections.sort(this.sortedSumConsts, new Cpr());
		this.regularConstants = regularConstants;
		Integer step = this.sortedSumConsts.isEmpty() ? 1 :this.sortedSumConsts.get(this.sortedSumConsts.size()-1).getId();
		this.freshStep = step * freshFactor;
		this.smBgStep = new DataValue<Integer>(type, step * smBgFactor);
	}
	
	
    @Override
	public EnumSet<DataRelation> recognizedRelations() {
		return EnumSet.of(DEQ, EQ, LT, DEFAULT, LT_SUMC1, LT_SUMC2, EQ_SUMC1, EQ_SUMC2, DEQ_SUMC1, DEQ_SUMC2);
	}

	public List<DataValue<Integer>> getPotential(List<DataValue<Integer>> dvs) {
		// assume we can just sort the list and get the values
		List<DataValue<Integer>> sortedList = makeNewPotsWithSumC(dvs);

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
	private List<DataValue<Integer>> makeNewPotsWithSumC(List<DataValue<Integer>> dvs) {
		Stream<DataValue<Integer>> dvWithoutConsts = 
				dvs.stream().filter(dv -> !regularConstants.contains(dv));
		Stream<DataValue<Integer>> valAndSums = dvWithoutConsts
				.map(val -> Stream.concat(Stream.of(val), 
					sortedSumConsts.stream()
						.map(sum -> new SumCDataValue<Integer>(val, sum))
						.filter(sumc -> !dvs.contains(sumc)))
				).flatMap(s -> s).distinct()
				.filter(dv -> !canRemove(dv));
		
		List<DataValue<Integer>> valAndSumsAndConsts = Stream.concat(valAndSums, regularConstants.stream())
				.collect(Collectors.toList()); 

		return valAndSumsAndConsts;
	}
	
	// if a value is already a SumCDv, it can only be operand in another SumCDv if its sum constant is ONE.
	// this is a hack optimization for TCP
	private boolean canRemove(DataValue<Integer> dv) {
		Set<Object> sumCsOtherThanOne = new HashSet<Object>();
		while (dv instanceof SumCDataValue) {
			SumCDataValue<Integer> sum = ((SumCDataValue<Integer>) dv);
			if (!DataValue.ONE(this.getType()).equals(sum.getConstant())) {
				if (sumCsOtherThanOne.contains(sum.getConstant()))
					return true;
				sumCsOtherThanOne.add(sum.getConstant());
			}
			dv = sum.getOperand();
		}
		return false;
	}

	/**
	 * The next fresh value
	 */
	public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
		List<DataValue<Integer>> valsWithConsts = new ArrayList<>(vals);
		// we add regular constants
		valsWithConsts.addAll(this.regularConstants);

		DataValue<Integer> fv = super.getFreshValue(valsWithConsts);
		Integer nextFresh;
		for(nextFresh=0; nextFresh<fv.getId(); nextFresh+=this.freshStep);
		
		while (nextFresh - fv.getId() < this.smBgStep.getId() * 5) 
			nextFresh += this.freshStep;
		
		return new DataValue<Integer>(fv.getType(), nextFresh);
	}
	
	
	public IntervalDataValue<Integer> pickIntervalDataValue(DataValue<Integer> left, DataValue<Integer> right) {
		if (right != null && left!=null) 
			if (right.getId() - left.getId() > this.freshStep && right.getId() - left.getId() < this.freshStep * 10) 
				throw new DecoratedRuntimeException("Unexpectedly broad range").addDecoration("left", left).addDecoration("right", right);
		return IntervalDataValue.instantiateNew(left, right, smBgStep);
	}

	public ValueMapper<Integer> getValueMapper() {
		return new SumCInequalityValueMapper<Integer>(this, this.sortedSumConsts);
	}

	public Collection<DataValue<Integer>> getAllNextValues(List<DataValue<Integer>> vals) {
		// adds sumc constants to interesting values
		List<DataValue<Integer>> potential = getPotential(vals);

		// the superclass should complete this list with in-between values.
		return super.getAllNextValues(potential);
	}

	public List<EnumSet<DataRelation>> getRelations(List<DataValue<Integer>> left, DataValue<Integer> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		for (DataValue<Integer> dv : left) {
			DataRelation rel = getRelation(dv, right);
			ret.add(EnumSet.of(rel));	
		}
		return ret;
	}
	
	private DataValue<Integer> maxSumCOrZero () {
		return this.sortedSumConsts.isEmpty() ? DataValue.ZERO(type):
				this.sortedSumConsts.get(sortedSumConsts.size()-1);
	}
	
	private DataRelation getRelation(DataValue<Integer> dv, DataValue<Integer> right) {
		DataRelation rel = null;
		if (dv.equals(right))
			rel = DataRelation.EQ;
		else if (dv.getId().compareTo(right.getId()) > 0)
			rel = DataRelation.LT;
		else if ( Integer.valueOf(dv.getId()+ maxSumCOrZero().getId()).compareTo(right.getId()) < 0) 
			rel = DataRelation.DEFAULT;
		else 
		{
			Optional<DataValue<Integer>> sumcEqual = sortedSumConsts.stream().filter(c -> Integer.valueOf(c.getId() + dv.getId())
					.equals(right.getId())).findFirst();
			if (sumcEqual.isPresent()) {
				int ind = this.sortedSumConsts.indexOf(sumcEqual.get());
				if (ind == 0) 
					rel = DataRelation.EQ_SUMC1; 
				else if (ind == 1) 
					rel = DataRelation.EQ_SUMC2;
				else
					throw new DecoratedRuntimeException("Over 2 sumcs not supported");
			} else {
				Optional<DataValue<Integer>> sumcLt = sortedSumConsts.stream().filter(c -> 
				Integer.valueOf(c.getId() + dv.getId()).compareTo(right.getId()) > 0).findFirst();
				if (sumcLt.isPresent()) {
					int ind = this.sortedSumConsts.indexOf(sumcLt.get());
					if (ind == 0) 
						rel = DataRelation.LT_SUMC1; 
					else if (ind == 1) 
						rel = DataRelation.LT_SUMC2;
					else
						throw new DecoratedRuntimeException("Over 2 sumcs not supported");
				} else 
					throw new DecoratedRuntimeException("Exhausted all cases");
			}
		}
		return rel;
	}
}
