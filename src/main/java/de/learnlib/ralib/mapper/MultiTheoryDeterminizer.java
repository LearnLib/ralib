package de.learnlib.ralib.mapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * Maps data values (typed values) to values of the same type by maintaining a mapping from system (non-canonized) values to canonized values for each data type. 
 * Non-canonized values received are added to the map, paired with an analogous canonized value.  
 * 
 * The analogy is inferred by the relation a value has with its respective set. If no relation is found, the value is deemed fresh,
 * thus it will be paired with a fresh value from the other set. For the purpose of matching a value with its analogous, we use Theory-specific Determinizers. 
 *  
 * <br/>
 * Example for equality: <br/>
 * (inp) [0, 1] => {0:0, 1:1}   0,1 fresh <br/>
 * (out) [1, 234] => {0:0, 234:2} 	1 not fresh, 234 fresh <br/>
 * Example for equ/incr: <br/>
 * (inp) [0,1, 100] => {0:0, 1:1, 100:20} <br/>
 * (out) [2, 101, 68878] => {0:0, 1:1, 100:20, 101:21, 68878:40} <br/>
 * <p/>
 * We assume that analogies are not coincidental. 
 */

public class MultiTheoryDeterminizer {
	
	
	private final Map<DataType, BiMap<DataValue, DataValue>> buckets = new HashMap<>(); // from decanonized to canonized for each type
	private final Map<DataType, Determinizer> determinizers; // determinizers used to find pairings for each key supplied 

	private Constants constants;
	
	/**
	 * Constructs and MT Determizer using a custom range of determinizers. 
	 */
	public static MultiTheoryDeterminizer newCustom(Map<DataType, Determinizer> determinizers, Constants constants) {
		MultiTheoryDeterminizer mtDeterminizer = new MultiTheoryDeterminizer(constants);
		mtDeterminizer.determinizers.putAll(determinizers);
		return mtDeterminizer;
	}
	
	private MultiTheoryDeterminizer(Constants constants) {
		this.determinizers = new LinkedHashMap<DataType, Determinizer>();
		this.constants = constants;
	}

	/**
	 * C
	 */
	public MultiTheoryDeterminizer(Map<DataType, Theory> theories, Constants constants) {
		this.determinizers = new LinkedHashMap<DataType, Determinizer>();
		theories.forEach((dt, th) ->  {
			if ( th.getDeterminizer() != null) {
				determinizers.put(dt, th.getDeterminizer());
			}
		});
		this.constants = constants;
	}
	
	public Word<PSymbolInstance> canonize(Word<PSymbolInstance> trace, boolean inverse) {
		Word<PSymbolInstance> newTrace = Word.epsilon();
		for (PSymbolInstance sym : trace) {
			PSymbolInstance canSym = this.canonize(sym, inverse);
			newTrace = newTrace.append(canSym);
		}

		return newTrace;
	}
	
	
	
	public PSymbolInstance canonize(PSymbolInstance symbol, boolean inverse) {
		DataValue[] canonized = canonize(symbol.getParameterValues(), inverse);
		return new PSymbolInstance(symbol.getBaseSymbol(), canonized);
	}
	
	/**
	 * If reverse is false, acts as a canonizer, that is each of the de-canonized values passed along, returns
	 * an analogous canonized values. If inverse is true, then for each canonized value returns a de-canonized value.
	 * 
	 * A mapping from canonized to de-canonized is updated for every value.
	 */
	public DataValue [] canonize(DataValue [] dvs, boolean reverse) {
		int i = 0;
		DataValue [] resultDvs = new DataValue [dvs.length];
		try {
			for (i = 0; i < dvs.length; i ++) {
				resultDvs[i] = 
						determinizers.containsKey(dvs[i].getType()) ? 
						reverse ? decanonize(dvs[i]) : canonize(dvs[i]) : dvs[i]; 
			}
		} 
		catch(Exception exception) {
			DecoratedRuntimeException exc;
			if (exception instanceof DecoratedRuntimeException) {
				exc = (DecoratedRuntimeException) exception;
			} else {
				exc =  new DecoratedRuntimeException(exception.getMessage()).addSuppressedExc(exception);
			}
			exc.addDecoration("method", (reverse?"de":"") + "canonize ").
			addDecoration("processed value", dvs[i]).
			addDecoration("state", stateString());
			throw exc;
		}
	    return resultDvs;
	}
	
	
	private DataValue  canonize(DataValue dv) {
		BiMap<DataValue, DataValue> map = getOrCreateBucket(dv);
        BiMap<DataValue, DataValue> inverseMap = map.inverse();
        
        Determinizer valueMatcher = determinizers.get(dv.getType());
        DataValue resultDv = valueMatcher.canonize(dv, inverseMap, constants);
        
        if (!inverseMap.containsKey(dv)) {
        	inverseMap.put(dv, resultDv);
        }
        
        return resultDv;
	}
	
	private DataValue  decanonize(DataValue dv) {
		BiMap<DataValue, DataValue> map = getOrCreateBucket(dv);
        
        Determinizer valueMatcher = determinizers.get(dv.getType());
        DataValue resultDv = valueMatcher.decanonize(dv, map, constants); 
        
        if (!map.containsKey(dv)) {
        	map.put(dv, resultDv);
        } 
        
        return resultDv;
	}

	private String stateString() {
		StringBuilder b = new StringBuilder();
		for (DataType type : this.buckets.keySet()) {
			b.append("\n ").append(type.toString());
			BiMap<DataValue, DataValue> bucket = this.buckets.get(type);
			TreeMap<DataValue, DataValue> sortedMap = new TreeMap<DataValue, DataValue> ((dv1,dv2) -> {
				if (Comparable.class.isAssignableFrom(dv1.getType().getBase())) 
					return ((Comparable)dv1.getId()).compareTo((Comparable)dv2.getId());
				else 
					return dv1.getId().toString().compareTo(dv2.getId().toString());
			});
			sortedMap.putAll(bucket);
			sortedMap.forEach((cdv, ddv) -> b.append("\n 	").append(cdv).append(":").append(ddv));
		}
		return b.toString();
	}
	

	private BiMap<DataValue, DataValue> getOrCreateBucket(DataValue dv) {
		BiMap<DataValue, DataValue> map = buckets.get(dv.getType());
        if (map == null) {
            map = HashBiMap.create();
            buckets.put(dv.getType(), map);
        }
        return map;
	}
	
}
