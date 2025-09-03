package de.learnlib.ralib.data.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.TypedValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.words.PSymbolInstance;

public class DataUtils {

	public static Map<DataType, Integer> typedSize(Set<? extends TypedValue> set) {
		Map<DataType, Integer> ts = new LinkedHashMap<>();
		for (TypedValue s : set) {
			Integer i = ts.get(s.getDataType());
			i = (i == null) ? 1 : i + 1;
			ts.put(s.getDataType(), i);
		}
		return ts;
	}
	
	public static SuffixValuation actionValuation(PSymbolInstance action) {
		SuffixValuation valuation = new SuffixValuation();
		DataValue[] vals = action.getParameterValues();
		SuffixValueGenerator svgen = new SuffixValueGenerator();
		for (int i = 0; i < vals.length; i++) {
			SuffixValue sv = svgen.next(vals[i].getDataType());
			valuation.put(sv, vals[i]);
		}
		return valuation;
	}
}
