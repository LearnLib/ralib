package de.learnlib.ralib.sul;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;

/**
 * Maps data values (typed values) to values of the same type by maintaining a mapping from chaotic (non-canonized) values
 * to canonized values for each data type. New non-canonized values received are added to the map, paired with an 
 * analogous canonized value.  
 * 
 * The analogy is induced by the relation a value has with its respective set. If no relation is found, the value is deemed fresh,
 * thus it will be paired with a fresh value from the other set. 
 * <br/>
 * Example for equality: <br/>
 * (inp) [0, 1] => {0:0, 1:1}   0,1 fresh <br/>
 * (out) [1, 234] => {0:0, 234:2} 	1 not fresh, 234 fresh <br/>
 * Example for equ/incr: <br/>
 * (inp) [0,1, 100] => {0:0, 1:1, 100:20} <br/>
 * (out) [2, 101, 68878] => {0:0, 1:1, 100:20, 101:21, 68878:40} <br/>
 * <p/>
 * We assume that analogies are not coincidental and never check them. Attention must be paid to the case when i 
 */

public class TypedValueCanonizer {
	
	
	private final Map<DataType, BiMap<Object, Object>> buckets = new HashMap<>(); // from decanonized to canonized for each type
	
	private final Map<DataType, ValueMatcher> valueMatchers; // value matchers are used to find pairings for each key supplied 
	
	public TypedValueCanonizer( Map<DataType, ValueMatcher> valueMatchers) {
		this.valueMatchers = valueMatchers;
	}
	
	public DataValue[] decanonize(DataValue[] dvs ) {
	     DataValue[] undetInVal = new DataValue[dvs.length];
	     int i = 0;
	     for (DataValue detVal : dvs) {
	         undetInVal[i++] = new DataValue( detVal.getType(), resolveValue(dvs[i], true));
	     }
	     return undetInVal;
	     
	}
	
	public DataValue[] canonize(DataValue [] dvs, Map<DataValue, FreshValue> freshValueMap) {
       DataValue [] detOutVals = new DataValue [dvs.length];
       int i = 0;
       for (DataValue outVal : dvs) {
       		detOutVals[i++] = new DataValue(outVal.getType(), resolveValue(outVal, false));
       }
       return detOutVals;
	}
	
	
	private DataValue resolveValue(DataValue dv, boolean inverse) {
		BiMap map = this.buckets.get(dv.getType());
        if (map == null) {
            map = HashBiMap.create();
            this.buckets.put(dv.getType(), map);
        }
        if (inverse) 
        	map = map.inverse();
        
        ValueMatcher valueMatcher = valueMatchers.get(dv.getType());
        DataValue resultDv; 
        
        if (!map.containsKey(dv.getId())) {
        	if (valueMatcher.isValueFresh(dv.getId(), map.keySet())) {
	        	Object freshValue = valueMatcher.getFreshValue(map.keySet());
	        	resultDv = new FreshValue(dv.getType(), freshValue);
        	} else {
        		Object relatedValue = valueMatcher.getAnalogousValue(dv.getId(), map);
        		resultDv = new DataValue(dv.getType(), relatedValue);
        	}
        	map.put(dv.getId(), resultDv.getId());
        	
        } else {
        	Object valueInMap = map.get(dv.getId());
        	resultDv = new DataValue(dv.getType(), valueInMap);
        }
        
        return resultDv;
	}


}
