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
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class RaModel extends RegisterAutomaton {

    public static final OutputSymbol ACC = new OutputSymbol("+");
    public static final OutputSymbol REJ = new OutputSymbol("-");

    protected static final OutputMapping EmptyMap = new OutputMapping(new ArrayList<>(), new VarMapping<>());

    private final RegisterAutomaton model;

    private final BiMap<Location, RALocation> locMap;

    private final Map<Location, Set<Transition>> predecessors;

    private final Map<Transition, Location> destinations;

    public RaModel(RegisterAutomaton model) {
        this.model = model;
        Map<RALocation, Set<Transition>> outputParents = getOutputParents(model);
        this.locMap = createLocations(model);
        this.predecessors = computePredecessors(model, locMap, outputParents);
        this.destinations = computeDestinations(model, locMap.inverse());
    }

    private static BiMap<Location, RALocation> createLocations(RegisterAutomaton model) {
        BiMap<Location, RALocation> locMap = HashBiMap.create();
        for (RALocation raloc : model.getInputStates()) {
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

            Location loc = new Location(raloc, regs);
            locMap.put(loc, raloc);
        }
        return locMap;
    }

    private static Map<Location, Set<Transition>> computePredecessors(RegisterAutomaton model, BiMap<Location, RALocation> locMap, Map<RALocation, Set<Transition>> outputParents) {
        Map<Location, Set<Transition>> ret = new LinkedHashMap<>();
        Collection<RALocation> inputLocs = model.getInputStates();
        Collection<RALocation> outputLocs = new LinkedHashSet<>(model.getStates());
        outputLocs.removeAll(inputLocs);
        for (Map.Entry<Location, RALocation> locEntry : locMap.entrySet()) {
            Location loc = locEntry.getKey();
            RALocation dest = locEntry.getValue();
            if (outputLocs.isEmpty()) {
                // not io mode
                for (Transition t : getTransitionsTo(dest, inputLocs)) {
                    Set<Transition> transitions = ret.containsKey(loc) ? ret.get(loc) : new LinkedHashSet<>();
                    transitions.add(t);
                    ret.put(loc, transitions);
                }
            } else {
                // io mode
                for (Transition outTrans : getTransitionsTo(dest, outputLocs)) {
                    for (Map.Entry<RALocation, Set<Transition>> outParentsEntry : outputParents.entrySet()) {
                        if (outParentsEntry.getKey().equals(outTrans.getSource())) {
                            Set<Transition> transitions = ret.containsKey(loc) ? ret.get(loc) : new LinkedHashSet<>();
                            transitions.addAll(outParentsEntry.getValue());
                            ret.put(loc, transitions);
                        }
                    }
                }
            }
        }
        return ret;
    }

    private static Map<RALocation, Set<Transition>> getOutputParents(RegisterAutomaton model) {
        Map<RALocation, Set<Transition>> parents = new LinkedHashMap<>();
        Collection<RALocation> inputLocs = model.getInputStates();
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

    private static Map<Transition, Location> computeDestinations(RegisterAutomaton model, BiMap<RALocation, Location> locMap) {
        Map<Transition, Location> destinations = new LinkedHashMap<>();
        Collection<RALocation> inputLocs = model.getInputStates();
        for (RALocation raloc : locMap.keySet()) {
            for (Transition t : raloc.getOut()) {
                if (inputLocs.contains(t.getDestination())) {
                    RALocation dest = t.getDestination();
                    destinations.put(t, locMap.get(dest));
                } else {
                    RALocation outLoc = t.getDestination();
                    Collection<Transition> trans = outLoc.getOut();
                    assert trans.size() == 1 : "Malformed output location";
                    RALocation dest = trans.iterator().next().getDestination();
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

    @Override
    public Collection<Transition> getTransitions(RALocation state, ParameterizedSymbol input) {
        return model.getTransitions(state, input);
    }

    @Override
    public RALocation getSuccessor(Transition transition) {
        return model.getSuccessor(transition);
    }

    @Override
    public Collection<RALocation> getStates() {
        return model.getStates();
    }

    @Override
    public @Nullable RALocation getSuccessor(RALocation state, ParameterizedSymbol input) {
        return model.getSuccessor(state, input);
    }

    @Override
    public @Nullable RALocation getInitialState() {
        return model.getInitialState();
    }

    @Override
    public @Nullable Transition getTransition(RALocation state, ParameterizedSymbol input) {
        return model.getTransition(state, input);
    }

    @Override
    public boolean accepts(Word<PSymbolInstance> dw) {
        return model.accepts(dw);
    }

    @Override
    public RALocation getLocation(Word<PSymbolInstance> dw) {
        return model.getLocation(dw);
    }

}
