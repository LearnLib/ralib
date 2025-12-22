package de.learnlib.ralib.data;

import java.util.*;

import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;


public class Bijection<T extends TypedValue> implements Map<T, T> {

	private final Map<T, T> injection;
	private final Map<T, T> surjection;

	public Bijection() {
		injection = new LinkedHashMap<>();
		surjection = new LinkedHashMap<>();
	}

	public Bijection(Map<T, T> map) {
		this();
		putAll(injection, surjection, map);
	}

    public static Bijection<DataValue> identity(Set<DataValue> dataValues) {
		Bijection<DataValue> bij = new Bijection<>();
		for (DataValue dv : dataValues) {
			bij.put(dv, dv);
		}
		return bij;
    }

    @Override
	public int size() {
		return injection.size();
	}

	@Override
	public boolean isEmpty() {
		return injection.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return injection.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return surjection.containsKey(value);
	}

	@Override
	public T get(Object key) {
		return injection.get(key);
	}

	@Override
	public T remove(Object key) {
		T val = get(key);
		injection.remove(key);
		surjection.remove(val);
		return val;
	}

	@Override
	public void clear() {
		injection.clear();
		surjection.clear();
	}

	@Override
	public Set<T> keySet() {
		return new LinkedHashSet<>(injection.keySet());
	}

	@Override
	public Set<Entry<T, T>> entrySet() {
		return new LinkedHashSet<>(injection.entrySet());
	}

	@Override
	public T put(T key, T value) {
		assert key.getDataType().equals(value.getDataType());
		T existingVal = injection.get(key);
		if (existingVal != null && !existingVal.equals(value)) {
			remove(key);
		}
		injection.put(key, value);
		surjection.put(value, key);
		return existingVal;
	}

	@Override
	public void putAll(Map<? extends T, ? extends T> map) {
		Bijection.putAll(injection, surjection, map);
	}

	@Override
	public Set<T> values() {
		return new LinkedHashSet<>(injection.values());
	}

	public Bijection<T> inverse() {
		Bijection<T> bi = new Bijection<>();
		for (Map.Entry<T, T> e : injection.entrySet()) {
			bi.put(e.getValue(), e.getKey());
		}
		return bi;
	}

	public Bijection<T> compose(Bijection<T> other) {
		Bijection<T> composition = new Bijection<>();
		for (Map.Entry<T, T> entry : entrySet()) {
			T val = other.get(entry.getValue());
			if (val == null) {
				throw new IllegalArgumentException("Registers mismatch");
			}
			composition.put(entry.getKey(), val);
		}
		return composition;
	}

	public Mapping<T, T> toVarMapping() {
		Mapping<T, T> vars = new Mapping<>();
		vars.putAll(injection);
		return vars;
	}

	@Override
	public String toString() {
		return injection.toString();
	}

	private static  <U extends TypedValue>  void putAll(Map<U, U> in,
			Map<U, U> sur,
			Map<? extends U, ? extends U> map) {
		Set<U> values = new LinkedHashSet<>(map.values());
		if (map.keySet().size() != values.size()) {
			throw new IllegalArgumentException("Mismatched size of keyset and valueset");
		}
		for (Map.Entry<? extends U, ? extends U> e : map.entrySet()) {
			assert e.getKey().getDataType().equals(e.getValue().getDataType());
			U key = e.getKey();
			U val = e.getValue();

			U existingVal = in.get(key);
			if (existingVal != null && !existingVal.equals(val)) {
				sur.remove(val);
			}

			in.put(key, val);
			sur.put(val, key);
		}
	}

	public static Bijection<Register> dvToRegBijection(Bijection<DataValue> bijection, Word<PSymbolInstance> keyWord, Word<PSymbolInstance> valueWord) {
		DataValue[] keyVals = DataWords.valsOf(keyWord);
		DataValue[] valueVals = DataWords.valsOf(valueWord);
		Bijection<Register> ret = new Bijection<>();
		for (Map.Entry<DataValue, DataValue> e : bijection.entrySet()) {
			DataValue key = e.getKey();
			DataValue val = e.getValue();
			int keyId = findFirstDv(key, keyVals);
			int valueId = findFirstDv(val, valueVals);
			Register regKey = new Register(keyVals[keyId].getDataType(), keyId+1);
			Register regValue = new Register(valueVals[valueId].getDataType(), valueId+1);
			ret.put(regKey, regValue);
		}
		return ret;
	}

	private static int findFirstDv(DataValue dv, DataValue[] vals) {
		for (int i = 0; i < vals.length; i++) {
			if (vals[i].equals(dv)) {
				return i;
			}
		}
		throw new IllegalArgumentException("No matching data value for " + dv + ": " + vals);
	}
}
