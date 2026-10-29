package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

public enum Relation {
    EQ,
    NEQ;

    public Relation negate() {
        if (this.equals(EQ)) {
            return NEQ;
        }
        return EQ;
    }

    @Override
    public String toString() {
        if (this.equals(EQ)) {
            return "==";
        }
        return "!=";
    }
}
