package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaEqualityGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaGuard;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Relation;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class QuantifiedInputSymbolGenerator implements Iterator<DSymbolInstance> {

    private final ParameterizedSymbol[] inacts;
    private final int normalStartIndex;
    private final int quantStartIndex;
    private final Map<ParameterizedSymbol, Map<Integer, Boolean>> quantifiedParams;

    private int currAct;

    public QuantifiedInputSymbolGenerator(int normalStartIndex, int quantStartIndex, Block block, RaModel model, ParameterizedSymbol ... inacts) {
        this.normalStartIndex = normalStartIndex;
        this.quantStartIndex = quantStartIndex;
        this.inacts = inacts;
        Map<ParameterizedSymbol, List<Integer>> quantifiedParams = getQuantifiedParams(block, model, inacts);
        this.quantifiedParams = new LinkedHashMap<>();
        for (Map.Entry<ParameterizedSymbol, List<Integer>> e : quantifiedParams.entrySet()) {
            Map<Integer, Boolean> isQuantified = new LinkedHashMap<>();
            List<Integer> quantList = e.getValue();
            for (int i = 0; i < inacts.length; i++) {
                isQuantified.put(i, quantList.contains(i));
            }
            this.quantifiedParams.put(e.getKey(), isQuantified);
        }
        currAct = 0;
    }

    private static Map<ParameterizedSymbol, List<Integer>> getQuantifiedParams(Block block, RaModel model, ParameterizedSymbol[] inacts) {
        Map<ParameterizedSymbol, List<Integer>> potQuant = new LinkedHashMap<>();
        for (ParameterizedSymbol inact : inacts) {
            potQuant.put(inact, new ArrayList<>());
            for (SymbolicState s : block.getStates()) {
                Location loc = s.getLocation();
                Constraint c = s.getConstraints();
                for (Transition t : loc.getRaLocation().getOut(inact)) {
                    Set<RaGuard> guards = RaGuard.fromRaGuard(t.getGuard());
                    for (RaGuard g : guards) {
                        if (g instanceof RaEqualityGuard eg &&
                                eg.getRight().isRegister() &&
                                eg.getRelation().equals(Relation.EQ) &&
                                isUnidentified((Register) eg.getRight(), c)) {
                            Parameter p = eg.getLeft();
                            List<Integer> ids = potQuant.get(inact);
                            ids.add(p.getId() - 1);
                            potQuant.put(inact, ids);
                        }
                    }
                }
            }
        }
        return potQuant;
    }

    private static boolean isUnidentified(Register r, Constraint c) {
        Set<Entry<SymbolicDataValue, SymbolicDataValue>> eqs = c.getEqualities();
        for (Entry<SymbolicDataValue, SymbolicDataValue> eq : eqs) {
            if (eq.getKey().isRegister() && r.equals((Register) eq.getKey())) {
                return false;
            }
            if (eq.getValue().isRegister() && r.equals((Register) eq.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasNext() {
        return currAct < inacts.length;
    }

    @Override
    public DSymbolInstance next() {
        ParameterizedSymbol inact = inacts[currAct];
        SymbolicDataVariable[] sdvs = new SymbolicDataVariable[inact.getArity()];
        DataType[] types = inact.getPtypes();
        Map<Integer, Boolean> isQuantified = quantifiedParams.get(inact);
        for (int i = 0; i < inact.getArity(); i++) {
            sdvs[i] = isQuantified.get(i) ?
                    new QuantifiedSDV(types[i], quantStartIndex + i) :
                        new SDV(types[i], normalStartIndex + i);
        }
        currAct++;
        return new DSymbolInstance(inact, sdvs);
    }
}
