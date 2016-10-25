package de.learnlib.ralib.sul;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

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
 * We assume that analogies are not coincidental and never check them. 
 */

public class ValueCanonizer {
	
	
	private final Map<DataType, BiMap<DataValue, DataValue>> buckets = new HashMap<>(); // from decanonized to canonized for each type
	
	private final Map<DataType, ValueMapper> valueMappers; // value matchers are used to find pairings for each key supplied 
	
	public static ValueCanonizer buildNew(Map<DataType, Theory> theories) {
		LinkedHashMap<DataType, ValueMapper> valueMappers = new LinkedHashMap<DataType, ValueMapper>();
		theories.forEach((dt, th) ->  {
			if ( th.getValueMapper() != null) {
				valueMappers.put(dt, th.getValueMapper());
			}
		});
		
		return new ValueCanonizer(valueMappers);
	}
	
	public ValueCanonizer( Map<DataType, ValueMapper> valueMappers) {
		this.valueMappers = valueMappers;
	}
	
//	public ValueCanonizer( Map<DataType, ValueMapper> valueMatchers) {
//		this.valueMatchers = valueMatchers;
//	}
	
	public Word<PSymbolInstance> canonize(Word<PSymbolInstance> trace, boolean inverse) {
		Word<PSymbolInstance> newTrace = Word.epsilon();
		for (PSymbolInstance sym : trace) {
			PSymbolInstance canSym = this.canonize(sym, inverse);
			newTrace = newTrace.append(canSym);
		}
		return newTrace;
//		return trace.transform(sym -> this.canonize(sym, inverse)); // switched away because it is hard to run a debugger
	}
	
	public PSymbolInstance canonize(PSymbolInstance symbol, boolean inverse) {
		DataValue[] canonized = canonize(symbol.getParameterValues(), inverse);
		return new PSymbolInstance(symbol.getBaseSymbol(), canonized);
	}
	
	/**
	 * If inverse is false, acts as a canonizer, that is each of the de-canonized values passed along, returns
	 * an analogous canonized values. If inverse is true, then for each canonized value returns a de-canonized value.
	 * 
	 * A mapping from canonized to de-canonized is updated for every value.
	 */
	public DataValue [] canonize(DataValue [] dvs, boolean inverse) {
		
		DataValue [] resultDvs = new DataValue [dvs.length];
		for (int i = 0; i < dvs.length; i ++) {
			resultDvs[i] = 
					this.valueMappers.containsKey(dvs[i].getType()) ? 
					inverse ? decanonize(dvs[i]) : canonize(dvs[i]) : dvs[i]; 
		}
//				
//				Stream.of(dvs).map(dv -> 
//		this.valueMatchers.containsKey(dv.getType()) ? inverse ? decanonize(dv) : canonize(dv) : dv).toArray(DataValue []::new);
	    return resultDvs;
	}
		
	
	private DataValue  canonize(DataValue dv) {
		BiMap<DataValue, DataValue> map = getOrCreateBucket(dv);
        BiMap<DataValue, DataValue> inverseMap = map.inverse();
        
        ValueMapper valueMatcher = valueMappers.get(dv.getType());
        DataValue resultDv = valueMatcher.canonize(dv, inverseMap);
        
        if (!inverseMap.containsKey(dv)) {
        	inverseMap.put(dv, resultDv);
        }
        
        return resultDv;
	}
	
	private DataValue  decanonize(DataValue dv) {
		BiMap<DataValue, DataValue> map = getOrCreateBucket(dv);
        
        ValueMapper valueMatcher = valueMappers.get(dv.getType());
        DataValue resultDv = valueMatcher.decanonize(dv, map); 
        
        if (!map.containsKey(dv)) {
        	map.put(dv, resultDv);
        } 
        
        return resultDv;
	}

	private BiMap<DataValue, DataValue> getOrCreateBucket(DataValue dv) {
		BiMap<DataValue, DataValue> map = this.buckets.get(dv.getType());
        if (map == null) {
            map = HashBiMap.create();
            this.buckets.put(dv.getType(), map);
        }
        return map;
	}
}
