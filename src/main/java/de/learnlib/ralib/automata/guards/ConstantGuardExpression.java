package de.learnlib.ralib.automata.guards;

import java.util.Collection;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

public class ConstantGuardExpression extends GuardExpression{
	
	private SymbolicDataValue sdv;
	private DataValue<?> constant;

	public ConstantGuardExpression(SymbolicDataValue sdv, DataValue<?> constant) {
		this.sdv = sdv;
		this.constant = constant;
	}

	public GuardExpression relabel(VarMapping relabelling) {
		if (relabelling.containsKey(sdv))
			return new ConstantGuardExpression( (SymbolicDataValue)relabelling.get(sdv), constant);
		return this;
	}
	
	public SymbolicDataValue getVariable() {
		return sdv;
	}
	
	public DataValue<?> getConstant() {
		return constant;
	}

	@Override
	public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {
		return val.get(sdv).equals(constant);
	}

	@Override
	protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
		vals.add(sdv);
	}

	@Override
	protected void getAtoms(Collection<AtomicGuardExpression> vals) {
		// TODO Auto-generated method stub
		
	}
	
	public String toString() {
		return "(" + sdv + "=" + this.constant.getId() + ")";
	}

}
