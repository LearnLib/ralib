package de.learnlib.ralib.equivalence.wmethod.partref.automata;

import static de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel.EMPTY_INPUT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class Location {

    private final RALocation raloc;

    private final Set<Register> regs;

    private final boolean ioMode;

    private final Collection<Transition> transitions;

    private final Transition epsilonTransition;

    private final Map<Transition, OutputMapping> outputMappings;

    private final Map<Transition, OutputSymbol> outputSymbols;

    private final Map<Transition, BiMap<Parameter, SymbolicDataValue>> outputParamMappings;

    private final Map<Transition, Set<RaGuard>> raGuards;

    private final Map<Transition, RALocation> destinations;

    public Location(RALocation raloc, Set<Register> regs, boolean ioMode) {
        this.raloc = raloc;
        this.regs = regs;
        this.ioMode = ioMode;
        outputMappings = new LinkedHashMap<>();
        outputSymbols = new LinkedHashMap<>();
        outputParamMappings = new LinkedHashMap<>();
        raGuards = new LinkedHashMap<>();
        destinations = new LinkedHashMap<>();

        transitions = new ArrayList<>(raloc.getOut());
        epsilonTransition = ioMode ? null : createEpsilonTransition(raloc, regs);
        if (!ioMode) {
            transitions.add(epsilonTransition);
        }

        for (Transition t : transitions) {
            assert !(t instanceof OutputTransition) : "Location is not an input location";
            RALocation dest = t.getDestination();
            if (ioMode) {
                Collection<Transition> nextOut = dest.getOut();
                assert nextOut.size() == 1 : "Malformed output location";
                Transition next = nextOut.iterator().next();
                assert next instanceof OutputTransition : "Output transition expected";
                OutputTransition ot = (OutputTransition) next;
                outputMappings.put(t, ot.getOutput());
                outputSymbols.put(t, ot.getLabel());
                outputParamMappings.putAll(getOutputParamMappings(outputMappings));
                destinations.put(t, ot.getDestination());
            } else {
                outputMappings.put(t, RaModel.EMPTY_MAP);
                outputSymbols.put(t, dest.isAccepting() ? RaModel.ACC : RaModel.REJ);
                outputParamMappings.put(t, HashBiMap.create());
                destinations.put(t, t.getDestination());
            }
            raGuards.put(t, RaGuard.fromRaGuard(t.getGuard()));
        }
    }

    private static Map<Transition, BiMap<Parameter, SymbolicDataValue>> getOutputParamMappings(Map<Transition, OutputMapping> outputMappings) {
        Map<Transition, BiMap<Parameter, SymbolicDataValue>> ret = new LinkedHashMap<>();
        for (Map.Entry<Transition, OutputMapping> outEntry : outputMappings.entrySet()) {
            Transition t = outEntry.getKey();
            OutputMapping outmap = outEntry.getValue();
            VarMapping<Register, ? extends SymbolicDataValue> assign = t.getAssignment().getAssignment();
            BiMap<Parameter, SymbolicDataValue> outParamMap = HashBiMap.create();
            for (Map.Entry<Parameter, SymbolicDataValue> outmapEntry : outmap.getOutput().entrySet()) {
                Parameter outParam = outmapEntry.getKey();
                SymbolicDataValue s = outmapEntry.getValue();
                if (s.isConstant()) {
                    outParamMap.put(outParam, s);
                } else if (s.isRegister()) {
                    Register r = (Register) s;
                    SymbolicDataValue inValue = assign.get(r);
                    assert inValue != null : "Invalid output register";
                    assert inValue.isConstant() || inValue.isParameter() || inValue.isRegister() : "Invalid input symbol";
                    outParamMap.put(outParam, inValue);
                } else {
                    throw new IllegalArgumentException("Invalid type of symbolic data value");
                }
            }
            ret.put(t, outParamMap);
        }
        return ret;
    }

    public RALocation getRaLocation() {
        return raloc;
    }

    public Set<Register> getRegisters() {
        return regs;
    }

    public Collection<Transition> getTransitions() {
        return transitions;
    }

    public Collection<Transition> getTransitions(ParameterizedSymbol inact) {
        if (inact.equals(EMPTY_INPUT)) {
            return List.of(epsilonTransition);
        }
        return raloc.getOut(inact);
    }

    public OutputSymbol getOutputSymbol(Transition t) {
        return outputSymbols.get(t);
    }

    public OutputMapping getOutputMapping(Transition t) {
        return outputMappings.get(t);
    }

    public BiMap<Parameter, SymbolicDataValue> getOutputParameterMapping(Transition t) {
        return outputParamMappings.get(t);
    }

    public Set<RaGuard> getGuards(Transition t) {
        return raGuards.get(t);
    }

    public boolean isDestination(Transition t) {
        return t.getDestination().equals(raloc);
    }

    public Set<OutputSymbol> getPossibleOutputActions(ParameterizedSymbol inact) {
        Set<OutputSymbol> outacts = new LinkedHashSet<>();
        for (Transition t : getTransitions(inact)) {
            outacts.add(outputSymbols.get(t));
        }
        return outacts;
    }

    public Set<DSymbolInstance> getPossibleOutputSymbols(DSymbolInstance in, Constraint constraint) {
        Set<DSymbolInstance> outs = new LinkedHashSet<>();
        SymbolicDataVariable[] inVals = in.getSymbolicValues();
        int maxId = in.maxIndex();
        for (Transition t : getTransitions(in.getBaseSymbol())) {
            Set<RaGuard> guards = raGuards.get(t);
            Constraint guardConstraint = RaGuard.toConstraint(guards, in);
            Constraint con = Constraint.construct(constraint, guardConstraint);
            if (Constraint.isSatisfiable(con)) {
                OutputSymbol outact = outputSymbols.get(t);
                DataType[] types = outact.getPtypes();
                BiMap<Parameter, SymbolicDataValue> outParamMap = outputParamMappings.get(t);
                SymbolicDataVariable[] sdvs = new SymbolicDataVariable[outact.getArity()];
                for (int i = 0; i < outact.getArity(); i++) {
                    Parameter outParam = new Parameter(types[i], i + 1);
                    SymbolicDataValue inVal = outParamMap.get(outParam);
                    assert inVal != null : "Unknown parameter";
                    if (inVal.isParameter()) {
                        sdvs[i] = inVals[inVal.getId() - 1];
                    } else {
                        sdvs[i] = new SDV(outParam.getDataType(), (maxId++));
                    }
                }

                outs.add(new DSymbolInstance(outact, sdvs));
            }
        }

        return outs;
    }

    public Set<Transition> getTransitionsWithOutput(OutputSymbol outact) {
        Set<Transition> transitions = new LinkedHashSet<>();
        for (Map.Entry<Transition, OutputSymbol> e : outputSymbols.entrySet()) {
            if (e.getValue().equals(outact)) {
                transitions.add(e.getKey());
            }
        }
        return transitions;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Location other = (Location) obj;
        if (!raloc.equals(other.raloc)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(raloc);
        return hash;
    }

    @Override
    public String toString() {
        return raloc.toString();
    }

    private static Transition createEpsilonTransition(RALocation loc, Set<Register> regs) {
        VarMapping<Register, Register> regmap = new VarMapping<>();
        for (Register r : regs) {
            regmap.put(r, r);
        }
        Assignment copy = new Assignment(regmap);
        return new InputTransition(ExpressionUtil.TRUE, EMPTY_INPUT, loc, loc, copy);
    }
}
