package de.learnlib.ralib.tools.theories;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory{
	
	private List<DataValue<Double>> sumConstants;
	private List<DataValue<Double>> regularConstants;

	public SumCDoubleInequalityTheory(DataType dataType) {
		this(dataType, Collections.emptyList(), Collections.emptyList());
	}
	
	public SumCDoubleInequalityTheory(DataType dataType, List<DataValue<Double>> sumConstants, List<DataValue<Double>> regularConstants) {
		super(dataType);
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
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
    	List<DataValue<Double>> sortedList = dvs.stream().filter(potVal -> !regularConstants.contains(potVal)).  // we filter the actual constants (we don't want sums over constants)
    			flatMap(value -> sumConstants.stream().map(c -> addNumbers(value, c))). // apply each of the sum constants to the potential
    			distinct(). // remove any duplicates
    			collect(Collectors.toList()); // collect them to a list
    	        sortedList.addAll(dvs); // add the initial data values
    	        return sortedList;
    }

	private DataValue<Double> addNumbers(DataValue<Double> value, DataValue<Double> c) {
		return new SumCDataValue<Double>(value, c);
	}

}
