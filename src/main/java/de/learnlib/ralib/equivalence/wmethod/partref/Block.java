package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.words.ParameterizedSymbol;

public abstract class Block {

    private static class InputSymbolBuilder implements Iterator<DSymbolInstance> {
        private final ParameterizedSymbol act;
        private final SymbolicDataVariable[] vars;
        private final int[] indices;
        private boolean hasNext;

        public InputSymbolBuilder(ParameterizedSymbol act, Set<SymbolicDataVariable> vars) {
            this.act = act;
            this.vars = new SymbolicDataVariable[vars.size()];
            indices = new int[act.getArity()];
            int i = 0;
            for (SymbolicDataVariable var : vars) {
                this.vars[i++] = var;
            }
            for (i = 0; i < act.getArity(); i++) {
                indices[i] = 0;
            }
            hasNext = !vars.isEmpty();
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public DSymbolInstance next() {
            SymbolicDataVariable[] vars = new SymbolicDataVariable[act.getArity()];
            for (int i = 0; i < act.getArity(); i++) {
                vars[i] = this.vars[indices[i]];
            }
            increment(act.getArity() - 1);
            return new DSymbolInstance(act, vars);
        }

        private void increment(int i) {
            indices[i]++;
            if (indices[i] >= vars.length) {
                indices[i] = 0;
                if (i > 0) {
                    increment(i - 1);
                } else {
                    hasNext = false;
                }
            }
        }
    };

    private final Set<SymbolicState> states;

    public Block(Set<SymbolicState> states) {
        this.states = states;
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

    public int maxQuantifiedSDVIndex() {
        int max = 0;
        for (SymbolicState s : states) {
            max = Integer.max(s.maxQuantifiedSDVIndex(), max);
        }
        return max;
    }

    public boolean isOutputConsistent(ParameterizedSymbol ... inacts) {
        if (states.size() == 1) {
            return true;
        }
        Set<SymbolicDataVariable> allVars = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            Set<SymbolicDataValue> vals = s.getConstraints().getVariables();
            for (SymbolicDataValue val : vals) {
                if (val instanceof SymbolicDataVariable var) {
                    allVars.add(var);
                }
            }
        }

        for (ParameterizedSymbol inact : inacts) {
            Set<SymbolicDataVariable> vars = new LinkedHashSet<>(allVars);
            int k = maxIndex() + 1;
            DataType[] types = inact.getPtypes();
            // make sure there are enough vars to allow for each param to be fresh
            for (int i = 0; i < inact.getArity(); i++) {
                vars.add(new SDV(types[i], k++));
            }
            InputSymbolBuilder builder = new InputSymbolBuilder(inact, vars);
            while (builder.hasNext) {
                DSymbolInstance in = builder.next();
                Set<DSymbolInstance> outsExpected = null;
                for (SymbolicState s : states) {
                    Set<DSymbolInstance> outs = s.getPossibleOutputs(in);
                    assert !outs.isEmpty() : "No output for input";
                    if (outsExpected == null) {
                        outsExpected = outs;
                    } else {
                        if (outs.size() != outsExpected.size() ||
                                !outs.containsAll(outsExpected)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public Set<SymbolicState> intersect(Set<SymbolicState> preconditions) {
        Set<SymbolicState> conjunctions = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            Set<SymbolicState> con = s.and(preconditions);
            conjunctions.addAll(con);
        }
        return conjunctions;
    }

    public Set<DSymbolInstance> getPossibleOutputs(DSymbolInstance in) {
        Set<DSymbolInstance> outs = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            outs.addAll(s.getPossibleOutputs(in));
        }
        return outs;
    }

    public Set<SymbolicState> computeOutputPreconditions(DSymbolInstance in, DSymbolInstance out) {
        Set<SymbolicState> precs = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            for (Constraint c : s.computeOutputPreconditions(in, out)) {
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
