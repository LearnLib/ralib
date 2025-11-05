package de.learnlib.ralib.data;

import java.util.Map;

import de.learnlib.ralib.data.SymbolicDataValue.Register;

/**
 * A register assignment models which data values
 * of a prefix are stored in which registers.
 */
public class RegisterAssignment extends Mapping<DataValue, Register> {

    public RegisterValuation registerValuation() {
        RegisterValuation vars = new RegisterValuation();
        for (Map.Entry<DataValue, Register> e : this.entrySet()) {
            vars.put(e.getValue(), e.getKey());
        }
        return vars;
    }
    
    public RegisterAssignment relabel(VarMapping<Register, Register> remapping) {
    	RegisterAssignment ret = new RegisterAssignment();
    	for (Map.Entry<DataValue, Register> e : entrySet()) {
    		ret.put(e.getKey(), remapping.get(e.getValue()));
    	}
    	return ret;
    }
}
