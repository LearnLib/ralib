package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.BiMap;

import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaEqualityGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import de.learnlib.ralib.words.OutputSymbol;

public class SymbolicState {

    private final Location loc;

    private final Constraint constr;

    public SymbolicState(Location loc) {
        this.loc = loc;
        this.constr = Constraint.TRUE;
    }

    public SymbolicState(Location loc, Constraint constr) {
        this.loc = loc;
        this.constr = constr;
    }

    public Location getLocation() {
        return loc;
    }

    public Constraint getConstraints() {
        return constr;
    }

    public int maxIndex() {
        return constr.maxIndex();
    }

    public boolean areAllRegistersIdentified() {
        Set<Register> eqRegs = getIdentifiedRegisters();
        return eqRegs.containsAll(loc.getRegisters());
    }

    public Set<Register> getIdentifiedRegisters() {
        Set<Register> eqRegs = new LinkedHashSet<>();
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e : constr.getEqualities()) {
            SymbolicDataValue left = e.getKey();
            SymbolicDataValue right = e.getValue();
            if (left.isRegister()) {
                eqRegs.add((Register) left);
            }
            if (right.isRegister()) {
                eqRegs.add((Register) right);
            }
        }
        return eqRegs;
    }

    public Set<SymbolicState> and(Set<SymbolicState> others) {
        Set<SymbolicState> ret = new LinkedHashSet<>();
        for (SymbolicState other : others) {
            Optional<SymbolicState> con = and(other);
            if (con.isPresent()) {
                ret.add(con.get());
            }
        }
        return ret;
    }

    public Optional<SymbolicState> and(SymbolicState other) {
        if (!other.getLocation().equals(loc)) {
            return Optional.empty();
        }

        Constraint con = Constraint.construct(constr, other.constr);
        if (Constraint.isSatisfiable(con)) {
            SymbolicState s = new SymbolicState(loc, con);
            return Optional.of(s);
        }
        return Optional.empty();
    }

    public SymbolicState offset(int k) {
        return new SymbolicState(loc, constr.offset(k));
    }

    public Set<DSymbolInstance> getPossibleOutputs(DSymbolInstance in) {
        return loc.getPossibleOutputSymbols(in, constr);
    }

    public Set<Constraint> computeOutputPreconditions(DSymbolInstance in, DSymbolInstance out) {
        assert out.getBaseSymbol() instanceof OutputSymbol : "Invalid output symbol";
        OutputSymbol outact = (OutputSymbol) out.getBaseSymbol();
        SymbolicDataVariable[] outSdvs = out.getSymbolicValues();
        SymbolicDataVariable[] inSdvs = in.getSymbolicValues();
        Set<Constraint> precs = new LinkedHashSet<>();
        for (Transition t : loc.getTransitionsWithOutput(outact)) {
            if (t.getLabel().equals(in.getBaseSymbol())) {
                BiMap<Parameter, SymbolicDataValue> outParamMap = loc.getOutputParameterMapping(t);
                assert outParamMap != null : "No output parameter mapping found";

                Map<SymbolicDataVariable, SymbolicDataValue> constraintMap = new LinkedHashMap<>();

                for (Map.Entry<Parameter, SymbolicDataValue> outParamEntry : outParamMap.entrySet()) {
                    Parameter outParam = outParamEntry.getKey();
                    SymbolicDataValue inVal = outParamEntry.getValue();
                    SymbolicDataVariable outSdv = outSdvs[outParam.getId() - 1];
                    if (inVal.isConstant() || inVal.isRegister()) {
                        constraintMap.put(outSdv, inVal);
                    } else if (inVal.isParameter()) {
                        constraintMap.put(outSdv, (SymbolicDataValue) inSdvs[inVal.getId() - 1]);
                    } else {
                        throw new IllegalArgumentException("Invalid type of symbolic data value");
                    }
                }

                Constraint mappingConstraint = Constraint.construct(constraintMap);
                Constraint guardConstraint = constraintFromGuard(t, in, loc);
                Constraint con = Constraint.construct(guardConstraint, mappingConstraint);
                if (Constraint.isSatisfiable(con)) {
                    precs.add(Constraint.construct(guardConstraint, mappingConstraint));
                }
            }
        }
        return precs;
    }

    public Set<SymbolicState> computeStatePreconditions(DSymbolInstance in, DSymbolInstance out, RaModel model) {
        Set<SymbolicState> ret = new LinkedHashSet<>();
        for (Transition t : model.getPredecessorTransitions(loc)) {
            if (model.getDestination(t).equals(loc)) {
                Location src = model.getLocation(t.getSource());
                Optional<Constraint> precOpt = computeStatePrecondition(in, out, t, src);
                if (precOpt.isPresent()) {
                    ret.add(new SymbolicState(src, precOpt.get()));
                }
            }
        }
        return ret;
    }

    private Optional<Constraint> computeStatePrecondition(DSymbolInstance in, DSymbolInstance out, Transition trans, Location src) {
        if (!trans.getLabel().equals(in.getBaseSymbol())) {
            return Optional.empty();
        }
        if (!src.getOutputSymbol(trans).equals(out.getBaseSymbol())) {
            return Optional.empty();
        }
        Constraint guardConstraint = constraintFromGuard(trans, in, src);
        Constraint assignConstraint = constraintFromAssignment(trans, in);
        return Optional.of(Constraint.construct(guardConstraint, assignConstraint));
    }

    private Constraint constraintFromGuard(Transition transition, DSymbolInstance in, Location src) {
        SymbolicDataVariable[] sdvs = in.getSymbolicValues();
        Set<RaGuard> guards = src.getGuards(transition);
        Set<Constraint> constraintList = new LinkedHashSet<>();
        for (RaGuard guard : guards) {
            if (guard instanceof RaEqualityGuard g) {
                Parameter param = g.getLeft();
                SymbolicDataValue right = g.getRight();
                Relation rel = g.getRelation();
                SymbolicDataVariable left = sdvs[param.getId() - 1];
                constraintList.add(Constraint.construct(left, rel, right));
            }
        }
        return Constraint.construct(constraintList);
    }

    private Constraint constraintFromAssignment(Transition t, DSymbolInstance in) {
        Mapping<Register, SymbolicDataValue> remapping = new Mapping<>();
        SymbolicDataVariable[] inVars = in.getSymbolicValues();
        for (Map.Entry<Register, ? extends SymbolicDataValue> assignEntry : t.getAssignment().getAssignment().entrySet()) {
            Register r = assignEntry.getKey();
            SymbolicDataValue val = assignEntry.getValue();
            if (val.isConstant() || val.isRegister()) {
                remapping.put(r, val);
            } else if (val.isParameter()) {
                remapping.put(r, (SymbolicDataValue) inVars[val.getId() - 1]);
            } else {
                throw new IllegalStateException("Shouldn't be here");
            }
        }

        return constr.remap(remapping);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SymbolicState other = (SymbolicState) obj;
        if (!loc.equals(other.loc)) {
            return false;
        }
        if (!constr.equals(other.constr)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = hash * Objects.hashCode(loc);
        hash = hash * Objects.hashCode(constr);
        return hash;
    }

    @Override
    public String toString() {
        return "<" + loc.toString() + ", " + constr.toString() + ">";
    }
}
