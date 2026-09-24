package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataVariable;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;

public abstract class AtomicConstraint implements Constraint {

    protected SymbolicDataVariable dataValue;

    protected Relation relation;

    protected SymbolicDataValue param;

    public AtomicConstraint(SymbolicDataVariable dataValue, Relation relation, SymbolicDataValue param) {
        this.dataValue = dataValue;
        this.relation = relation;
        this.param = param;
    }

    public SymbolicDataVariable getDataValue() {
        return dataValue;
    }

    public SymbolicDataValue getRegister() {
        return param;
    }

    public Relation getRelation() {
        return relation;
    }

    @Override
    public int maxIndex() {
        return dataValue.getId();
    }

    @Override
    public Expression<Boolean> toExpression() {
        NumericComparator rel = switch(relation) {
        case EQ -> NumericComparator.EQ;
        case NEQ -> NumericComparator.NE;
        default -> throw new IllegalArgumentException("Invalid relation type");
        };

        return new NumericBooleanExpression((SymbolicDataValue) dataValue, rel, param);
    }

    @Override
    public Mapping<? extends SymbolicDataValue, ? extends SymbolicDataVariable> toMapping() {
        Mapping<SymbolicDataValue, SymbolicDataVariable> mapping = new Mapping<>();
        mapping.put(param, dataValue);
        return mapping;
    }

    @Override
    public Set<SymbolicDataValue> getVariables() {
        Set<SymbolicDataValue> vars = new LinkedHashSet<>();
        vars.add((SymbolicDataValue) dataValue);
        vars.add(param);
        return vars;
    }

    @Override
    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getEqualities() {
        if (relation.equals(Relation.EQ)) {
            return Set.of(Map.entry((SymbolicDataValue) dataValue, param));
        }
        return Set.of();
    }

    @Override
    public Set<Map.Entry<SymbolicDataValue, SymbolicDataValue>> getDisequalities() {
        if (relation.equals(Relation.NEQ)) {
            return Set.of(Map.entry((SymbolicDataValue) dataValue, param));
        }
        return Set.of();
    }

    @Override
    public Set<AtomicConstraint> getAtomics() {
        return Set.of(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AtomicConstraint other = (AtomicConstraint) obj;
        if (!Objects.equals(this.dataValue, other.dataValue)) {
            return false;
        }
        if (!Objects.equals(this.relation, other.relation)) {
            return false;
        }
        if (!Objects.equals(this.param, other.param)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(dataValue);
        hash = 83 * hash + Objects.hashCode(relation);
        hash = 83 * hash + Objects.hashCode(param);
        return hash;
    }

    @Override
    public String toString() {
        String rel = switch(relation) {
        case EQ -> "==";
        case NEQ -> "!=";
        default -> "??";
        };
        return dataValue.toString() + rel + param.toString();
    }

}
