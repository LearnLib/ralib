package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataVariable;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class ConstraintList implements Constraint, Iterable<Constraint> {

    private final Set<Constraint> constrs;

    protected ConstraintList() {
        constrs = new LinkedHashSet<>();
    }

    protected ConstraintList(Collection<Constraint> constraints) {
        constrs = new LinkedHashSet<>();
        for (Constraint c : constraints) {
            if (c instanceof ConstraintList cl) {
                constrs.addAll(cl.constrs);
            } else {
                constrs.add(c);
            }
        }
    }

    public void add(Constraint constraint) {
        if (constraint instanceof ConstraintList cl) {
            constrs.addAll(cl.constrs);
        } else {
            constrs.add(constraint);
        }
    }

    @Override
    public int maxIndex() {
        int index = 0;
        for (Constraint c : constrs) {
            index = Integer.max(index, c.maxIndex());
        }
        return index;
    }

    @Override
    public int maxQuantifiedSDVIndex() {
        int index = 0;
        for (Constraint c : constrs) {
            index = Integer.max(index, c.maxQuantifiedSDVIndex());
        }
        return index;
    }

    @Override
    public Expression<Boolean> toExpression() {
        if (constrs.isEmpty()) {
            return ExpressionUtil.TRUE;
        }
        Expression[] exprs = new Expression[constrs.size()];
        int i = 0;
        for (Constraint c : constrs) {
            exprs[(i++)] = c.toExpression();
        }
        return ExpressionUtil.and(exprs);
    }

    @Override
    public Mapping<? extends SymbolicDataValue, ? extends SymbolicDataVariable> toMapping() {
        Mapping<SymbolicDataValue, SymbolicDataVariable> mapping = new Mapping<>();
        for (Constraint c : constrs) {
            mapping.putAll(c.toMapping());
        }
        return mapping;
    }

    @Override
    public Constraint remap(Mapping<Register, ? extends SymbolicDataValue> remapping) {
        ConstraintList ret = new ConstraintList();
        for (Constraint c : constrs) {
            ret.add(c.remap(remapping));
        }
        return ret;
    }

    @Override
    public Constraint offset(int k) {
        ConstraintList ret = new ConstraintList();
        for (Constraint c : constrs) {
            ret.add(c.offset(k));
        }
        return ret;
    }

    @Override
    public Set<SymbolicDataValue> getVariables() {
        Set<SymbolicDataValue> vars = new LinkedHashSet<>();
        for (Constraint c : constrs) {
            vars.addAll(c.getVariables());
        }
        return vars;
    }

    @Override
    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getEqualities() {
        Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> eqs = new LinkedHashSet<>();
        for (Constraint c : constrs) {
            eqs.addAll(c.getEqualities());
        }
        return eqs;
    }

    @Override
    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getDisequalities() {
        Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> eqs = new LinkedHashSet<>();
        for (Constraint c : constrs) {
            eqs.addAll(c.getDisequalities());
        }
        return eqs;
    }

    @Override
    public Set<AtomicConstraint> getAtomics() {
        Set<AtomicConstraint> acs = new LinkedHashSet<>();
        for (Constraint c : constrs) {
            acs.addAll(c.getAtomics());
        }
        return acs;
    }

    @Override
    public Iterator<Constraint> iterator() {
        return constrs.iterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConstraintList other = (ConstraintList) obj;
        if (constrs.size() != other.constrs.size()) {
            return false;
        }
        if (!other.constrs.containsAll(constrs)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 83 * hash + Objects.hashCode(getClass());
        hash = 83 * hash + Objects.hashCode(constrs);
        return hash;
    }

    @Override
    public String toString() {
        if (constrs.isEmpty()) {
            return "true";
        }
        String str = "[";
        Iterator<Constraint> it = constrs.iterator();
        while (it.hasNext()) {
            str = str + it.next().toString();
            if (it.hasNext()) {
                str = str + " ∧ ";
            }
        }
        return str + "]";
    }

    public static Constraint construct(Collection<Constraint> constrs) {
        if (constrs.contains(Constraint.FALSE)) {
            return Constraint.FALSE;
        }
        Set<Constraint> flattened = new LinkedHashSet<>();
        for (Constraint c : constrs) {
            if (c instanceof ConstraintList cl) {
                if (!cl.constrs.isEmpty()) {
                    flattened.addAll(cl.constrs);
                }
            } else {
                flattened.add(c);
            }
        }
        if (flattened.size() == 1) {
            return flattened.iterator().next();
        }
        return new ConstraintList(flattened);
    }
}
