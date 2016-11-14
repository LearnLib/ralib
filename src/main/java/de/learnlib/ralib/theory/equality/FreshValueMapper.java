package de.learnlib.ralib.theory.equality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;

public class FreshValueMapper<T> implements ValueMapper<T>{

	private Theory<T> theory;

	public FreshValueMapper(Theory<T> theory) {
		this.theory = theory;
	}
	

	public DataValue<T> canonize(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(value)) {
			DataValue<T> dv = thisToOtherMap.get(value);
			return new DataValue<>(dv.getType(), dv.getId());
		}
		
		DataValue<T> fv = this.theory.getFreshValue(new ArrayList<>(thisToOtherMap.values()));
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	public DataValue<T> decanonize(DataValue<T> value, Map<DataValue<T>, DataValue<T>> thisToOtherMap, Constants constants) {
		if (thisToOtherMap.containsKey(value)) 
			return thisToOtherMap.get(value);
		if (constants.containsValue(value))
			return value;

		List<DataValue<T>> valList = DataWords.joinValsToList(thisToOtherMap.values(), constants.values(value.getType()));
		DataValue<T> fv = this.theory.getFreshValue(valList);
//				this.theory.getFreshValue(thisToOtherMap.values().stream().
//				map(v -> new DataValue<>(this.type, v)).collect(Collectors.toList()));
		return fv;
	}
}
