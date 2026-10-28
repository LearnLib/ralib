package de.learnlib.ralib.equivalence.wmethod.partref.symbolgen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.FreshSDV;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.Block;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.SymbolicState;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaEqualityGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class InputSymbolGenerator implements Iterator<DSymbolInstance> {

    private final ParameterizedSymbol[] inacts;
    protected final Map<ParameterizedSymbol, List<Integer>> guardedParams;
    private final int startIndex;
    private int currAct;
    private boolean[] isCurrentlyFresh;

    protected InputSymbolGenerator(int startIndex, Block block, RaModel model, ParameterizedSymbol[] inacts, Map<ParameterizedSymbol, List<Integer>> guardedParams) {
        this.inacts = inacts;
        this.startIndex = startIndex;
        this.guardedParams = guardedParams;
        currAct = 0;

        if (inacts.length > 0) {
            isCurrentlyFresh = new boolean[inacts[0].getArity()];
            for (int i = 0; i < inacts[0].getArity(); i++) {
                isCurrentlyFresh[i] = false;
            }
        } else {
            isCurrentlyFresh = new boolean[0];
        }
    }

    public InputSymbolGenerator(int startIndex, Block block, RaModel model, ParameterizedSymbol ... inacts) {
        this(startIndex, block, model, inacts, findGuardedParams(block, model, inacts));
    }

    protected static Map<ParameterizedSymbol, List<Integer>> findGuardedParams(Block block, RaModel model, ParameterizedSymbol[] inacts) {
        Map<ParameterizedSymbol, List<Integer>> guardedParams = new LinkedHashMap<>();
        for (ParameterizedSymbol inact : inacts) {
            guardedParams.put(inact, new ArrayList<>());
            for (SymbolicState s : block.getStates()) {
                Location loc = s.getLocation();
                Constraint c = s.getConstraints();
                for (Transition t : loc.getRaLocation().getOut(inact)) {
                    Set<RaGuard> guards = RaGuard.fromRaGuard(t.getGuard());
                    for (RaGuard g : guards) {
                        if (g instanceof RaEqualityGuard eg && eg.getRelation().equals(Relation.EQ)) {
                            int id = eg.getLeft().getId() - 1;
                            List<Integer> ids = guardedParams.get(inact);
                            if (!ids.contains(id)) {
                                ids.add(id);
                            }
                            guardedParams.put(inact, ids);
                        }
                    }
                }
            }
        }
        return guardedParams;
    }

    @Override
    public boolean hasNext() {
        return currAct < inacts.length;
    }

    @Override
    public DSymbolInstance next() {
        ParameterizedSymbol inact = inacts[currAct];
        SymbolicDataVariable[] vars = new SymbolicDataVariable[inact.getArity()];
        DataType[] types = inact.getPtypes();
        List<Integer> guarded = guardedParams.get(inact);
        for (int i = 0; i < inact.getArity(); i++) {
            if (isCurrentlyFresh[i]) {
                vars[i] = new FreshSDV(types[i], startIndex + i);
            } else {
                vars[i] = new SDV(types[i], startIndex + i);
            }
        }
        DSymbolInstance symbol = new DSymbolInstance(inact, vars);

        // advance
        boolean flippedToTrue = false;
        for (int i = guarded.size() - 1; i >= 0; i--) {
            int index = guarded.get(i);
            if (isCurrentlyFresh[index]) {
                isCurrentlyFresh[index] = false;
            } else {
                isCurrentlyFresh[index] = true;
                flippedToTrue = true;
                break;
            }
        }
        if (!flippedToTrue) {
            currAct++;
            if (currAct < inacts.length) {
                isCurrentlyFresh = new boolean[inacts[currAct].getArity()];
                for (int i = 0; i < inacts[currAct].getArity(); i++) {
                    isCurrentlyFresh[i] = false;
                }
            }
        }

        return symbol;
    }

}
