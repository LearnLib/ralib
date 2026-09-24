package de.learnlib.ralib.equivalence.wmethod.partref.automata;

import java.util.Objects;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;

public class RaEqualityGuard extends RaGuard {

    private Parameter left;
    private SymbolicDataValue right;
    private Relation rel;

    public RaEqualityGuard(Parameter param, Relation rel, SymbolicDataValue right) {
        this.left = param;
        this.right = right;
        this.rel = rel;
    }

    public Parameter getLeft() {
        return left;
    }

    public SymbolicDataValue getRight() {
        return right;
    }

    public Relation getRelation() {
        return rel;
    }

    public boolean isEquality() {
        return rel.equals(Relation.EQ);
    }

    public boolean isDisequality() {
        return rel.equals(Relation.NEQ);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RaEqualityGuard other = (RaEqualityGuard) obj;
        if (!Objects.equals(left, other.left)) {
            return false;
        }
        if (!Objects.equals(right, other.right)) {
            return false;
        }
        if (!Objects.equals(rel, other.rel)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 13;
        hash = 103 * hash + Objects.hashCode(left);
        hash = 103 * hash + Objects.hashCode(right);
        hash = 103 * hash + Objects.hashCode(rel);
        return hash;
    }

    @Override
    public String toString() {
        String relstr = rel.equals(Relation.EQ) ? "==" : "!=";
        return "(" + left.toString() + relstr + right.toString() + ")";
    }
}
