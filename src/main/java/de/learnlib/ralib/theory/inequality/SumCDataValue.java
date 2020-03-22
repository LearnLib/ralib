package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataValue;

public class SumCDataValue<T> extends DataValue<T>{
	
	private DataValue<T> operand;
	private DataValue<T> constant;
	
	public SumCDataValue(DataValue<T> dv, DataValue<T> constant) {
		super(dv.getType(), (T) DataValue.add(dv, constant).getId() );
		this.operand = dv;
		this.constant = constant;
		
	}
	
	public DataValue<T> getConstant() {
		return constant;
	}
	
	public DataValue<T> getOperand() {
		return operand;
	}
	
	public DataValue<T> toRegular() {
		return new DataValue<T> (type, id);
	}

	
    @Override
    public String toString() {
        return getOperand().toString() + " + " + constant.toString(); 
    }

}
