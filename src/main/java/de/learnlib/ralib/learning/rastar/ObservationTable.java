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

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * An observation table.
 *
 * @author falk
 */
class ObservationTable {

    private final List<SymbolicSuffix> suffixes = new LinkedList<>();

    private final Map<Word<PSymbolInstance>, Component> components
            = new LinkedHashMap<>();

    private final Deque<SymbolicSuffix> newSuffixes = new LinkedList<>();

    private final Deque<Word<PSymbolInstance>> newPrefixes = new LinkedList<>();

    private final Deque<Component> newComponents = new LinkedList<>();

    private final TreeOracle oracle;

    private final ParameterizedSymbol[] inputs;

    private final boolean ioMode;

    private final Constants consts;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationTable.class);

    public ObservationTable(TreeOracle oracle, boolean ioMode,
            Constants consts, ParameterizedSymbol ... inputs) {
        this.oracle = oracle;
        this.inputs = inputs;
        this.ioMode = ioMode;
        this.consts = consts;
        this.restrictionBuilder = oracle.getRestrictionBuilder();
    }

    void addComponent(Component c) {
        LOGGER.info(Category.EVENT, "Queueing component for obs: {}", c);
        newComponents.add(c);
    }

    void addSuffix(SymbolicSuffix suffix) {
        LOGGER.info(Category.EVENT, "Queueing suffix for obs: {}", suffix);
        newSuffixes.add(suffix);
    }

    void addPrefix(Word<PSymbolInstance> prefix) {
        LOGGER.info(Category.EVENT, "Queueing prefix for obs: {}", prefix);
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

        //AutomatonBuilder ab = new AutomatonBuilder(getComponents(), new Constants());
        //Hypothesis hyp = ab.toRegisterAutomaton();
        //FIXME: the default logging appender cannot log models and data structures
        //System.out.println(hyp.toString());
        return checkVariableConsistency();
    }

    private boolean checkBranchingCompleteness() {
        LOGGER.info(Category.PHASE, "Checking Branching Completeness");
        boolean ret = true;
        for (Component c : components.values()) {
            boolean ub = c.updateBranching(oracle);
            ret = ret && ub;
        }
        return ret;
    }

    private boolean checkVariableConsistency() {
        LOGGER.info(Category.PHASE, "Checking Variable Consistency");
        for (Component c : components.values()) {
            if (!c.checkVariableConsistency()) {
                return false;
            }
        }
        return true;
    }

    private void processNewSuffix() {
        SymbolicSuffix suffix = newSuffixes.poll();
        LOGGER.info(Category.EVENT, "Adding suffix to obs: {}", suffix);
        //System.out.println("Adding suffix to obs: " + suffix);
        suffixes.add(suffix);
        for (Component c : components.values()) {
            c.addSuffix(suffix, oracle);
        }
    }

    private void processNewPrefix() {
        Word<PSymbolInstance> prefix = newPrefixes.poll();
        LOGGER.info(Category.EVENT, "Adding prefix to obs: {}", prefix);
        Row r = Row.computeRow(oracle, prefix, suffixes, ioMode);
        for (Component c : components.values()) {
            if (c.addRow(r)) {
                return;
            }
        }
        Component c = new Component(r, this, ioMode, consts, restrictionBuilder);
        addComponent(c);
    }

    private void processNewComponent() {
        Component c = newComponents.poll();
        //System.out.println("Adding component to obs: " + c);
        components.put(c.getAccessSequence(), c);
        c.start(oracle, inputs);
    }

    Map<Word<PSymbolInstance>, Component> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OBS *******************************************************************\n");
        for (Component c : getComponents().values()) {
            c.toString(sb);
        }
        sb.append("***********************************************************************\n");
        return sb.toString();
    }

}
