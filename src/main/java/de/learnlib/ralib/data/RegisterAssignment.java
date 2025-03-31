package de.learnlib.ralib.data;


import java.util.Map;

/**
 * A register assignment models which data values
 * of a prefix are stored in which registers.
 */
public class RegisterAssignment extends Mapping<DataValue, SymbolicDataValue.Register> {

    public RegisterValuation registerValuation() {
        RegisterValuation vars = new RegisterValuation();
        for (Map.Entry<DataValue, SymbolicDataValue.Register> e : this.entrySet()) {
            vars.put(e.getValue(), e.getKey());
        }
        return vars;
    }
}
