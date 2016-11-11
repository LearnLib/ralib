package de.learnlib.ralib.data;

import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDataExpression implements SymbolicDataExpression{
	

	private SymbolicDataExpression expr;
	private DataValue<?> constant;

	public SumCDataExpression(SymbolicDataExpression expr, DataValue<?> constant) {
		this.expr = expr;
		assert !(expr instanceof Constant);
		this.constant = constant;
	}

	public SymbolicDataValue getSDV() {
		return expr.getSDV();
	}
	
	public DataValue<?> getConstant() {
		return constant;
	}
	
	public String toString() {
		return this.expr + " + " +  this.constant.id.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constant == null) ? 0 : constant.hashCode());
		result = prime * result + ((expr == null) ? 0 : expr.hashCode());
		return result;
	}
	
	public SymbolicDataExpression swapSDV(SymbolicDataValue newSDV) {
		return new SumCDataExpression(this.expr.swapSDV(newSDV), constant);
	} 
	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SumCDataExpression other = (SumCDataExpression) obj;
		if (constant == null) {
			if (other.constant != null)
				return false;
		} else if (!constant.equals(other.constant))
			return false;
		if (expr == null) {
			if (other.expr != null)
				return false;
		} else if (!expr.equals(other.expr))
			return false;
		return true;
	}

	public  DataValue<?> instantiateExprForValuation(Mapping<SymbolicDataValue, DataValue<?>> valuation) {
		DataValue<?> operand = expr.instantiateExprForValuation(valuation);
		return  new SumCDataValue(operand, constant);
	}


}
