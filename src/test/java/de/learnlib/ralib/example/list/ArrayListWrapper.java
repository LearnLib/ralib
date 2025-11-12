package de.learnlib.ralib.example.list;

import java.math.BigDecimal;
import java.util.ArrayList;

public class ArrayListWrapper {
	public static final int DEFAULT_MAX_CAPACITY = 3;

	private final ArrayList<BigDecimal> list;
	private final int capacity;

	public ArrayListWrapper(int capacity) {
		this.capacity = capacity;
		this.list = new ArrayList<>();
	}

	public ArrayListWrapper() {
		this(DEFAULT_MAX_CAPACITY);
	}

	public boolean add(BigDecimal e) {
		if (list.size() < capacity) {
			list.add(e);
			return true;
		}
		return false;
	}

	public boolean remove(BigDecimal e) {
		if (list.contains(e)) {
			list.remove(e);
			return true;
		}
		return false;
	}
}
