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

package de.learnlib.ralib.words;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.trees.SymbolicSuffix;
import net.automatalib.words.Word;
import org.testng.annotations.Test;


/**
 *
 * @author falk
 */
@Test
public class TestWords {
    
    
    
    public void testSymbolicSuffix() {
        
        DataType intType = new DataType("int") {};
                
        ParameterizedSymbol a = new ParameterizedSymbol("a", new DataType[]{intType});
        
        DataValue<Integer> i1 = new DataValue<>(intType, 1);
        DataValue<Integer> i2 = new DataValue<>(intType, 2);
        DataValue<Integer> i3 = new DataValue<>(intType, 3);
        
        PSymbolInstance[] prefixSymbols = new PSymbolInstance[] {
            new PSymbolInstance(a, i1),
            new PSymbolInstance(a, i3)
        };
        
        PSymbolInstance[] suffixSymbols = new PSymbolInstance[] {
            new PSymbolInstance(a, i1),
            new PSymbolInstance(a, i2),
            new PSymbolInstance(a, i2),
            new PSymbolInstance(a, i1)
        };
        
        Word<PSymbolInstance> prefix = Word.fromSymbols(prefixSymbols);
        Word<PSymbolInstance> suffix = Word.fromSymbols(suffixSymbols);
        
        System.out.println(prefix);
        System.out.println(suffix);
        
        SymbolicSuffix sym = new SymbolicSuffix(prefix, suffix);
        
        System.out.println(sym);
    }
    
}
