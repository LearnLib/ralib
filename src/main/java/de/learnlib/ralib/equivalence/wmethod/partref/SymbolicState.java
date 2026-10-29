package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaEqualityGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.symbolgen.BooleanArrayIterator;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;

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

    public Set<DSymbolInstance> getPossibleOutputs(DSymbolInstance in, int startIndex) {
        return loc.getPossibleOutputSymbols(in, startIndex);
    }

    public Set<Constraint> computeOutputPreconditions(DSymbolInstance in, DSymbolInstance out) {
        assert out.getBaseSymbol() instanceof OutputSymbol : "Invalid output symbol";
        OutputSymbol outact = (OutputSymbol) out.getBaseSymbol();
        Set<Constraint> precs = new LinkedHashSet<>();

        for (Transition t : loc.getTransitionsWithOutput(outact)) {
            if (t.getLabel().equals(in.getBaseSymbol())) {
                Map<SymbolicDataVariable, SymbolicDataValue> outEqs = loc.getOutputEqualities(in, out, t);

                List<Transition> transitions = new ArrayList<>(getTransitionsWithInput(loc.getTransitionsWithOutput(outact), in.getBaseSymbol()));
                transitions.remove(t);
                Set<Constraint> otherGuardsConstraints = constraintsFromGuard(transitions, in);
                Constraint mappingConstraint = Constraint.construct(outEqs);
                Constraint guardConstraint = constraintFromGuard(t, in, loc);
                if (otherGuardsConstraints.isEmpty()) {
                    Constraint con = Constraint.construct(guardConstraint, mappingConstraint);
                    if (Constraint.isSatisfiable(con)) {
                        precs.add(Constraint.construct(guardConstraint, mappingConstraint));
                    }
                } else {
                    for (Constraint otherGuardsConstraint : otherGuardsConstraints) {
                        Constraint con = Constraint.construct(guardConstraint, otherGuardsConstraint, mappingConstraint);
                        if (Constraint.isSatisfiable(con)) {
                            precs.add(Constraint.construct(con, mappingConstraint));
                        }
                    }
                }
            }
        }
        return precs;
    }

    public Set<SymbolicState> computeStatePreconditions(DSymbolInstance in, DSymbolInstance out, RaModel model) {
        Set<SymbolicState> ret = new LinkedHashSet<>();
        Collection<Transition> predecessors = model.getPredecessorTransitions(loc);
        if (predecessors == null) {
            assert loc.getRaLocation().getId() == 0 : "Non-initial location with no predecessors";
            return ret;
        }
        Map<RALocation, List<Transition>> transitions = new LinkedHashMap<>();
        for (Transition t : predecessors) {
            if (model.getDestination(t).equals(loc) &&
                    t.getLabel().equals(in.getBaseSymbol()) &&
                    model.getLocation(t.getSource()).getOutputSymbol(t).equals(out.getBaseSymbol())) {
                List<Transition> trans = transitions.containsKey(t.getSource()) ? transitions.get(t.getSource()) : new ArrayList<>();
                trans.add(t);
                transitions.put(t.getSource(), trans);
            }
        }
        for (Map.Entry<RALocation, List<Transition>> e : transitions.entrySet()) {
            Location src = model.getLocation(e.getKey());
            List<Constraint> constraints = computeStatePreconditions(in, out, e.getValue(), src);
            for (Constraint constraint : constraints) {
                ret.add(new SymbolicState(src, constraint));
            }
        }
        return ret;
    }

    private List<Transition> getTransitionsWithInput(Collection<Transition> transitions, ParameterizedSymbol inact) {
        List<Transition> ret = new ArrayList<>();
        for (Transition t : transitions) {
            if (t.getLabel().equals(inact)) {
                ret.add(t);
            }
        }
        return ret;
    }

    private List<Constraint> computeStatePreconditions(DSymbolInstance in, DSymbolInstance out, List<Transition> transitions, Location src) {
        List<Constraint> constraints = new ArrayList<>();
        for (Transition t : transitions) {
            if (t.getLabel().equals(in.getBaseSymbol()) &&
                    src.getOutputSymbol(t).equals(out.getBaseSymbol())) {
                List<Transition> others = new ArrayList<>(getTransitionsWithInput(transitions, in.getBaseSymbol()));
                others.remove(t);
                Constraint outGuardConstraint = Constraint.construct(src.getOutputEqualities(in, out, t));
                Constraint assignConstraint = constraintFromAssignment(t, in, out);
                Constraint guardConstraint = constraintFromGuard(t, in, src);
                Set<Constraint> otherGuardsConstraints = constraintsFromGuard(others, in);
                if (otherGuardsConstraints.isEmpty()) {
                    constraints.add(Constraint.construct(guardConstraint, outGuardConstraint, assignConstraint));
                } else {
                    for (Constraint otherGuardsConstraint : constraintsFromGuard(others, in)) {
                        constraints.add(Constraint.construct(guardConstraint, otherGuardsConstraint, outGuardConstraint, assignConstraint));
                    }
                }
            }
        }
        return constraints;
    }

