package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;

public class SDVConstraint extends AtomicConstraint {

    protected SDVConstraint(SDV dataValue, Relation relation, SymbolicDataValue param) {
        super(dataValue, relation, param);
    }

    @Override
    public SDV getDataValue() {
        return (SDV) dataValue;
    }

    @Override
    public Constraint remap(Mapping<Register, ? extends SymbolicDataValue> remapping) {
        if (remapping.containsKey(param)) {
            return new SDVConstraint(getDataValue(), relation, remapping.get(param));
        }
        return this;
    }

    @Override
    public Constraint offset(int k) {
        return new SDVConstraint(new SDV(dataValue.getDataType(), dataValue.getId() + k), relation, param);
    }
}
