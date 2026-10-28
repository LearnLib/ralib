package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataVariable;
import gov.nasa.jpf.constraints.api.Expression;

public interface Constraint {

    public static final FalseConstraint FALSE = new FalseConstraint();
    public static final Constraint TRUE = trueConstraint();

    public int maxIndex();

    public Expression<Boolean> toExpression();

    public Mapping<? extends SymbolicDataValue, ? extends SymbolicDataVariable> toMapping();

    public Constraint remap(Mapping<Register, ? extends SymbolicDataValue> remapping);

    public Constraint offset(int k);

    public Set<SymbolicDataValue> getVariables();

    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getEqualities();

    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getDisequalities();

    public Set<AtomicConstraint> getAtomics();

    public static Constraint construct(SymbolicDataVariable dataValue, Relation relation, SymbolicDataValue param) {
        return new AtomicConstraint(dataValue, relation, param);
    }

    public static Constraint construct(Map<SymbolicDataVariable, SymbolicDataValue> equalities) {
        List<Constraint> cl = new ArrayList<>();
        for (Map.Entry<SymbolicDataVariable, SymbolicDataValue> e : equalities.entrySet()) {
            Constraint c = construct(e.getKey(), Relation.EQ, e.getValue());
            cl.add(c);
        }
        return ConstraintList.construct(cl);
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

    public static Optional<Constraint> combine(Constraint c1, Constraint c2) {
        Optional<Map.Entry<Constraint, Constraint>> prepared = cancelOutContradictions(c1, c2);
        Constraint ca = prepared.isEmpty() ? c1 : prepared.get().getKey();
        Constraint cb = prepared.isEmpty() ? c2 : prepared.get().getValue();

        Set<SymbolicDataValue> vars1 = ca.getVariables();
        Set<SymbolicDataValue> vars2 = cb.getVariables();
        if (vars1.size() != vars2.size() || !vars1.containsAll(vars2)) {
            return Optional.empty();
        }

        if (isMatchingSet(ca.getEqualities(), cb.getEqualities()) &&
                isMatchingSet(ca.getDisequalities(), cb.getDisequalities())) {
            return Optional.of(construct(ca, cb));
        }
        return Optional.empty();
    }

    private static boolean isMatchingSet(Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> s1, Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> s2) {
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e1 : s1) {
            boolean found = false;
            for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e2 : s2) {
                if ((e2.getKey().equals(e1.getKey()) && e2.getValue().equals(e1.getValue())) ||
                        (e2.getKey().equals(e1.getValue()) && e2.getValue().equals(e1.getKey()))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static Optional<Map.Entry<Constraint, Constraint>> cancelOutContradictions(Constraint c1, Constraint c2) {
        Optional<Map.Entry<SymbolicDataValue, SymbolicDataValue>> contradiction = findContradiction(c1, c2);
        if (contradiction.isEmpty()) {
            Constraint temp = c1;
            c1 = c2;
            c2 = temp;
            contradiction = findContradiction(c1, c2);
        }
        if (contradiction.isPresent()) {
            SymbolicDataValue l = contradiction.get().getKey();
            SymbolicDataValue r = contradiction.get().getValue();
            Constraint nc1 = removeElement(l, r, c1);
            Constraint nc2 = removeElement(l, r, c2);
            Optional<Map.Entry<Constraint, Constraint>> next = cancelOutContradictions(nc1, nc2);
            if (next.isEmpty()) {
                return Optional.of(Map.entry(nc1, nc2));
            }
            return next;
        }
        return Optional.empty();
    }

    private static Optional<Map.Entry<SymbolicDataValue, SymbolicDataValue>> findContradiction(Constraint c1, Constraint c2) {
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> eq1 : c1.getEqualities()) {
            for (Map.Entry<SymbolicDataValue, SymbolicDataValue> de2 : c2.getDisequalities()) {
                if ((eq1.getKey().equals(de2.getKey()) && eq1.getValue().equals(de2.getValue())) ||
                        (eq1.getKey().equals(de2.getValue()) && eq1.getValue().equals(de2.getKey()))) {
                    return Optional.of(eq1);
                }
            }
        }
        return Optional.empty();
    }

    private static Constraint removeElement(SymbolicDataValue l, SymbolicDataValue r, Constraint c) {
        Set<Map.Entry<SymbolicDataVariable, SymbolicDataValue>> eqs = removeElement(l, r, c.getEqualities());
        Set<Map.Entry<SymbolicDataVariable, SymbolicDataValue>> des = removeElement(l, r, c.getDisequalities());
        Constraint[] constrs = new Constraint[eqs.size() + des.size()];
        int i = 0;
        for (Map.Entry<SymbolicDataVariable, SymbolicDataValue> eq : eqs) {
            constrs[i++] = construct(eq.getKey(), Relation.EQ, eq.getValue());
        }
        for (Map.Entry<SymbolicDataVariable, SymbolicDataValue> de : des) {
            constrs[i++] = construct(de.getKey(), Relation.NEQ, de.getValue());
        }
        return construct(constrs);
    }

    private static Set<Map.Entry<SymbolicDataVariable, SymbolicDataValue>> removeElement(SymbolicDataValue l, SymbolicDataValue r, Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> elems) {
        Set<Map.Entry<SymbolicDataVariable, SymbolicDataValue>> ret = new LinkedHashSet<>();
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e : elems) {
            if (!(e.getKey().equals(l) && e.getValue().equals(r)) &&
                    !(e.getKey().equals(r) && e.getValue().equals(l))) {
                assert e.getKey() instanceof SymbolicDataVariable;
                ret.add(Map.entry((SymbolicDataVariable) e.getKey(), e.getValue()));
            }
        }
        return ret;
    }
}
