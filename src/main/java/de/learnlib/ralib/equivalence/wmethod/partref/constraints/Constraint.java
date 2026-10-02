package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import gov.nasa.jpf.constraints.api.Expression;

public interface Constraint {

    public static final FalseConstraint FALSE = new FalseConstraint();
    public static final Constraint TRUE = trueConstraint();

    public int maxIndex();

    public int maxQuantifiedSDVIndex();

    public Expression<Boolean> toExpression();

    public Mapping<? extends SymbolicDataValue, ? extends SymbolicDataVariable> toMapping();

    public Constraint remap(Mapping<Register, ? extends SymbolicDataValue> remapping);

    public Constraint offset(int k);

    public Set<SymbolicDataValue> getVariables();

    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getEqualities();

    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getDisequalities();

    public Set<AtomicConstraint> getAtomics();

    public static Constraint construct(SymbolicDataVariable dataValue, Relation relation, SymbolicDataValue param) {
        if (dataValue instanceof SDV sdv) {
            return new SDVConstraint(sdv, relation, param);
        }
        if (dataValue instanceof QuantifiedSDV qsdv) {
            if (!relation.equals(Relation.EQ)) {
                return FALSE;
            }
            assert param.isRegister() : "Existentialy quantified constraints only defined for equality with registers";
            return new ExistentialConstraint(qsdv, (Register) param);
        }
        throw new IllegalStateException("Shouldn't be here");
    }

    public static ConstraintList construct(Map<SymbolicDataVariable, SymbolicDataValue> equalities) {
        ConstraintList cl = new ConstraintList();
        for (Map.Entry<SymbolicDataVariable, SymbolicDataValue> e : equalities.entrySet()) {
            Constraint c = construct(e.getKey(), Relation.EQ, e.getValue());
            cl.add(c);
        }
        return cl;
    }

    public static Constraint construct(Constraint ... constraints) {
        if (constraints.length == 1) {
            return constraints[0];
        }
        return ConstraintList.construct(Arrays.asList(constraints));
    }

    public static Constraint construct(Collection<Constraint> constraints) {
        if (constraints.contains(FALSE)) {
            return FALSE;
        }
        return ConstraintList.construct(constraints);
    }

    public static Constraint trueConstraint() {
        return new ConstraintList();
    }

    public static boolean isSatisfiable(Constraint constraint) {
        if (constraint.equals(FALSE)) {
            return false;
        }

        DSU dsu = new DSU();

        for (SymbolicDataValue v : constraint.getVariables()) {
            dsu.add(v);
        }
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e : constraint.getEqualities()) {
            dsu.union(e.getKey(), e.getValue());
        }
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e : constraint.getDisequalities()) {
            if (dsu.same(e.getKey(), e.getValue())) {
                return false;
            }
        }

        return true;
    }

    public static boolean implies(Constraint a, Constraint b) {
        if (a.equals(FALSE)) {
            return true;
        }
        if (b.equals(FALSE)) {
            return false;
        }

        DSU dsu = new DSU();

        for (SymbolicDataValue v : a.getVariables()) {
            dsu.add(v);
        }
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e : a.getEqualities()) {
            dsu.union(e.getKey(), e.getValue());
        }
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e : a.getDisequalities()) {
            if (dsu.same(e.getKey(), e.getValue())) {
                // a is false, so implication is true
                return true;
            }
        }

        for (AtomicConstraint atomicOfB : b.getAtomics()) {
            boolean satisfiedByA;

            if (atomicOfB.getRelation().equals(Relation.EQ)) {
                satisfiedByA = dsu.same((SymbolicDataValue) atomicOfB.getDataValue(), atomicOfB.getRegister());
            } else {
                satisfiedByA = !dsu.same((SymbolicDataValue) atomicOfB.getDataValue(), atomicOfB.getRegister());
            }

            if (!satisfiedByA) {
                return false;
            }
        }

        return true;
    }
}
