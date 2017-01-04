package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCIntegerInequalityTheory extends IntegerInequalityTheory{
	
	private List<DataValue<Integer>> sumConstants;
	private List<DataValue<Integer>> regularConstants;

	public SumCIntegerInequalityTheory() {
		super();
	}
	
	public void setSumcConstants(SumConstants constants) {
		this.sumConstants = new ArrayList<>(constants.values(this.type));
	}
	
	public void setConstants(Constants constants) {
		this.regularConstants = new ArrayList<>(constants.values(this.type));
		this.sumConstants = constants.getSumCs(this.type);
	}
	

    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
    	List<DataValue<Integer>> valsWithConsts = new ArrayList<>(vals);
    	valsWithConsts.addAll(this.regularConstants);
    	
    	// we add regular constants
    	DataValue<Integer> fv = super.getFreshValue(valsWithConsts);
    	DataValue<Integer> maxSumC = this.sumConstants.isEmpty()? new DataValue<Integer>(this.getType(), 1) : 
    		Collections.max(this.sumConstants, new Cpr());
    	return new DataValue<Integer>(fv.getType(), fv.getId() + maxSumC.getId() * 100); // * 100);
    }
	
	public SumCIntegerInequalityTheory(DataType<Integer> dataType) {
		// the constants have to be introduced manually
		this(dataType, Collections.emptyList(), Collections.emptyList());
	}
	
	public SumCIntegerInequalityTheory(DataType<Integer> dataType, List<DataValue<Integer>> sumConstants, List<DataValue<Integer>> regularConstants) {
		super(dataType);
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
	}
	
	
	
    public ValueMapper<Integer> getValueMapper() {
    	return new SumCInequalityValueMapper<Integer>(this, this.sumConstants);
    }
	
    public Collection<DataValue<Integer>> getAllNextValues(
            List<DataValue<Integer>> vals) {
    	// adds sumc constants to interesting values
    	List<DataValue<Integer>> potential = getPotential(vals);
    	
    	// the superclass should complete this list with in-between values.
    	return super.getAllNextValues(potential);
    }

    
    public List<DataValue<Integer>> getPotential(List<DataValue<Integer>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<Integer>> sortedList = makeNewPotsWithSumC(dvs);
        //sortedList.addAll(dvs);
        Collections.sort(sortedList, new Cpr());

        //System.out.println("I'm sorted!  " + sortedList.toString());
        return sortedList;
    }
    
    /** Creates a list of values comprising the data values supplied, plus all values
     * obtained by adding each of the sum constants to each of the data values supplied.
     * 
     *  The sum values are wrapped around a {@link SumCDataValue} element. In case sums
     *  with different constants lead to the same value (for example 100+1.0 and 1+100.0 with
     *  1.0 and 100.0 as constants), we pick the sum with the constant of the smallest index
     *  in the sumConstants list.  
     * 
     */
    private List<DataValue<Integer>> makeNewPotsWithSumC(List<DataValue<Integer>> dvs) {
    	List<DataValue<Integer>> pot = new ArrayList<DataValue<Integer>> (dvs.size() * (sumConstants.size()+1));
    	pot.addAll(dvs);
    	List<DataValue<Integer>> dvWithoutConsts = dvs.stream().filter(dv -> !regularConstants.contains(dv)).collect(Collectors.toList());
    	// potential optimization, don't make sums out of sumC
    	//dvWithoutConsts = dvWithoutConsts.stream().filter(dv -> dv.getId() < 100).collect(Collectors.toList()); // ignore sumc constants
    	List<DataValue<Integer>> flattenedPot = new ArrayList<DataValue<Integer>> (dvs.size() * (sumConstants.size()+1));
    	flattenedPot.addAll(pot);
    	for (DataValue<Integer> sumConst : sumConstants) {
	    	for (DataValue<Integer> dv : dvWithoutConsts.stream().filter(pdv -> pdv.getType().equals(sumConst.getType())).collect(Collectors.toList()) ) {
	    		DataValue<Integer> regularSum = (DataValue<Integer>) DataValue.add(dv, sumConst);
	    		if ( !flattenedPot.contains(regularSum)) {
	    			SumCDataValue<Integer> sumDv = new SumCDataValue<Integer>(dv, sumConst);
	    			pot.add(sumDv);
	    		}
	    		flattenedPot.add(regularSum);
	    	}
    	}
    	
    	
    	return pot;
    }
    
    
	@Override
	public List<EnumSet<DataRelation>> getRelations(List<DataValue<Integer>> left, DataValue<Integer> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		LOOP: for (DataValue<Integer> dv : left) {
			if (!this.sumConstants.isEmpty()) {
				for (int ind = 0; ind < this.sumConstants.size(); ind++)
					if (Integer.valueOf((this.sumConstants.get(ind).getId() + dv.getId()))
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
