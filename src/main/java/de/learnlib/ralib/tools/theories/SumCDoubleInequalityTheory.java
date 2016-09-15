package de.learnlib.ralib.tools.theories;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    	List<DataValue<Double>> listOfValues = dvs.stream().filter(potVal -> !regularConstants.contains(potVal)).  // we filter the actual constants (we don't want sums over constants)
    			flatMap(value ->
    			// apply each of the sum constants to the potential, plus the initial value
    			Stream.of(Stream.of(value), sumConstants.stream().map(c -> addNumbers(value, c, dvs) )).flatMap(Function.identity())). 
    			distinct(). // remove any duplicates
    			collect(Collectors.toList()); // collect them to a list
    	for (DataValue<Double> val : dvs) { 
    		if (!listOfValues.contains(val)) {
    			listOfValues.add(val);
    		}
    	}
    	
    	return listOfValues;
    }

	private DataValue<Double> addNumbers(DataValue<Double> value, DataValue<Double> c, List<DataValue<Double>> dvs) {
		SumCDataValue<Double> sumC = new SumCDataValue<Double>(value, c);
		if (dvs.contains(sumC.toRegular())) {
			return value;
		}
		return sumC;
	}

}
