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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.mapper.Determinizer;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.theory.sumc.inequality.TestTwoWayTCPTreeOracle;

//TODO instead of 0 reduction, just use output constants (constants that are only found in outputs and should not be used)
/**
 * This theory implementation has a failing test case (see {@link TestTwoWayTCPTreeOracle}). 
 * Integers are messy to work with. 
 */
public class IntegerSumCInequalityTheory extends IntegerInequalityTheory implements SumCTheory{

	private static int SM_BG_FACTOR = 10;  
	
	private static int FRESH_FACTOR = 100; 

	
	private List<DataValue<Integer>> sortedSumConsts;
	private List<DataValue<Integer>> regularConstants;
	private int freshStep;
	private DataValue<Integer> smBgStep;

	public IntegerSumCInequalityTheory() {
		super();
	}

	public IntegerSumCInequalityTheory(DataType dataType) {
		// the constants have to be introduced manually
		this(dataType, Collections.emptyList(), Collections.emptyList());
	}
	
	public IntegerSumCInequalityTheory(DataType dataType, List<DataValue<Integer>> sumConstants, List<DataValue<Integer>> regularConstants) {
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
		sortedSumConsts = new ArrayList<>(sumConstants);
		Collections.sort(sortedSumConsts, new Cpr());
		this.regularConstants = regularConstants;
		Integer step = sortedSumConsts.isEmpty() ? 1 :sortedSumConsts.get(sortedSumConsts.size()-1).getId();
		freshStep = step * FRESH_FACTOR;
		smBgStep = new DataValue<Integer>(type, step * SM_BG_FACTOR);
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
				).flatMap(s -> s).distinct();
		
		List<DataValue<Integer>> valAndSumsAndConsts = valAndSums //Stream.concat(valAndSums, regularConstants.stream())
				.collect(Collectors.toList()); 

		return valAndSumsAndConsts;
	}
	
	
	/**
	 * The next fresh value
	 */
	public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
		List<DataValue<Integer>> valsWithConsts = new ArrayList<>(vals);
		// we add regular constants
		valsWithConsts.addAll(regularConstants);

		DataValue<Integer> fv = super.getFreshValue(valsWithConsts);
		Integer nextFresh;
		for(nextFresh=0; nextFresh<fv.getId(); nextFresh+=freshStep);

		return new DataValue<Integer>(fv.getType(), nextFresh);
	}
	
	
	public IntervalDataValue<Integer> pickIntervalDataValue(DataValue<Integer> left, DataValue<Integer> right) {
//		if (right != null && left!=null) 
//			if (right.getId() - left.getId() > this.freshStep && right.getId() - left.getId() < this.freshStep * 10) 
//				throw new DecoratedRuntimeException("This shouldn't be happening").addDecoration("left", left).addDecoration("right", right);
		return IntervalDataValue.instantiateNew(left, right, smBgStep);
	}

	public Determinizer<Integer> getDeterminizer() {
		return new SumCInequalityDeterminizer<Integer>(this, sortedSumConsts);
	}

	public Collection<DataValue<Integer>> getAllNextValues(List<DataValue<Integer>> vals) {
		// adds sumc constants to interesting values
		List<DataValue<Integer>> potential = getPotential(vals);
		
		if (potential.isEmpty()) 
			return Collections.singleton(this.getFreshValue(vals));
		else {
			// the superclass should complete this list with in-between values.
			Collection<DataValue<Integer>> nextValues = super.getAllNextValues(potential);
			// We are not interested non positive numbers. (incl constant)
			nextValues.removeIf(v -> v.getId() <= 0L);
			return nextValues;
		}
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
		return sortedSumConsts.isEmpty() ? DataValue.ZERO(type, getDomainType()):
				sortedSumConsts.get(sortedSumConsts.size()-1);
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
				int ind = sortedSumConsts.indexOf(sumcEqual.get());
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
					int ind = sortedSumConsts.indexOf(sumcLt.get());
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

	@Override
	public Class<Integer> getDomainType() {
		return Integer.class;
	}
}
