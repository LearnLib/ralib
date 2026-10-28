package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

public enum Relation {
    EQ,
    NEQ;

    public Relation negate() {
        return switch(this.ordinal()) {
            case 0 -> NEQ;
            case 1 -> EQ;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return switch(this.ordinal()) {
            case 0 -> "==";
            case 1 -> "!=";
            default -> null;
        };
    }
}
