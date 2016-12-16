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

public class SumCLongInequalityTheory extends LongInequalityTheory{
	// default constants
	private static Long [] defaultSumConst = new Long [] {
			 Long.valueOf(0),
			Long.valueOf(1000)
			};
	private static Long [] defaultRegularConst = new Long [] {
			Long.valueOf(0)
			};
	
	
	private List<DataValue<Long>> sumConstants;
	private List<DataValue<Long>> regularConstants;

	public SumCLongInequalityTheory() {
		super();
	}
	
	public SumCLongInequalityTheory(DataType dataType) {
		// the constants have to be introduced manually
		super(dataType);
		setupDefaultConstants(dataType);
	}
	
	public SumCLongInequalityTheory(DataType dataType, List<DataValue<Long>> sumConstants, List<DataValue<Long>> regularConstants) {
		super(dataType);
		this.sumConstants = sumConstants;
		this.regularConstants = regularConstants;
	}
	
	public void setType(DataType dataType) {
		super.setType(dataType);
		setupDefaultConstants(dataType);
	}
	
	private void setupDefaultConstants(DataType<Long> dataType) {
		this.sumConstants = Arrays.asList(defaultSumConst).stream()
				.map(c -> new DataValue<Long>(dataType, c))
				.collect(Collectors.toList());
		this.regularConstants = Arrays.asList(defaultRegularConst).stream()
				.map(c -> new DataValue<Long>(dataType, c))
				.collect(Collectors.toList());
	}
	
    public List<DataValue<Long>> getPotential(List<DataValue<Long>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<Long>> sortedList = makeNewPotsWithSumC(dvs);
        
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
    private List<DataValue<Long>> makeNewPotsWithSumC(List<DataValue<Long>> dvs) {
    	List<DataValue<Long>> pot = new ArrayList<DataValue<Long>> (dvs.size() * (sumConstants.size()+1));
    	pot.addAll(dvs);
    	List<DataValue<Long>> dvWithoutConsts = dvs.stream()
    			.filter(dv -> !regularConstants.contains(dv)).map(dv -> {
    				if (dv instanceof SumCDataValue) return ((SumCDataValue<Long>)dv).toRegular(); 
    				return dv;
    			})
    			.collect(Collectors.toList());
    	// potential optimization, don't make sums out of sumC
    	// dvWithoutConsts = dvWithoutConsts.stream().filter(dv -> dv.getId() < 100.0).collect(Collectors.toList()); // ignore sumc constants
    	List<DataValue<Long>> flattenedPot = new ArrayList<DataValue<Long>> (dvs.size() * (sumConstants.size()+1));
    	flattenedPot.addAll(pot);
    	for (DataValue<Long> sumConst : sumConstants) {
	    	for (DataValue<Long> dv : dvWithoutConsts.stream().filter(pdv -> pdv.getType().equals(sumConst.getType())).collect(Collectors.toList()) ) {
	    		DataValue<Long> regularSum = (DataValue<Long>) DataValue.add(dv, sumConst);
	    		if ( !flattenedPot.contains(regularSum)) {
	    			SumCDataValue<Long> sumDv = new SumCDataValue<Long>(dv, sumConst);
	    			pot.add(sumDv);
	    		}
	    		flattenedPot.add(regularSum);
	    	}
    	}
    	
    	
    	return pot;
    }
    
    public DataValue<Long> getFreshValue(List<DataValue<Long>> vals) {
    	List<DataValue<Long>> valsWithConsts = new ArrayList<>(vals);
    	valsWithConsts.addAll(this.regularConstants);
    	
    	// we add regular constants
    	DataValue<Long> fv = super.getFreshValue(valsWithConsts);
    	DataValue<Long> maxSumC = this.sumConstants.isEmpty()? new DataValue<Long>(this.getType(), Long.valueOf(1)) : 
    		Collections.max(this.sumConstants, new Cpr());
    	return new DataValue<Long>(fv.getType(), fv.getId() + maxSumC.getId() * 100);
    }
    
    public ValueMapper<Long> getValueMapper() {
    	return new SumCInequalityValueMapper<Long>(this, this.sumConstants);
    }
    
    
    
    public Collection<DataValue<Long>> getAllNextValues(
            List<DataValue<Long>> vals) {
    	// adds sumc constants to interesting values
    	List<DataValue<Long>> potential = getPotential(vals);
    	
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
	            List<DataValue<Long>> left, DataValue<Long> right) {
	        
	        List<EnumSet<DataRelation>> ret = new ArrayList<>();
	        LOOP: for (DataValue<Long> dv : left) {
	        	if (!this.sumConstants.isEmpty()) {
	        		for (int ind=0; ind < this.sumConstants.size(); ind++) 
	        			if(	Long.valueOf((this.sumConstants.get(ind).getId() + dv.getId())).compareTo(right.getId()) == 0) {
	        				switch (ind) {
	        					case 0: ret.add(
	        							EnumSet.of(DataRelation.EQ_SUMC1
	        									, DataRelation.DEQ_SUMC1
	        									)); break;
	        					case 1: ret.add(EnumSet.of(DataRelation.EQ_SUMC2
	        							, DataRelation.DEQ_SUMC2
	        							)); break;
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
