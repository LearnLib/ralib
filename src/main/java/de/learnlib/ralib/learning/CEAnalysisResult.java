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

import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * Container for output of an analysis of a counterexample.
 * 
 * @author falk
 */
public class CEAnalysisResult {
    
    private final Word<PSymbolInstance> prefix;
    
    private final SymbolicSuffix suffix;

    public CEAnalysisResult(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }
    
    public CEAnalysisResult(Word<PSymbolInstance> prefix) {
        this(prefix, null);
    }
    
    public boolean hasSuffix() {
        return (getSuffix() != null);
    }

    /**
     * @return the prefix
     */
    public Word<PSymbolInstance> getPrefix() {
        return prefix;
    }

    /**
     * @return the suffix
     */
    public SymbolicSuffix getSuffix() {
        return suffix;
    }
        
}
