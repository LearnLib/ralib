package de.learnlib.ralib.equivalence.wmethod.partref.automata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RARun;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class RaModel extends RegisterAutomaton {

    public static final OutputSymbol ACC = new OutputSymbol("+");
    public static final OutputSymbol REJ = new OutputSymbol("-");
    public static final InputSymbol EMPTY_INPUT = new InputSymbol("ϵ");

    protected static final OutputMapping EMPTY_MAP = new OutputMapping(new ArrayList<>(), new VarMapping<>());

    private final RegisterAutomaton model;

    private final boolean ioMode;

    private final BiMap<Location, RALocation> locMap;

    private final Collection<RALocation> inputLocations;

    private final Map<Location, Set<Transition>> predecessors;

    private final Map<Transition, Location> destinations;

    public RaModel(RegisterAutomaton model, boolean ioMode) {
        this.model = model;
        this.ioMode = ioMode;
        this.inputLocations = getInputLocations(model, ioMode);
        Map<RALocation, Set<Transition>> outputParents = getOutputParents(model, inputLocations);
        this.locMap = createLocations(model, inputLocations, ioMode);
        this.predecessors = computePredecessors(model, inputLocations, locMap, outputParents, ioMode);
        this.destinations = computeDestinations(model, locMap.inverse(), ioMode);
    }

    private static Collection<RALocation> getInputLocations(RegisterAutomaton model, boolean ioMode) {
        if (!ioMode) {
            return model.getStates();
        }
        Collection<RALocation> locs = new LinkedHashSet<>();
        for (RALocation loc : model.getStates()) {
            Collection<Transition> transitions = loc.getOut();
            if (transitions.isEmpty()) {
                continue;
            }
            if (transitions.size() > 1 || !(transitions.iterator().next() instanceof OutputTransition)) {
                locs.add(loc);
            }
        }
        return locs;
    }

    private static BiMap<Location, RALocation> createLocations(RegisterAutomaton model, Collection<RALocation> inputLocations, boolean ioMode) {
        BiMap<Location, RALocation> locMap = HashBiMap.create();
        for (RALocation raloc : inputLocations) {
            Set<Register> regs = new LinkedHashSet<>();
            for (RALocation src : model.getStates()) {
                for (Transition t : src.getOut()) {
                    if (t.getDestination().equals(raloc)) {
                        Set<Register> transRegs = t.getAssignment().getAssignment().keySet();
                        if (regs.isEmpty() && !transRegs.isEmpty()) {
                            regs.addAll(transRegs);
                        } else if (regs.size() != transRegs.size() ||
                                !regs.containsAll(transRegs)) {
                            throw new IllegalArgumentException("Not a valid register automaton");
                        }
                    }
                }
            }

            Location loc = new Location(raloc, regs, ioMode);
            locMap.put(loc, raloc);
        }
        return locMap;
    }

    private static Map<Location, Set<Transition>> computePredecessors(RegisterAutomaton model, Collection<RALocation> inputLocs, BiMap<Location, RALocation> locMap, Map<RALocation, Set<Transition>> outputParents, boolean ioMode) {
        Map<Location, Set<Transition>> ret = new LinkedHashMap<>();
        Collection<RALocation> outputLocs = new LinkedHashSet<>(model.getStates());
        outputLocs.removeAll(inputLocs);
        for (Map.Entry<Location, RALocation> locEntry : locMap.entrySet()) {
            Location loc = locEntry.getKey();
            RALocation dest = locEntry.getValue();
            if (ioMode) {
                for (Transition outTrans : getTransitionsTo(dest, outputLocs)) {
                    for (Map.Entry<RALocation, Set<Transition>> outParentsEntry : outputParents.entrySet()) {
                        if (outParentsEntry.getKey().equals(outTrans.getSource())) {
                            Set<Transition> transitions = ret.containsKey(loc) ? ret.get(loc) : new LinkedHashSet<>();
                            transitions.addAll(outParentsEntry.getValue());
                            ret.put(loc, transitions);
                        }
                    }
                }
            } else {
                assert outputLocs.isEmpty() : "Output locations when not in IO mode";
                for (Transition t : getTransitionsTo(dest, inputLocs)) {
                    Set<Transition> transitions = ret.containsKey(loc) ? ret.get(loc) : new LinkedHashSet<>();
                    transitions.add(t);
                    ret.put(loc, transitions);
                }
            }
        }
        return ret;
    }

    private static Map<RALocation, Set<Transition>> getOutputParents(RegisterAutomaton model, Collection<RALocation> inputLocs) {
        Map<RALocation, Set<Transition>> parents = new LinkedHashMap<>();
        Collection<RALocation> outputLocs = new LinkedHashSet<>(model.getStates());
        outputLocs.removeAll(inputLocs);
        for (RALocation outLoc : outputLocs) {
            for (Transition t : outLoc.getOut()) {
                assert t instanceof OutputTransition && outLoc.getOut().size() == 1 : "Malformed output location";
                RALocation src = t.getSource();
                Set<Transition> trans = parents.containsKey(src) ? parents.get(src) : new LinkedHashSet<>();
                trans.addAll(getTransitionsTo(outLoc, inputLocs));
                parents.put(outLoc, trans);
            }
        }
        return parents;
    }

    private static Map<Transition, Location> computeDestinations(RegisterAutomaton model, BiMap<RALocation, Location> locMap, boolean ioMode) {
        Map<Transition, Location> destinations = new LinkedHashMap<>();
        for (RALocation raloc : locMap.keySet()) {
            for (Transition t : raloc.getOut()) {
                if (ioMode) {
                    RALocation outLoc = t.getDestination();
                    Collection<Transition> trans = outLoc.getOut();
                    assert trans.size() == 1 : "Malformed output location";
                    RALocation dest = trans.iterator().next().getDestination();
                    destinations.put(t, locMap.get(dest));
                } else {
                    RALocation dest = t.getDestination();
                    destinations.put(t, locMap.get(dest));
                }
            }
        }
        return destinations;
    }

    private static Set<Transition> getTransitionsTo(RALocation dest, Collection<RALocation> locs) {
        Set<Transition> trans = new LinkedHashSet<>();
        for (RALocation loc : locs) {
            for (Transition t : loc.getOut()) {
                if (t.getDestination().equals(dest)) {
                    trans.add(t);
                }
            }
        }
        return trans;
    }

    public Set<Location> getLocations() {
        return locMap.keySet();
    }

    public Location getLocation(RALocation raloc) {
        return locMap.inverse().get(raloc);
    }

    public Map<Location, Set<Transition>> getPredecessorTransitions() {
        return predecessors;
    }

    public Collection<Transition> getPredecessorTransitions(Location loc) {
        return predecessors.get(loc);
    }

    public Location getDestination(Transition t) {
        return destinations.get(t);
    }

    public RARun getRun(Word<PSymbolInstance> prefix, Constants consts) {
        if (!ioMode) {
            prefix = removeEpsilon(prefix);
        }
        return super.getRun(prefix, consts);
    }

    @Override
    public Collection<Transition> getTransitions(RALocation state, ParameterizedSymbol input) {
        if (input.equals(EMPTY_INPUT)) {
            return locMap.inverse().get(state).getTransitions(EMPTY_INPUT);
        }
        return model.getTransitions(state, input);
    }

    @Override
    public RALocation getSuccessor(Transition transition) {
        if (transition.getLabel().equals(EMPTY_INPUT)) {
            return locMap.inverse().get(transition.getSource()).getTransitions(EMPTY_INPUT).iterator().next().getDestination();
        }
        return model.getSuccessor(transition);
    }

    @Override
    public Collection<RALocation> getStates() {
        return model.getStates();
    }

    @Override
    public @Nullable RALocation getSuccessor(RALocation state, ParameterizedSymbol input) {
        if (input.equals(EMPTY_INPUT)) {
            return locMap.inverse().get(state).getTransitions(EMPTY_INPUT).iterator().next().getDestination();
        }
        return model.getSuccessor(state, input);
    }

    @Override
    public @Nullable RALocation getInitialState() {
        return model.getInitialState();
    }

    @Override
    public @Nullable Transition getTransition(RALocation state, ParameterizedSymbol input) {
        if (input.equals(EMPTY_INPUT)) {
            return locMap.inverse().get(state).getTransitions(EMPTY_INPUT).iterator().next();
        }
        return model.getTransition(state, input);
    }

    @Override
    public boolean accepts(Word<PSymbolInstance> dw) {
        if (!ioMode) {
            return model.accepts(removeEpsilon(dw));
        }
        return model.accepts(dw);
    }

    @Override
    public RALocation getLocation(Word<PSymbolInstance> dw) {
        return model.getLocation(dw);
    }

    public static Word<PSymbolInstance> removeEpsilon(Word<PSymbolInstance> dw) {
        Word<PSymbolInstance> ret = Word.epsilon();
        for (PSymbolInstance psi : dw) {
            if (!psi.getBaseSymbol().equals(EMPTY_INPUT)) {
                ret = ret.append(psi);
            }
        }
        return ret;
    }
}
