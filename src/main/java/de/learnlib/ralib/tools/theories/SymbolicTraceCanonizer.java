package de.learnlib.ralib.tools.theories;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * Canonizes a symbolic data trace produced after reduction techniques. This could be merged with the ValueMapper / canonizer
 * (in the end, it's a type of canonizing from DataValue to DataValue). 
 */
public class SymbolicTraceCanonizer implements TraceCanonizer{
	
	final Map<DataType, ValueMapper> valueMappers = new LinkedHashMap<>();
	private Constants constants;
	
	public SymbolicTraceCanonizer(Map<DataType, Theory> theories, Constants constants) {
		theories.forEach( (dt, th) -> valueMappers.put(dt, new SymbolicTraceValueMapper(th, dt)));
		this.constants = constants;
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
	public Word<PSymbolInstance> canonizeTrace(Word<PSymbolInstance> trace) {
		ValueCanonizer canonizer = new ValueCanonizer(valueMappers, constants);

		try {
			Word<PSymbolInstance> canonicalTrace = canonizer.canonize(trace, false);  
		
			
			return canonicalTrace;
		}catch(DecoratedRuntimeException e) {
			e.addDecoration("trace to be canonized", trace);
			throw e;
		}
	}
	

	/**
	 * A value mapper which assumes that the given trace is decorated with (semi-) symbolic information. 
	 */
	static class SymbolicTraceValueMapper<T extends Comparable<T>> implements ValueMapper<T> {
		
		private Theory<T> theory;
		private DataType<T> type;

		public SymbolicTraceValueMapper(Theory<T> theory, DataType<T> type) {
			this.theory = theory;
			this.type = type;
		}

		public DataValue<T> canonize(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
			if (thisToOtherMap.containsKey(value)) {
				DataValue<T> mapping = thisToOtherMap.get(value);
				return new DataValue<T>( mapping.getType(), mapping.getId());
			}
			if (constants.containsValue(value)) 
				return value;
			DataValue<T> mappedValue = resolveValue(value, thisToOtherMap);
			if (mappedValue == null) {
				mappedValue = getFreshValue(thisToOtherMap, constants);
			}
			return mappedValue;
		}
		
		// returns the resolved value or null, if the value cannot be resolved. The value cannot be resolved if:
		// 	for SumC the operand is missing
		//  for Interval data guards, at least one of the endpoints are missing or the interval is invalid
		private DataValue<T> resolveValue(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap) {
			if (value instanceof SumCDataValue) {
				SumCDataValue<T> sumc = (SumCDataValue<T>) value;
				DataValue<T> operand = resolveValue(sumc.getOperand(), thisToOtherMap);
				if (operand != null) {
					return  new SumCDataValue<T>(operand, sumc.getConstant());
				} else {
					return null;
				}
			} else if (value instanceof IntervalDataValue) {
				IntervalDataValue<T> intv = (IntervalDataValue<T>) value;
				DataValue<T> newLeft = null;
				if (intv.getLeft() != null) {
					newLeft = resolveValue(intv.getLeft(), thisToOtherMap);
				}
				
				DataValue<T> newRight = null;
				if (intv.getRight() != null) {
					newRight = resolveValue(intv.getRight(), thisToOtherMap);
				}

				// if the endpoints of the interval were resolved
				if (!Boolean.logicalXor(newRight == null, intv.getRight() == null) && !Boolean.logicalXor(newLeft == null, intv.getLeft() == null)
						&& (newRight != null || newLeft != null)) {
					if ((newRight == null || newLeft == null) || (newRight.getId().compareTo(newLeft.getId()) > 0))
						return ((NumberInequalityTheory<T>)this.theory).pickIntervalDataValue(newLeft, newRight); 
					else 
						// since we use this to canonize a trace from which segments where removed, it can happen that fresh values appear disorderly,
						// that is a bigger fresh value appears before a smaller one. In such cases, intervals are mapped to fresh values
						
						return null;
				} else {
					return null;
				}
				
			} else {
				return thisToOtherMap.get(value);
			}
		}
		
		private FreshValue<T> getFreshValue(Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
			List<DataValue<T>> valList = DataWords.joinValsToList(thisToOtherMap.values(), constants.values(this.type));
			DataValue<T> fv = this.theory.getFreshValue(valList);
			return new FreshValue<T>(fv.getType(), fv.getId());
		}


		public DataValue<T> decanonize(DataValue<T> value,
				Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
			return null;
		}
	}
}
