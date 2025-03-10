package de.learnlib.ralib.data;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue.Register;

public class Bijection implements Map<Register, Register> {

	private final Map<Register, Register> injection;
	private final Map<Register, Register> surjection;

	public Bijection() {
		injection = new LinkedHashMap<>();
		surjection = new LinkedHashMap<>();
	}

	public Bijection(Map<Register, Register> map) {
		this();
		putAll(injection, surjection, map);
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
	public Register get(Object key) {
		return injection.get(key);
	}

	@Override
	public Register remove(Object key) {
		Register val = get(key);
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
	public Set<Register> keySet() {
		return new LinkedHashSet<>(injection.keySet());
	}

	@Override
	public Set<Entry<Register, Register>> entrySet() {
		return new LinkedHashSet<>(injection.entrySet());
	}

	@Override
	public Register put(Register key, Register value) {
		Register existingVal = injection.get(key);
		if (existingVal != null && !existingVal.equals(value)) {
			remove(key);
		}
		injection.put(key, value);
		surjection.put(value, key);
		return existingVal;
	}

	@Override
	public void putAll(Map<? extends Register, ? extends Register> map) {
		Bijection.putAll(injection, surjection, map);
	}

	@Override
	public Set<Register> values() {
		return new LinkedHashSet<>(injection.values());
	}

	public Bijection inverse() {
		Bijection bi = new Bijection();
		for (Map.Entry<Register, Register> e : injection.entrySet()) {
			bi.put(e.getValue(), e.getKey());
		}
		return bi;
	}

	public Bijection compose(Bijection other) {
		Bijection composition = new Bijection();
		for (Map.Entry<Register, Register> entry : entrySet()) {
			Register val = other.get(entry.getValue());
			if (val == null) {
				throw new IllegalArgumentException("Registers mismatch");
			}
			composition.put(entry.getKey(), val);
		}
		return composition;
	}

	public VarMapping<Register, Register> toVarMapping() {
		VarMapping<Register, Register> vars = new VarMapping<>();
		vars.putAll(injection);
		return vars;
	}

	public String toString() {
		return injection.toString();
	}

	private static void putAll(Map<Register, Register> in,
			Map<Register, Register> sur,
			Map<? extends Register, ? extends Register> map) {
		Set<Register> values = new LinkedHashSet<>(map.values());
		if (map.keySet().size() != values.size()) {
			throw new IllegalArgumentException("Mismatched size of keyset and valueset");
		}
		for (Map.Entry<? extends Register, ? extends Register> e : map.entrySet()) {
			Register key = e.getKey();
			Register val = e.getValue();

			Register existingVal = in.get(key);
			if (existingVal != null && !existingVal.equals(val)) {
				sur.remove(val);
			}

			in.put(key, val);
			sur.put(val, key);
		}
	}
}
