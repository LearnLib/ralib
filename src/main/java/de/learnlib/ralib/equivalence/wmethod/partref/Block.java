package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.words.ParameterizedSymbol;

public abstract class Block {

    private final Set<SymbolicState> states;

    public Block(Set<SymbolicState> states) {
        this.states = combine(states);
        assert SymbolicState.isDisjoint(this.states) : "Symbolic states not disjoint";
    }

    private static Set<SymbolicState> combine(Set<SymbolicState> states) {
        SymbolicState[] stateArr = states.toArray(new SymbolicState[states.size()]);
        Set<SymbolicState> ret = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>();
        for (int i = 0; i < stateArr.length; i++) {
            Location l = stateArr[i].getLocation();
            if (!done.contains(i)) {
                Constraint combined = stateArr[i].getConstraints();
                for (int j = i + 1; j < stateArr.length; j++) {
                    if (stateArr[j].getLocation().equals(l) &&
                            !done.contains(j)) {
                        Optional<Constraint> opt = Constraint.combine(combined, stateArr[j].getConstraints());
                        if (opt.isPresent()) {
                            combined = opt.get();
                            done.add(j);
                        }
                    }
                }
                ret.add(new SymbolicState(l, combined));
            }
        }
        return ret;
    }

    public Set<SymbolicState> getStates() {
        return states;
    }

    public Set<Location> getLocations() {
        Set<Location> locs = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            locs.add(s.getLocation());
        }
        return locs;
    }

    public int maxIndex() {
        int max = 0;
        for (SymbolicState s : states) {
            max = Integer.max(s.maxIndex(), max);
        }
        return max;
    }

    public boolean hasState(SymbolicState state) {
        for (SymbolicState s : states) {
            if (s.getLocation().equals(state.getLocation())) {
                Constraint conjunction = Constraint.construct(s.getConstraints(), state.getConstraints());
                if (Constraint.isSatisfiable(conjunction)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOutputConsistent(ParameterizedSymbol ... inacts) {
        int k = maxIndex() + 1;

        for (ParameterizedSymbol inact : inacts) {
            DataType[] types = new DataType[inact.getArity()];
            SymbolicDataVariable[] inVars = new SymbolicDataVariable[inact.getArity()];
            for (int i = 0; i < inact.getArity(); i++) {
                inVars[i] = new SDV(types[i], k + i);
            }
            DSymbolInstance in = new DSymbolInstance(inact, inVars);

            Set<DSymbolInstance> outsExpected = null;
            for (SymbolicState s : states) {
                Set<DSymbolInstance> outs = s.getPossibleOutputs(in, k + inact.getArity());
                assert !outs.isEmpty() : "No output for input";
                if (outsExpected == null) {
                    outsExpected = outs;
                } else {
                    if (outs.size() != outsExpected.size() ||
                            !outs.containsAll(outsExpected)) {
                        return false;
                    }
                }
                if (!hasAllRegisters(s, in, outs)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasAllRegisters(SymbolicState state, DSymbolInstance in, Set<DSymbolInstance> outs) {
        Set<Register> regs = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            if (s.getLocation().equals(state.getLocation())) {
                for (SymbolicDataValue v : s.getConstraints().getVariables()) {
                    if (v.isRegister()) {
                        regs.add((Register) v);
                    }
                }
            }
        }

        for (DSymbolInstance out : outs) {
            for (Constraint c : state.computeOutputPreconditions(in, out/*, OutputQualifier.ANY*/)) {
                for (SymbolicDataValue v : c.getVariables()) {
                    if (v.isRegister() && !regs.contains(v)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Set<SymbolicState> intersect(Set<SymbolicState> preconditions) {
        return SymbolicState.intersect(states, preconditions);
    }

    public Set<DSymbolInstance> getPossibleOutputs(DSymbolInstance in, int outStartIndex) {
        Set<DSymbolInstance> outs = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            outs.addAll(s.getPossibleOutputs(in, outStartIndex));
        }
        return outs;
    }

    public Set<SymbolicState> computeOutputPreconditions(DSymbolInstance in, DSymbolInstance out/*, OutputQualifier qualifier*/) {
        Set<SymbolicState> precs = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            for (Constraint c : s.computeOutputPreconditions(in, out/*, qualifier*/)) {
                precs.add(new SymbolicState(s.getLocation(), c));
            }
        }
        return precs;
    }

    public Set<SymbolicState> computeStatePreconditions(DSymbolInstance in, DSymbolInstance out, int offset, RaModel model) {
        Set<SymbolicState> precs = new LinkedHashSet<>();
        for (SymbolicState state : states) {
            SymbolicState s = new SymbolicState(state.getLocation(), state.getConstraints().offset(offset));
            precs.addAll(s.computeStatePreconditions(in, out, model));
        }
        return precs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        // subsclasses should be treated as equal
        if (!(obj instanceof Block)) {
            return false;
        }
        Block other = (Block) obj;
        if (!Objects.equals(states, other.states)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 53 + Objects.hashCode(states);
        return hash;
    }

    @Override
    public String toString() {
        return states.toString();
    }
}
