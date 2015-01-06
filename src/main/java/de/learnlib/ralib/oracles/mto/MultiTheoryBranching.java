/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryBranching implements Branching {

    public static class Node {
        private final Parameter parameter;
        private final Map<DataValue, Node> next = new HashMap<>();
        private final Map<DataValue, SDTGuard> guards = new HashMap<>();

        public Node(Parameter parameter) {
            this.parameter = parameter;
        }
 
    }
    
    private final Word<PSymbolInstance> prefix;
    
    private final PSymbolInstance action;

    public MultiTheoryBranching(Word<PSymbolInstance> prefix, PSymbolInstance action) {
        this.prefix = prefix;
        this.action = action;
    }
    
    
    @Override
    public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
}
