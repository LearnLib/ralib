/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.learning;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * A component is a bunch of rows that correspond to the
 * same location in a hypothesis.
 * 
 * @author falk
 */
class Component {

    private final Row primeRow;

    private final Map<Row, VarMapping> otherRows = new LinkedHashMap<>();

    private final ObservationTable obs;

    private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<>();

    private static final LearnLogger log = LearnLogger.getLogger(Component.class);
    
    public Component(Row primeRow, ObservationTable obs) {
        this.primeRow = primeRow;
        this.obs = obs;
    }

    /**
     * tries to add a row to this component. checks if row is equivalent to rows
     * in this component.
     *
     * @param r
     * @return true if successful
     */
    boolean addRow(Row r) {
        if (!primeRow.couldBeEquivalentTo(r)) {
            return false;
        }

        PIVRemappingIterator iterator = new PIVRemappingIterator(
                r.getParsInVars(), primeRow.getParsInVars());

        for (VarMapping m : iterator) {
            if (r.isEquivalentTo(primeRow, m)) {
                this.otherRows.put(r, m);
                return true;
            }
        }
        return false;
    }

    void start(TreeOracle oracle, ParameterizedSymbol... inputs) {
        for (ParameterizedSymbol ps : inputs) {
            SymbolicDecisionTree[] sdts = primeRow.getSDTsForInitialSymbol(ps);
            Branching b = oracle.getInitialBranching(
                    getAccessSequence(), ps, primeRow.getParsInVars(), sdts);

            branching.put(ps, b);
            for (Word<PSymbolInstance> prefix : b.getBranches().keySet()) {
                obs.addPrefix(prefix);
            }
        }
    }

    void addSuffix(SymbolicSuffix suffix, TreeOracle oracle) {

        primeRow.addSuffix(suffix, oracle);
        Map<Row, VarMapping> otherOld = new LinkedHashMap<>(otherRows);
        otherRows.clear();
        List<Component> newComponents = new ArrayList<>();

        for (Row r : otherOld.keySet()) {
            r.addSuffix(suffix, oracle);
            if (addRow(r)) {
                continue;
            }

            boolean added = false;
            for (Component c : newComponents) {
                if (c.addRow(r)) {
                    added = true;
                    break;
                }
            }

            if (!added) {
                Component c = new Component(r, obs);
                newComponents.add(c);
            }
        }

        for (Component c : newComponents) {
            obs.addComponent(c);
        }
    }

    boolean updateBranching(TreeOracle oracle) {
        boolean ret = true;
        for (ParameterizedSymbol ps : branching.keySet()) {
            boolean ub = updateBranching(ps, oracle);
            ret = ret && ub; 
        }
        return ret;
    }

    private boolean updateBranching(ParameterizedSymbol ps, TreeOracle oracle) {
        Branching b = branching.get(ps);
        SymbolicDecisionTree[] sdts = primeRow.getSDTsForInitialSymbol(ps);
        Branching newB = oracle.updateBranching(getAccessSequence(), ps, b, null, sdts);
        boolean ret = true;
        
        log.log(Level.FINEST,"OLD: " + Arrays.toString(b.getBranches().keySet().toArray()));
        log.log(Level.FINEST,"NEW: " + Arrays.toString(newB.getBranches().keySet().toArray()));
        
        for (Word<PSymbolInstance> prefix : newB.getBranches().keySet()) {
            if (!b.getBranches().containsKey(prefix)) {
                obs.addPrefix(prefix);
                ret = false;
            }
        }
        branching.put(ps, newB);
        return ret;
    }

    boolean checkVariableConsistency() {
        if (!checkVariableConsistency(primeRow)) {
            return false;
        }
        for (Row r : otherRows.keySet()) {
            if (!checkVariableConsistency(r)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkVariableConsistency(Row r) {
        if (r.getPrefix().length() < 2) {
            return true;
        }
            
        Word<PSymbolInstance> prefix = r.getPrefix().prefix(r.getPrefix().length() -1);
        Row prefixRow = obs.getComponents().get(prefix).primeRow;
                
        PIV memPrefix = prefixRow.getParsInVars();
        PIV memRow = r.getParsInVars();
        
        int max = DataWords.paramLength(DataWords.actsOf(prefix));
        
        for (Parameter p : memRow.keySet()) {
            // p is used by next but not stored by this and is from this word
            if (!memPrefix.containsKey(p) && p.getId() <= max) {
                SymbolicSuffix suffix = r.getSuffixForMemorable(p);
                SymbolicSuffix newSuffix = new SymbolicSuffix(
                        r.getPrefix(), suffix);
                
                obs.addSuffix(newSuffix);
                return false;
            }
        }
        return true;
    }
    
    Word<PSymbolInstance> getAccessSequence() {
        return primeRow.getPrefix();
    }

    boolean isAccepting() {
        return this.primeRow.isAccepting();
    }
    
    Branching getBranching(ParameterizedSymbol act) {
        return branching.get(act);
    }
    
    VarMapping getRemapping(Row r) {
        return this.otherRows.get(r);
    }
    
    Row getPrimeRow() {
        return this.primeRow;
    }
    
    Collection<Row> getOtherRows() {
        return this.otherRows.keySet();
    }

    @Override
    public String toString() {
        return primeRow.getPrefix().toString() + " " + 
                primeRow.getParsInVars() + " " +
                Arrays.toString(this.otherRows.keySet().toArray());
    }
    
    void toString(StringBuilder sb) {
        sb.append("********** COMPONENT: ").append(getAccessSequence()).append("\n");
        sb.append("PIV: ").append(this.primeRow.getParsInVars()).append("\n");        
        sb.append("******** PREFIXES: ").append("\n");
        for (Row r : getOtherRows()) {
            sb.append(r.getPrefix()).append("\n");
        }
        sb.append("******** BRANCHING: ").append("\n");
        for (Entry<ParameterizedSymbol, Branching> b : branching.entrySet()) {
             sb.append(b.getKey()).append(":\n");
             for (Entry<Word<PSymbolInstance>, TransitionGuard> e : 
                     b.getValue().getBranches().entrySet()) {
                 sb.append(e.getKey()).append(" -> ").append(e.getValue()).append("\n");
             }
        }        
        sb.append("******** ROWS: ").append("\n");
        this.primeRow.toString(sb);
        for (Entry<Row, VarMapping> e : otherRows.entrySet()) {
            e.getKey().toString(sb);
            sb.append("==== remapping: ").append(e.getValue()).append("\n");
        }        
    }
    
}
