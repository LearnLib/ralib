package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCIntegerInequalityTheory extends IntegerInequalityTheory{
	// default constants
	private static Integer [] defaultSumConst = new Integer [] {
			1,
			100
			//10000.0
			};
	private static Integer [] defaultRegularConst = new Integer [] {
		//	0.0
			};
	
	
	private List<DataValue<Integer>> sumConstants;
	private List<DataValue<Integer>> regularConstants;

	public SumCIntegerInequalityTheory() {
		super();
	}
	
	public SumCIntegerInequalityTheory(DataType dataType) {
		// the constants have to be introduced manually
		super(dataType);
		setupDefaultConstants(dataType);
	}
	
	public SumCIntegerInequalityTheory(DataType dataType, List<DataValue<Integer>> sumConstants, List<DataValue<Integer>> regularConstants) {
		super(dataType);
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
	}
	
	public void setType(DataType dataType) {
		super.setType(dataType);
		setupDefaultConstants(dataType);
	}
	
	private void setupDefaultConstants(DataType dataType) {
		this.sumConstants = Arrays.asList(defaultSumConst).stream().map(c -> new DataValue<Integer>(dataType, c)).collect(Collectors.toList());
		this.regularConstants = Arrays.asList(defaultRegularConst).stream().map(c -> new DataValue<Integer>(dataType, c)).collect(Collectors.toList());
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
    	dvWithoutConsts = dvWithoutConsts.stream().filter(dv -> dv.getId() < 100.0).collect(Collectors.toList()); // ignore sumc constants
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
    
    
    public Collection<DataValue<Integer>> getAllNextValues(
            List<DataValue<Integer>> vals) {
    	// adds window size interesting values
    	List<DataValue<Integer>> potential = getPotential(vals);
    	potential = potential.stream().map(dv -> 
    	dv instanceof SumCDataValue? ((SumCDataValue<Integer>) dv).toRegular(): dv)
    			.collect(Collectors.toList());
    	// the superclass should complete this list with in-between values.
    	return super.getAllNextValues(potential);
    }

}
