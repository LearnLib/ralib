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

import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
class Observations {
    
    private final List<SymbolicSuffix> suffixes = new LinkedList<>();
    
    private final Map<Word<PSymbolInstance>, Component> components
            = new LinkedHashMap<>();
    
    private final Deque<SymbolicSuffix> newSuffixes = new LinkedList<>();
    
    private final Deque<Word<PSymbolInstance>> newPrefixes = new LinkedList<>();
    
    private final Deque<Component> newComponents = new LinkedList<>();

    private final TreeOracle oracle;
    
    private final ParameterizedSymbol[] inputs;
    
    public Observations(TreeOracle oracle, ParameterizedSymbol ... inputs) {
        this.oracle = oracle;
        this.inputs = inputs;
    }
        
    void addComponent(Component c) {
        System.out.println("Queueing component for obs: " + c);
        newComponents.add(c);
    }
    
    void addSuffix(SymbolicSuffix suffix) {
        System.out.println("Queueing suffix for obs: " + suffix);
        newSuffixes.add(suffix);
    }
    
    void addPrefix(Word<PSymbolInstance> prefix) {
        System.out.println("Queueing prefix for obs: " + prefix);
        newPrefixes.add(prefix);
    }
    
    boolean complete() {        
        if (!newComponents.isEmpty()) {
            processNewComponent();
            return false;
        }
        
        if (!newPrefixes.isEmpty()) {
            processNewPrefix();
            return false;
        }
        
        if (!newSuffixes.isEmpty()) {
            processNewSuffix();
            checkBranchingCompleteness();
            return false;
        }
        
        if (!checkVariableConsistency()) {
            return false;
        }
        
        return true;
    }

    private boolean checkBranchingCompleteness() {
        boolean ret = true;
        for (Component c : components.values()) {
            boolean ub = c.updateBranching(oracle);
            ret = ret && ub;
        }       
        return ret;
    }
    
    private boolean checkVariableConsistency() {
        for (Component c : components.values()) {
            if (!c.checkVariableConsistency()) {
                return false;
            }
        }
        return true;
    }

    private void processNewSuffix() {
        SymbolicSuffix suffix = newSuffixes.poll();
        System.out.println("Adding suffix to obs: " + suffix);
        suffixes.add(suffix);
        for (Component c : components.values()) {
            c.addSuffix(suffix, oracle);
        }
    }

    private void processNewPrefix() {
        Word<PSymbolInstance> prefix = newPrefixes.poll();
        System.out.println("Adding prefix to obs: " + prefix);
        Row r = Row.computeRow(oracle, prefix, suffixes);
        for (Component c : components.values()) {
            if (c.addRow(r)) {
                return;
            }
        }
        Component c = new Component(r, this);
        addComponent(c);
    }

    private void processNewComponent() {
        Component c = newComponents.poll();
        System.out.println("Adding component to obs: " + c);
        components.put(c.getAccessSequence(), c);
        c.start(oracle, inputs);
    }

    Map<Word<PSymbolInstance>, Component> getComponents() {
        return components;
    }

}
