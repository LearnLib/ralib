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

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.List;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class RaStar {
    
    private List<SymbolicSuffix> suffixes;
    
    private Oracle oracle;
    
    // public interface
    
    public void learn() {
        
    }
    
    public boolean addCounterexample(Word<PSymbolInstance> ce) {
        throw new UnsupportedOperationException("not implemented yet.");
    }
    
    public RegisterAutomaton getHypothesis() {
        throw new UnsupportedOperationException("not implemented yet.");        
    }
    
    // internal interface 
    
    void addShortPrefix(Row r) {
        
    }
    
    /**
     * adds a new prefix to the table.
     * 
     * @param prefix 
     */
    void addPrefix(Word<PSymbolInstance> prefix) {
        //Row r = new Row(prefix,);
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    
    
    //private void computeCell(Word<PSymbolInstance> prefix, SymbolicSuffix prefix) 
    
            
}
