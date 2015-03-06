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

package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.ParsInVars;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.List;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public interface SDTConstructor {
    
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values, ParsInVars piv, ParsInVars pout,
            SuffixValuation suffixValues);
    
    public MultiTheoryBranching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, ParValuation pval, 
            List<SDTGuard> guards, SDT ... sdts);    

}