//    private Optional<Constraint> computeStatePrecondition(DSymbolInstance in, DSymbolInstance out, Transition trans, Location src) {
//        if (!trans.getLabel().equals(in.getBaseSymbol())) {
//            return Optional.empty();
//        }
//        if (!src.getOutputSymbol(trans).equals(out.getBaseSymbol())) {
//            return Optional.empty();
//        }
//        Constraint guardConstraint = constraintFromGuard(trans, in, src);
//        Constraint outGuardConstraint = Constraint.construct(src.getOutputEqualities(in, out, trans));
//        Constraint assignConstraint = constraintFromAssignment(trans, in, out);
//        return Optional.of(Constraint.construct(guardConstraint, outGuardConstraint, assignConstraint));
//    }

    private Set<Constraint> constraintsFromGuard(Collection<Transition> transitions, DSymbolInstance in) {
        List<Set<RaGuard>> guardList = new ArrayList<>();
        SymbolicDataVariable[] vars = in.getSymbolicValues();
        for (Transition t : transitions) {
            Set<RaGuard> guards = RaGuard.fromRaGuard(t.getGuard());
            Set<RaGuard> trimmedGuards = new LinkedHashSet<>();
            for (RaGuard g : guards) {
                if (g instanceof RaEqualityGuard eg) {
                    Parameter p = eg.getLeft();
                    if (!(vars[p.getId() - 1] instanceof QuantifiedSDV)) {
                        trimmedGuards.add(eg);
                    }
                } else {
                    trimmedGuards.add(g);
                }
            }
            guardList.add(trimmedGuards);
        }
        Set<Constraint> ret = new LinkedHashSet<>();
        for (List<Set<RaGuard>> guardPermutation : getGuardPermutations(guardList)) {
            for (Set<RaGuard> guardSet : guardPermutation) {
                Set<Constraint> constraintList = new LinkedHashSet<>();
                for (RaGuard g : guardSet) {
                    constraintList.add(g.toConstraint(in));
                }
                ret.add(Constraint.construct(constraintList));
            }
        }
        return ret;
    }

    private List<List<Set<RaGuard>>> getGuardPermutations(List<Set<RaGuard>> guards) {
        List<List<Set<RaGuard>>> guardPermutations = new ArrayList<>();

        for (int size = 0; size <= guards.size(); size++) {
            BooleanArrayIterator permit = new BooleanArrayIterator(guards.size(), size);
            while (permit.hasNext()) {
                List<Set<RaGuard>> guardPermutation = new ArrayList<>();
                boolean[] permutation = permit.next();
                for (int i = 0; i < guards.size(); i++) {
                    if (permutation[i]) {
                        guardPermutation.add(guards.get(i));
                    } else {
                        Set<RaGuard> negated = new LinkedHashSet<>();
                        for (RaGuard g : guards.get(i)) {
                            negated.add(g.negate());
                        }
                        guardPermutation.add(negated);
                    }
                }
                guardPermutations.add(guardPermutation);
            }
        }

        return guardPermutations;
    }

    private Constraint constraintFromGuard(Transition transition, DSymbolInstance in, Location src) {
        Set<RaGuard> guards = src.getGuards(transition);
        Set<Constraint> constraintList = new LinkedHashSet<>();
        for (RaGuard guard : guards) {
            constraintList.add(guard.toConstraint(in));
        }
        return Constraint.construct(constraintList);
    }

    private Constraint constraintFromAssignment(Transition t, DSymbolInstance in, DSymbolInstance out) {
        Collection<Transition> outTransitions = t.getDestination().getOut();
        Constraint ret = constr;
        if (outTransitions.size() == 1 && outTransitions.iterator().next() instanceof OutputTransition) {
            Transition ot = outTransitions.iterator().next();
            ret = ret.remap(assignmentRemapping(ot, out));
        }

        return ret.remap(assignmentRemapping(t, in));
    }

    private Mapping<Register, SymbolicDataValue> assignmentRemapping(Transition t, DSymbolInstance dsi) {
        Mapping<Register, SymbolicDataValue> remapping = new Mapping<>();
        SymbolicDataVariable[] inVars = dsi.getSymbolicValues();
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
        return remapping;
    }

//    private Constraint constraintFromOutputGuard(Transition t, DSymbolInstance out, Location src) {
//        Map<SymbolicDataVariable, SymbolicDataValue> equalities = new LinkedHashMap<>();
//        SymbolicDataVariable[] outVars = out.getSymbolicValues();
//        for (Map.Entry<Parameter, SymbolicDataValue> e : src.getOutputParameterMapping(t).entrySet()) {
//            Parameter outParam = e.getKey();
//            SymbolicDataValue val = e.getValue();
//            if (val.isRegister() || val.isConstant()) {
//                equalities.put(outVars[outParam.getId() - 1], val);
//            }
//        }
//        return Constraint.construct(equalities);
//    }

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

    public static boolean isDisjoint(Set<SymbolicState> states) {
        SymbolicState[] stateArr = states.toArray(new SymbolicState[states.size()]);
        for (int i = 0; i < states.size(); i++) {
            for (int j = i + 1; j < states.size(); j++) {
                Optional<SymbolicState> s = stateArr[i].and(stateArr[j]);
                if (s.isPresent() && Constraint.isSatisfiable(s.get().getConstraints())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Set<SymbolicState> intersect(Set<SymbolicState> states, Set<SymbolicState> preconditions) {
        Set<SymbolicState> conjunctions = new LinkedHashSet<>();
        for (SymbolicState s : states) {
            Set<SymbolicState> con = s.and(preconditions);
            conjunctions.addAll(con);
        }
        return conjunctions;
    }
}
