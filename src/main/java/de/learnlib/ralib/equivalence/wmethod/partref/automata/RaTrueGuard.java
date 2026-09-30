package de.learnlib.ralib.equivalence.wmethod.partref.automata;

import java.util.Objects;

import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;

public class RaTrueGuard extends RaGuard {

    public RaTrueGuard() {
    }

    @Override
    public Constraint toConstraint(DSymbolInstance in) {
        return Constraint.trueConstraint();
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
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        return "(true)";
    }
}
