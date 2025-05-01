package de.learnlib.ralib.example.list;

import java.math.BigDecimal;
import java.util.LinkedList;

/**
 *
 * @author Paul Fiterau
 *
 */
public class BoundedList {
	public static final int DEFAULT_MAX_CAPACITY = 3;
	public static final boolean DEFAULT_USE_NULL= false;
	public static final BigDecimal NULL_VALUE = BigDecimal.ZERO;

	private boolean useNull = false;
	private final LinkedList<BigDecimal> list;
	private final int maxCapacity;

	public BoundedList() {
		this(DEFAULT_MAX_CAPACITY, DEFAULT_USE_NULL);
	}

	public BoundedList(int maxCapacity) {
		this(maxCapacity, DEFAULT_USE_NULL);
	}

	public BoundedList(int maxCapacity, boolean useNull) {
		this.maxCapacity = maxCapacity;
		this.useNull = useNull;
		list = new LinkedList<BigDecimal>();
	}

	public void push(BigDecimal e) {
		if (useNull && e.equals(NULL_VALUE)) {
			throw new RuntimeException();
		}
		if (maxCapacity > list.size()) {
			list.push(e);
		}
	}

	public BigDecimal pop() {
		return list.pop();
	}

	public boolean contains(BigDecimal e) {
		return list.contains(e);
	}

	public boolean itemnull(BigDecimal e) {
		return (useNull && NULL_VALUE.equals(e)) || !list.contains(e);
	}

	public void insert(BigDecimal e1, BigDecimal e2) {
		int i = 1;
		if (useNull && e2.equals(NULL_VALUE)) {
			throw new RuntimeException();
		}
		if (maxCapacity > list.size()) {
			for (BigDecimal el : list) {
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
