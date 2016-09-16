package de.learnlib.ralib.tools.theories;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory{
	// default constants
	private static Double [] defaultSumConst = new Double [] {
			100.0
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
    
    private List<DataValue<Double>> makeNewPotsWithSumC(List<DataValue<Double>> dvs) {
    	List<DataValue<Double>> listOfValues = dvs.stream().
    			flatMap(value ->
    			// apply each of the sum constants to each of the potential values, 
    			Stream.of(Stream.of(value), sumConstants.stream().map(c -> applySumConstant(value, c, dvs) )).flatMap(Function.identity())). 
    			distinct(). // remove any duplicates 
    			collect(Collectors.toList()); // collect them to a list
    	
    	listOfValues.addAll(0, regularConstants);
    	return listOfValues;
    }

	private DataValue<Double> applySumConstant(DataValue<Double> value, DataValue<Double> c, List<DataValue<Double>> dvs) {
		if (this.regularConstants.contains(value)) {
			return value; // we cannot apply sumC over existing constants
		}
		
		SumCDataValue<Double> sumC = new SumCDataValue<Double>(value, c);
		// if the sum is already contained in the list we need not add it
		if (dvs.contains(sumC.toRegular())) {
			return value;
		}
		return sumC;
	}

}
