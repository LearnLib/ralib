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

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.theory.Memorables;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

import java.util.List;


/**
 * A cell of an observation table.
 *
 * @author falk
 */
final class Cell {

    private final Word<PSymbolInstance> prefix;

    private final SymbolicSuffix suffix;

    private final SDT sdt;

    private Cell(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, SDT sdt) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.sdt = sdt;
    }

    /**
     * checks whether the sdts of the two cells are equal
     *
     * @param other
     * @return
     */
    boolean isEquivalentTo(Cell other, SDTRelabeling renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }

        boolean check = this.suffix.equals(other.suffix) &&
                Memorables.relabel(this.getMemorableValues(), renaming).equals(other.getMemorableValues()) &&
                other.sdt.isEquivalent(this.sdt, renaming);
//        LOGGER.trace(this.sdt + "\nVS\n" + other.sdt + "\n");
//        LOGGER.trace(this.suffix + "    " + other.suffix);
//        LOGGER.trace(this.suffix.equals(other.suffix) + " " + this.parsInVars.relabel(renaming).equals(other.parsInVars) + " " + this.sdt.isEquivalent(other.sdt, renaming));

//        System.out.println("EQ: " + this.prefix + " . " + this.suffix + " : " + check);
        return check;
    }

    /**
     *
     * @param other
     * @return
     */
    boolean couldBeEquivalentTo(Cell other) {
        return Memorables.typedSize(this.getMemorableValues()).equals(Memorables.typedSize(other.getMemorableValues()));
        //TODO: call preliminary checks on SDTs
    }

    /**
     * computes a cell for a prefix and a symbolic suffix.
     *
     * @param oracle
     * @param prefix
     * @param suffix
     * @return
     */
    static Cell computeCell(TreeOracle oracle, Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        //System.out.println("START: computecell for " + prefix.toString() + "   .    " + suffix.toString());
        TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
        Cell c = new Cell(prefix, suffix, tqr.sdt());
        //System.out.println("END: computecell " + c.toString());
        return c;
    }

    SymbolicSuffix getSuffix() {
        return this.suffix;
    }

    Word<PSymbolInstance> getPrefix() {
        return this.prefix;
    }

    SDT getSDT() {
        return this.sdt;
    }

    List<DataValue> getMemorableValues() {
        return this.sdt.getDataValues();
    }

    @Override
    public String toString() {
        return "Cell: " + this.prefix + " / " + this.suffix + " :\n" + this.sdt;
    }

    void toString(StringBuilder sb) {
        sb.append("**** Cell: ").append(this.suffix).append(" : ").append(this.sdt).append("\n");
    }

    boolean isAccepting() {
        return this.sdt.isAccepting();
    }

}
