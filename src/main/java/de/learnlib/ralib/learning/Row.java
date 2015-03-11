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
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * A row in an observation table.
 * 
 * @author falk
 */
class Row {

    private final Word<PSymbolInstance> prefix;

    private final Map<SymbolicSuffix, Cell> cells;

    private final PIV memorable = new PIV();

    private final RegisterGenerator regGen = new RegisterGenerator();

    private static final LearnLogger log = LearnLogger.getLogger(Row.class);
    
    private Row (Word<PSymbolInstance> prefix) {
        this.prefix = prefix;
        this.cells = new LinkedHashMap<>();        
    }
    
    private Row(Word<PSymbolInstance> prefix, List<Cell> cells) {
        this(prefix);
        
        for (Cell c : cells) {
            this.cells.put(c.getSuffix(), c);
        }
    }
   
    void addSuffix(SymbolicSuffix suffix, TreeOracle oracle) {
        Cell c = Cell.computeCell(oracle, prefix, suffix);
        addCell(c);
    }
    
    private void addCell(Cell c) {
        
        assert c.getPrefix().equals(this.prefix);
        assert !this.cells.containsKey(c.getSuffix());
        
        // make sure that pars-in-vars is consistant with 
        // existing cells in his row
        PIV cpv = c.getParsInVars();
        VarMapping relabelling = new VarMapping();
        for (Entry<Parameter, Register> e : cpv.entrySet()) {
            Register r = this.memorable.get(e.getKey());
            if (r == null) {
                r = regGen.next(e.getKey().getType());
                memorable.put(e.getKey(), r);
            }
            relabelling.put(e.getValue(), r);
        }

        this.cells.put(c.getSuffix(), c.relabel(relabelling));
    }

    SymbolicSuffix getSuffixForMemorable(Parameter p) {
        for (Entry<SymbolicSuffix, Cell> c: cells.entrySet()) {
            if (c.getValue().getParsInVars().containsKey(p)) {
                return c.getKey();
            }
        }
        
        throw new IllegalStateException("This line is not supposed to be reached.");
    }
    
    SymbolicDecisionTree[] getSDTsForInitialSymbol(ParameterizedSymbol ps) {
        List<SymbolicDecisionTree> sdts = new ArrayList<>();
        for (Entry<SymbolicSuffix, Cell> c: cells.entrySet()) {
            Word<ParameterizedSymbol> acts = c.getKey().getActions();
            if (acts.length() > 0 && acts.firstSymbol().equals(ps)) {
                sdts.add(c.getValue().getSDT());
            }
        } 
        return sdts.toArray(new SymbolicDecisionTree[] {});
    }
    
    PIV getParsInVars() {
        return this.memorable;
    }

    Word<PSymbolInstance> getPrefix() {
        return this.prefix;
    }
    
    
    /**
     * checks rows for equality (disregarding of the prefix!). It is assumed
     * that both rows have the same set of suffixes.
     *
     * @param other
     * @return true if rows are equal
     */
    boolean isEquivalentTo(Row other, VarMapping renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }
        
        if (!this.memorable.relabel(renaming).equals(other.memorable)) {
            return false;
        }
        
        for (SymbolicSuffix s : this.cells.keySet()) {
            Cell c1 = this.cells.get(s);
            Cell c2 = other.cells.get(s);
            if (!c1.isEquivalentTo(c2, renaming)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param r
     * @return
     */
    boolean couldBeEquivalentTo(Row other) {
        if (!this.memorable.typedSize().equals(other.memorable.typedSize())) {
            return false;
        }

        for (SymbolicSuffix s : this.cells.keySet()) {
            Cell c1 = this.cells.get(s);
            Cell c2 = other.cells.get(s);
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
            Word<PSymbolInstance> prefix, List<SymbolicSuffix> suffixes) {

        Row r = new Row(prefix);    
        for (SymbolicSuffix s : suffixes) {
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
