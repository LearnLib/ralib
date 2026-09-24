package de.learnlib.ralib.equivalence.wmethod.partref.automata;

import java.util.Objects;

public class RaTrueGuard extends RaGuard {

    public RaTrueGuard() {
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
