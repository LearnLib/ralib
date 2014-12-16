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

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.VarsToInternalRegs;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.theory.Branching;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TreeOracle;
import de.learnlib.ralib.theory.TreeQueryResult;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class LoginExampleTheory implements Theory<Integer> {

    private final DataType type;

    public LoginExampleTheory(DataType type) {
        this.type = type;
    }
        
    private int nextFree = 1;

    @Override
    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
        return new DataValue<>(type, nextFree++);
    }

    @Override
    public List<DataValue<Integer>> getPotential(Collection<DataValue<Integer>> vals) {
        ArrayList<DataValue<Integer>> pot = new ArrayList<>(new LinkedHashSet<>(vals));
        return pot;       
    }

    @Override
    public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, ParsInVars piv, VarValuation suffixValues, TreeOracle oracle) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Branching getInitialBranching(SymbolicDecisionTree merged, VarsToInternalRegs vtir, ParValuation... parval) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
