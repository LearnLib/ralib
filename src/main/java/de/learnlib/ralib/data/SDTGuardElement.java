package de.learnlib.ralib.data;

import java.math.BigDecimal;

import gov.nasa.jpf.constraints.api.Expression;

public interface SDTGuardElement extends TypedValue {

    static boolean isConstant(SDTGuardElement e) {
        return e.getClass().equals(SymbolicDataValue.Constant.class);
    }

    static boolean isDataValue(SDTGuardElement e) {
        return e.getClass().equals(DataValue.class);
    }

    static boolean isSuffixValue(SDTGuardElement e) {
        return e.getClass().equals(SymbolicDataValue.SuffixValue.class);
    }

    static boolean isRegister(SDTGuardElement e) {
    	return e.getClass().equals(SymbolicDataValue.Register.class);
    }

    Expression<BigDecimal> asExpression();


}
