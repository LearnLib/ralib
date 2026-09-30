package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataVariable;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class FalseConstraint implements Constraint {

    @Override
    public int maxIndex() {
        return 0;
    }

    @Override
    public Expression<Boolean> toExpression() {
        return ExpressionUtil.FALSE;
    }

    @Override
    public Mapping<? extends SymbolicDataValue, ? extends SymbolicDataVariable> toMapping() {
        return new Mapping<>();
    }

    @Override
    public Constraint remap(Mapping<Register, ? extends SymbolicDataValue> remapping) {
        return this;
    }

    @Override
    public Constraint offset(int k) {
        return this;
    }

    @Override
    public Set<SymbolicDataValue> getVariables() {
        return Set.of();
    }

    @Override
    public Set<Entry<SymbolicDataValue, SymbolicDataValue>> getEqualities() {
        return Set.of();
    }

    @Override
    public Set<Entry<SymbolicDataValue, SymbolicDataValue>> getDisequalities() {
        return Set.of();
    }

    @Override
    public Set<AtomicConstraint> getAtomics() {
        return Set.of();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }

    @Override
    public String toString() {
        return "false";
    }
}
