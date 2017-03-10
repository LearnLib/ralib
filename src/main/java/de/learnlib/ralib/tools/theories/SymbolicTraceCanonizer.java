package de.learnlib.ralib.tools.theories;

import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.mapper.SymbolicValueMapper;
import de.learnlib.ralib.mapper.ValueCanonizer;
import de.learnlib.ralib.mapper.ValueMapper;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.theory.Theory;
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
		theories.forEach( (dt, th) -> valueMappers.put(dt, new SymbolicValueMapper(th, dt)));
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
	
}
