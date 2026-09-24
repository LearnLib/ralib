package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataVariable;

public class ExistentialConstraint extends AtomicConstraint {

    public ExistentialConstraint(QuantifiedSDV dataValue, Register param) {
        super(dataValue, Relation.EQ, param);
    }

    @Override
    public QuantifiedSDV getDataValue() {
        return (QuantifiedSDV) dataValue;
    }

    @Override
    public Register getRegister() {
        return (Register) param;
    }

    @Override
    public int maxIndex() {
        return 0;
    }

    @Override
    public Mapping<? extends SymbolicDataValue, ? extends SymbolicDataVariable> toMapping() {
        return new Mapping<>();
    }

    @Override
    public Constraint remap(Mapping<Register, ? extends SymbolicDataValue> remapping) {
        if (remapping.containsKey(param)) {
            SymbolicDataValue remapped = remapping.get(param);
            if (remapped.isRegister()) {
                return new ExistentialConstraint(getDataValue(), (Register) remapped);
            }
            return Constraint.trueConstraint();
        }
        return this;
    }

    @Override
    public String toString() {
        return "∃" + dataValue.toString() + "." + dataValue.toString() +  "==" + param.toString();
    }
}
