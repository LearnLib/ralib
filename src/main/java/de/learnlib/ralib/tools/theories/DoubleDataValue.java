package de.learnlib.ralib.tools.theories;

import java.math.BigDecimal;

import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;

public class DoubleDataValue extends DataValue<BigDecimal> {

	DoubleDataValue(DataType type, BigDecimal id) {
		super(type, id);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof DoubleDataValue && id.compareTo(((DoubleDataValue) other).getId()) == 0)
			return true;
		return super.equals(other);
	}

}
