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

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory{
	// default constants
	private static Double [] defaultSumConst = new Double [] {
			 1.0,
			1000.0
			//10000.0
			};
	private static Double [] defaultRegularConst = new Double [] {
		//	0.0
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
		this.sumConstants = Arrays.asList(defaultSumConst).stream()
				.map(c -> new DataValue<Double>(dataType, c))
				.collect(Collectors.toList());
		this.regularConstants = Arrays.asList(defaultRegularConst).stream()
				.map(c -> new DataValue<Double>(dataType, c))
				.collect(Collectors.toList());
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
    	pot.addAll(dvs);
    	List<DataValue<Double>> dvWithoutConsts = dvs.stream()
    			.filter(dv -> !regularConstants.contains(dv)).map(dv -> {
    				if (dv instanceof SumCDataValue) return ((SumCDataValue<Double>)dv).toRegular(); 
    				return dv;
    			})
    			.collect(Collectors.toList());
    	// potential optimization, don't make sums out of sumC
    	// dvWithoutConsts = dvWithoutConsts.stream().filter(dv -> dv.getId() < 100.0).collect(Collectors.toList()); // ignore sumc constants
    	List<DataValue<Double>> flattenedPot = new ArrayList<DataValue<Double>> (dvs.size() * (sumConstants.size()+1));
    	flattenedPot.addAll(pot);
    	for (DataValue<Double> sumConst : sumConstants) {
	    	for (DataValue<Double> dv : dvWithoutConsts.stream().filter(pdv -> pdv.getType().equals(sumConst.getType())).collect(Collectors.toList()) ) {
	    		DataValue<Double> regularSum = (DataValue<Double>) DataValue.add(dv, sumConst);
	    		if ( !flattenedPot.contains(regularSum)) {
	    			SumCDataValue<Double> sumDv = new SumCDataValue<Double>(dv, sumConst);
	    			pot.add(sumDv);
	    		}
	    		flattenedPot.add(regularSum);
	    	}
    	}
    	
    	
    	return pot;
    }
    
    public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
    	List<DataValue<Double>> valsWithConsts = new ArrayList<>(vals);
    	valsWithConsts.addAll(this.regularConstants);
    	
    	// we add regular constants
    	DataValue<Double> fv = super.getFreshValue(valsWithConsts);
    	DataValue<Double> maxSumC = this.sumConstants.isEmpty()? new DataValue<Double>(this.getType(), 1.0) : 
    		Collections.max(this.sumConstants, new Cpr());
    	return new DataValue<Double>(fv.getType(), fv.getId() + maxSumC.getId() * 100);
    }
    
    public ValueMapper<Double> getValueMapper() {
    	return new SumCInequalityValueMapper<Double>(this, this.sumConstants);
    }
    
    
    
    public Collection<DataValue<Double>> getAllNextValues(
            List<DataValue<Double>> vals) {
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
	    public List<EnumSet<DataRelation>> getRelations(
	            List<DataValue<Double>> left, DataValue<Double> right) {
	        
	        List<EnumSet<DataRelation>> ret = new ArrayList<>();
	        LOOP: for (DataValue<Double> dv : left) {
	        	if (!this.sumConstants.isEmpty()) {
	        		for (int ind=0; ind < this.sumConstants.size(); ind++) 
	        			if(	Double.valueOf((this.sumConstants.get(ind).getId() + dv.getId())).compareTo(right.getId()) == 0) {
	        				switch (ind) {
	        					case 0: ret.add(EnumSet.of(DataRelation.EQ_SUMC1)); break;
	        					case 1: ret.add(EnumSet.of(DataRelation.EQ_SUMC2)); break;
	        					default: 
	        						throw new DecoratedRuntimeException("Over 2 sumcs not supported");
	        				}
	        				continue LOOP;
	        			} 
	        	}

	        	final int c = dv.getId().compareTo(right.getId());
	        	if (c > 0)
	        		ret.add(EnumSet.of(DataRelation.GT));
	        	else if (c == 0)
	        		ret.add(EnumSet.of(DataRelation.EQ));
	        	else 
	        		ret.add(EnumSet.of(DataRelation.LT));
	        }
	        
	        return ret;
	    }
}
