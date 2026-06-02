/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.theory.Memorables;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * A row in an observation table.
 *
 * @author falk
 */
public class Row implements PrefixContainer {

    private final Word<PSymbolInstance> prefix;

    private final Map<SymbolicSuffix, Cell> cells;

    private static final Logger LOGGER = LoggerFactory.getLogger(Row.class);

    private final boolean ioMode;

    private Row(Word<PSymbolInstance> prefix, boolean ioMode) {
        this.prefix = prefix;
        this.cells = new LinkedHashMap<>();
        this.ioMode = ioMode;
    }

    void addSuffix(SymbolicSuffix suffix, TreeOracle oracle) {
        if (ioMode && !suffix.getActions().isEmpty()) {
            // error row
            if (!getPrefix().isEmpty() && !isAccepting()) {
                LOGGER.info(Category.EVENT, "Not adding suffix {} to error row {}", suffix, getPrefix());
                return;
            }
            // unmatching suffix
            if ((getPrefix().isEmpty() && (suffix.getActions().firstSymbol() instanceof OutputSymbol))
                    || (!prefix.isEmpty() && prefix.lastSymbol().getBaseSymbol() instanceof InputSymbol == suffix.getActions().firstSymbol() instanceof InputSymbol)) {
                LOGGER.info(Category.EVENT, "Not adding suffix {} to unmatching row {}", suffix, getPrefix());
                return;
            }
        }

        Cell c = Cell.computeCell(oracle, prefix, suffix);
        addCell(c);
    }

    private void addCell(Cell c) {
        assert c.getPrefix().equals(this.prefix);
        assert !this.cells.containsKey(c.getSuffix());
        this.cells.put(c.getSuffix(), c);
    }

    SymbolicSuffix getSuffixForMemorable(DataValue d) {
        return cells.entrySet().stream()
                .filter(e -> e.getValue().getMemorableValues().contains(d))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("This line is not supposed to be reached."))
                .getKey();
    }

    SDT[] getSDTsForInitialSymbol(ParameterizedSymbol ps) {
        List<SDT> sdts = new ArrayList<>();
        for (Entry<SymbolicSuffix, Cell> c : cells.entrySet()) {
            Word<ParameterizedSymbol> acts = c.getKey().getActions();
            if (acts.length() > 0 && acts.firstSymbol().equals(ps)) {
//                System.out.println("Using " + c.getKey() + " for branching of " + ps + " after " + prefix);
                sdts.add(c.getValue().getSDT());
            }
        }
        return sdts.toArray(new SDT[]{});
    }

    public Set<DataValue> memorableValues() {
        return cells.values().stream()
                .flatMap(c -> c.getMemorableValues().stream() )
                .collect(Collectors.toSet());
    }

    @Override
    public RegisterAssignment getAssignment() {
        RegisterAssignment ra = new RegisterAssignment();
        SymbolicDataValueGenerator.RegisterGenerator regGen =
                new SymbolicDataValueGenerator.RegisterGenerator();

        this.memorableValues().forEach(
                dv -> ra.put(dv, regGen.next(dv.getDataType()))
        );

        return ra;
    }

    @Override
    public Word<PSymbolInstance> getPrefix() {
        return this.prefix;
    }

    /**
     * checks rows for equality (disregarding of the prefix!). It is assumed
     * that both rows have the same set of suffixes.
     *
     * @param other
     * @return true if rows are equal
     */
    boolean isEquivalentTo(Row other, SDTRelabeling renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }

        if (!Memorables.relabel(this.memorableValues(), renaming).equals(other.memorableValues())) {
            return false;
        }

        for (Map.Entry<SymbolicSuffix, Cell> entry : this.cells.entrySet()) {
            Cell c1 = entry.getValue();
            Cell c2 = other.cells.get(entry.getKey());

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!c1.isEquivalentTo(c2, renaming)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param other
     * @return
     */
    boolean couldBeEquivalentTo(Row other) {
        if (!Memorables.typedSize(this.memorableValues()).equals(Memorables.typedSize(other.memorableValues()))) {
            return false;
        }

        for (Map.Entry<SymbolicSuffix, Cell> entry : this.cells.entrySet()) {
            Cell c1 = entry.getValue();
            Cell c2 = other.cells.get(entry.getKey());

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!c1.couldBeEquivalentTo(c2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * computes a new row object from a prefix and a set of symbolic suffixes.
     *
     * @param oracle
     * @param prefix
     * @param suffixes
     * @return
     */
    static Row computeRow(TreeOracle oracle,
            Word<PSymbolInstance> prefix, List<SymbolicSuffix> suffixes, boolean ioMode) {

        Row r = new Row(prefix, ioMode);
        for (SymbolicSuffix s : suffixes) {
            if (ioMode && s.getActions().length() > 0) {
                // error row
                if (r.getPrefix().length() > 0 && !r.isAccepting()) {
                    LOGGER.info(Category.EVENT, "Not adding suffix {} to error row {}", s, r.getPrefix());
                    continue;
                }
                // unmatching suffix
                if ((r.getPrefix().length() < 1 && (s.getActions().firstSymbol() instanceof OutputSymbol))
                        || (prefix.length() > 0 && prefix.lastSymbol().getBaseSymbol() instanceof InputSymbol == s.getActions().firstSymbol() instanceof InputSymbol)) {
                    LOGGER.info(Category.EVENT, "Not adding suffix {} to unmatching row {}", s, r.getPrefix());
                    continue;
                }
            }
            r.addCell(Cell.computeCell(oracle, prefix, s));
        }
        return r;
    }

    boolean isAccepting() {
        Cell c = this.cells.get(RaStar.EMPTY_SUFFIX);
        return c.isAccepting();
    }

    @Override
    public String toString() {
        return this.prefix.toString();
    }

    void toString(StringBuilder sb) {
        sb.append("****** ROW: ").append(prefix).append("\n");
        for (Entry<SymbolicSuffix, Cell> c : this.cells.entrySet()) {
            c.getValue().toString(sb);
        }
    }

}
