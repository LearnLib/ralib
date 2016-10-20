package de.learnlib.ralib.theory.equality;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueMapper;

public class EqualityValueMapper<T> implements ValueMapper<T>{

	private DataType type;
	private EqualityTheory<T> theory;

	public EqualityValueMapper(DataType type, EqualityTheory<T> theory) {
		this.type = type;
		this.theory = theory;
	}
	

	public DataValue<T> canonize(T value, Map<T, DataValue<T>> thisToOtherMap) {
		if (thisToOtherMap.containsKey(value)) {
			DataValue<T> dv = thisToOtherMap.get(value);
			return new DataValue<>(dv.getType(), dv.getId());
		}
		
		DataValue<T> fv = this.theory.getFreshValue(new ArrayList<>(thisToOtherMap.values()));
		return new FreshValue<>(fv.getType(), fv.getId());
	}

	public T decanonize(DataValue<T> value, Map<DataValue<T>, T> thisToOtherMap) {
		if (thisToOtherMap.containsKey(value)) 
			return thisToOtherMap.get(value);

		DataValue<T> fv = this.theory.getFreshValue(thisToOtherMap.values().stream().
				map(v -> new DataValue<>(this.type, v)).collect(Collectors.toList()));
		return fv.getId();
	}
}
