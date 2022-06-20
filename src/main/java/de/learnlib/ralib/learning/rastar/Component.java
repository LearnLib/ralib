/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.learning.rastar;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
public class Component implements LocationComponent {

    private final Row primeRow;

    private final Map<Row, VarMapping> otherRows = new LinkedHashMap<>();

    private final ObservationTable obs;

    private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<>();

    private final boolean ioMode;
    
    private final Constants consts;
    
    private static final LearnLogger log = LearnLogger.getLogger(Component.class);
    
    public Component(Row primeRow, ObservationTable obs, boolean ioMode, Constants consts) {
        this.primeRow = primeRow;
        this.obs = obs;
        this.ioMode = ioMode;
        this.consts = consts;
    }

    /**
     * tries to add a row to this component. checks if row is equivalent to rows
     * in this component.
     *
     * @param r
     * @return true if successful
     */
    boolean addRow(Row r) {
        
        if (ioMode && (isInputComponent() == isInput(r.getPrefix().lastSymbol().getBaseSymbol()))) {
            return false;
        }
        
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
        
        boolean input = isInputComponent();        
        for (ParameterizedSymbol ps : inputs) {
            
            if (ioMode && (input ^ isInput(ps))) {
                continue;
            }
            
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

        if (ioMode && suffix.getActions().length() > 0 && 
                getAccessSequence().length() > 0 && !isAccepting()) {
            log.log(Level.INFO, "Not adding suffix " + suffix + " to error component " + getAccessSequence());
            return;
        }
        
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
                Component c = new Component(r, obs, ioMode, consts);
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
        Branching newB = oracle.updateBranching(getAccessSequence(), ps, b, 
                primeRow.getParsInVars(), sdts);
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
                        r.getPrefix(), suffix, consts);
                
//               System.out.println("Found inconsistency. msissing " + p +
//                        " in mem. of " + prefix);                
//               System.out.println("Fixing with prefix " + r.getPrefix() + " and suffix " + suffix);
//               System.out.println("New symbolic suffix: " + newSuffix);

                obs.addSuffix(newSuffix);
                return false;
            }
        }
        return true;
    }
    
    public Word<PSymbolInstance> getAccessSequence() {
        return primeRow.getPrefix();
    }

    public boolean isAccepting() {
        return this.primeRow.isAccepting();
    }
    
    public Branching getBranching(ParameterizedSymbol act) {
        return branching.get(act);
    }
    
    public VarMapping getRemapping(PrefixContainer r) {
        return this.otherRows.get(r);
    }
    
    Row getPrimeRow() {
        return this.primeRow;
    }
    
    Collection<Row> getOtherRows() {
        return this.otherRows.keySet();
    }
    
    public PrefixContainer getPrimePrefix() {
    	return getPrimeRow();
    }
    
    public Collection<PrefixContainer> getOtherPrefixes() {
    	Collection<PrefixContainer> ret = new LinkedHashSet<PrefixContainer>();
    	for (Row r : getOtherRows())
    		ret.add(r);
    	return ret;
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

    private boolean isInputComponent() {
        if (this.getAccessSequence().length() == 0)
            return true;
        
        ParameterizedSymbol ps = this.getAccessSequence().lastSymbol().getBaseSymbol();
        return !isInput(ps);
    }

    private boolean isInput(ParameterizedSymbol ps) {
        return (ps instanceof InputSymbol);
    }
    
}
