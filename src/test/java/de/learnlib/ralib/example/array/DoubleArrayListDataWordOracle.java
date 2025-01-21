package de.learnlib.ralib.example.array;

import java.math.BigDecimal;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.InputSymbol;

public class DoubleArrayListDataWordOracle extends ArrayListDataWordOracle<BigDecimal> {

	public static final DataType DOUBLE_TYPE = new DataType("double", BigDecimal.class);

	public static final InputSymbol PUSH = new InputSymbol("push", new DataType[] {DOUBLE_TYPE});
	public static final InputSymbol REMOVE = new InputSymbol("remove", new DataType[] {DOUBLE_TYPE});

	public DoubleArrayListDataWordOracle(int capacity) {
		super(capacity);
	}

	@Override
	protected InputSymbol pushSymbol() {
		return PUSH;
	}

	@Override
	protected InputSymbol removeSymbol() {
		return REMOVE;
	}

}
