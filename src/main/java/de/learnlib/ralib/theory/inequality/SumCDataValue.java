package de.learnlib.ralib.theory.inequality;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;

public class SumCDataValue<T> extends DataValue<T>{
	
	private DataValue<T> operand;
	private DataValue<T> constant;
	
	public SumCDataValue(DataValue<T> dv, DataValue<T> constant) {
		super(dv.getType(), (T) DataValue.add(dv, constant) );
		this.operand = dv;
		this.constant = constant;
		
	}

	private SumCDataValue(DataType type, T id, DataValue<T> constant) {
		super(type, id);
		this.constant = constant;
	}
	
	public DataValue<T> getConstant() {
		return this.constant;
	}
	
	public DataValue<T> getOperand() {
		return this.operand;
	}

	
    @Override
    public String toString() {
        return this.getOperand().toString() + " + " + this.constant.toString(); 
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + operand.hashCode();
        hash = 97 * hash + constant.hashCode();
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SumCDataValue)) {
            return false;
        }
        
        SumCDataValue sumC = (SumCDataValue) obj;
        
        return sumC.constant.equals(this.constant) && super.equals(obj);
    }

}
