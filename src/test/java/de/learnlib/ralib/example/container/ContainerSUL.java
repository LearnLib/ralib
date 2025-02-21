package de.learnlib.ralib.example.container;

import java.math.BigDecimal;

public class ContainerSUL {

	public static final BigDecimal ERROR = BigDecimal.ZERO;

	private BigDecimal val = null;

	public void put(BigDecimal val) {
		this.val = val;
	}

	public BigDecimal get() {
		return val == null ? ERROR : val;
	}
}
