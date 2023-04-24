package de.learnlib.ralib.example.list;

import java.util.LinkedList;

/**
 *
 * @author Paul Fiterau
 *
 */
public class BoundedList {
	public static final int DEFAULT_MAX_CAPACITY = 3;
	public static final boolean DEFAULT_USE_NULL= false;
	public static final Integer NULL_VALUE = 0;

	private boolean useNull = false;
	private LinkedList<Integer> list;
	private int maxCapacity;

	public BoundedList() {
		this(DEFAULT_MAX_CAPACITY, DEFAULT_USE_NULL);
	}

	public BoundedList(int maxCapacity) {
		this(maxCapacity, DEFAULT_USE_NULL);
	}

	public BoundedList(int maxCapacity, boolean useNull) {
		this.maxCapacity = maxCapacity;
		this.useNull = useNull;
		list = new LinkedList<Integer>();
	}

	public void push(Integer e) {
		if (useNull && e.equals(NULL_VALUE)) {
			throw new RuntimeException();
		}
		if (maxCapacity > list.size()) {
			list.push(e);
		}
	}

	public Integer pop() {
		return list.pop();
	}

	public boolean contains(Integer e) {
		return list.contains(e);
	}

	public boolean itemnull(Integer e) {
		return (useNull && NULL_VALUE.equals(e)) || !list.contains(e);
	}

	public void insert(Integer e1, Integer e2) {
		int i=1;
		if (useNull && e2.equals(NULL_VALUE)) {
			throw new RuntimeException();
		}
		if (maxCapacity > list.size()) {
			for (Integer el : list) {
				if (el.equals(e1)) {
					list.add(i-1, e2);
					return;
				}
				i++;
			}
			list.addLast(e2);
		}
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}
}
