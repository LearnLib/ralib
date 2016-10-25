package de.learnlib.ralib.tools.theories;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * Fixes a symbolic data trace produced after reduction techniques. This could be merged with the ValueMapper / canonizer
 * (in the end, it's a type of canonizing from DataValue to DataValue). 
 */
public class TraceCanonizer {
	
	private Map<DataType, Theory> theories;

	final Map<DataType, ValueMapper> valueMappers = new LinkedHashMap<>();
	private Map<DataType, Map<DataValue, DataValue>> buckets;

	public TraceCanonizer(Map<DataType, Theory> theories) {
		this.theories = theories;
		this.theories.forEach( (dt, th) -> valueMappers.put(dt, new TraceFixerMapper(th)));
	}
	
	/**
	 * Makes a symbolic trace canonical by replacing any of the data values with missing operands/interval ends
	 * by fresh values. It works similarly to a ValueCanonizer, with the difference that mapping is from symbolic
	 * to symbolic (instead of to concrete).
	 * 
	 * Example:
	 * before: FV 10  INTV 15 (10 20) SUMC 15 1
	 * (data value 20 is missing, so INTV 15 has a missing endpoint, thus is turned into a fresh value. SUMC is now
	 * connected to this fresh value)
	 * after: FV 10 FV 20 SUMC 20 1 
	 */
	public Word<PSymbolInstance> fixTrace(Word<PSymbolInstance> trace) {
		ValueCanonizer canonizer = new ValueCanonizer(valueMappers);

		try {
		Word<PSymbolInstance> canonicalTrace = canonizer.canonize(trace, false);
	
		
		return canonicalTrace;
		}catch(Exception e) {
			System.exit(0);
			return null;
		}
	}
	
	static class TraceFixerMapper<T extends Comparable<T>> implements ValueMapper<T> {
		
		private Theory<T> theory;

		public TraceFixerMapper(Theory<T> theory) {
			this.theory = theory;
		}

		public DataValue<T> canonize(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap) {
			if (thisToOtherMap.containsKey(value)) {
				return new DataValue<T>( value.getType(), value.getId());
			}
			if (value instanceof SumCDataValue) {
				SumCDataValue<T> sumc = (SumCDataValue<T>) value;
				if (thisToOtherMap.containsKey(sumc.getOperand())) {
					return  new SumCDataValue<T>(thisToOtherMap.get(sumc.getOperand()), sumc.getConstant());
				}
			}
			
			if (value instanceof IntervalDataValue) {
				IntervalDataValue<T> intv = (IntervalDataValue<T>) value;
				DataValue<T> newLeft = null;
				if (intv.getLeft() != null) {
					newLeft = canonize(intv.getLeft(), thisToOtherMap);
				}
				
				DataValue<T> newRight = null;
				if (intv.getRight() != null) {
					newRight = canonize(intv.getRight(), thisToOtherMap);
				}
				
				// if either the ends are fresh it means their value hasn't been found
				if ((newLeft == null || !(newLeft instanceof FreshValue)) && (newRight == null || !(newRight instanceof FreshValue))) {
					return IntervalDataValue.instantiateNew(newLeft, newRight);
				} 
			}
			
			// ideally, we would only instantiate fresh if the value is an instance of a fresh value
			
			List<DataValue<T>> castList = thisToOtherMap.values().stream().map(t -> ((DataValue<T>) t)).collect(Collectors.toList());
			DataValue<T> fv = this.theory.getFreshValue(castList);
			
			return new FreshValue<T>(fv.getType(), fv.getId());
		}


		public DataValue<T> decanonize(DataValue<T> value,
				Map<DataValue<T>, DataValue<T>> thisToOtherMap) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
