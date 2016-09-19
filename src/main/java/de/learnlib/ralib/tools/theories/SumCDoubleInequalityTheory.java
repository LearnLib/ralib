package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory{
	// default constants
	private static Double [] defaultSumConst = new Double [] {
			100.0
			,10000.0
			};
	private static Double [] defaultRegularConst = new Double [] {
			//1.0
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
	
	public SumCDoubleInequalityTheory(DataType dataType, List<DataValue<Double>> sumConstants, List<DataValue<Double>> regularConstants) {
		super(dataType);
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
	}
	
	public void setType(DataType dataType) {
		super.setType(dataType);
		setupDefaultConstants(dataType);
	}
	
	private void setupDefaultConstants(DataType dataType) {
		this.sumConstants = Arrays.asList(defaultSumConst).stream().map(c -> new DataValue<Double>(dataType, c)).collect(Collectors.toList());
		this.regularConstants = Arrays.asList(defaultRegularConst).stream().map(c -> new DataValue<Double>(dataType, c)).collect(Collectors.toList());
	}
	
    public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<Double>> sortedList = makeNewPotsWithSumC(dvs);
        
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
    private List<DataValue<Double>> makeNewPotsWithSumC(List<DataValue<Double>> dvs) {
    	List<DataValue<Double>> pot = new ArrayList<DataValue<Double>> (dvs.size() * (sumConstants.size()+1));
    	dvs.forEach(dv -> { if (!regularConstants.contains(dv)) {
    		pot.add(dv);
    	} });
    	
    	List<DataValue<Double>> flattenedPot = new ArrayList<DataValue<Double>> (dvs.size() * (sumConstants.size()+1));
    	flattenedPot.addAll(pot);
    	for (DataValue<Double> sumConst : sumConstants) {
	    	for (DataValue<Double> dv : dvs ) {
	    		DataValue<Double> regularSum = (DataValue<Double>) DataValue.add(dv, sumConst);
	    		if ( !flattenedPot.contains(regularSum) ) {
	    			SumCDataValue<Double> sumDv = new SumCDataValue<Double>(dv, sumConst);
	    			pot.add(sumDv);
	    		}
	    		flattenedPot.add(regularSum);
	    	}
    	}
    	
    	
    	return pot;
    }
}
