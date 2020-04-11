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
package de.learnlib.ralib.oracles.mto;

import java.util.List;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * An sdt constructor by default should only support single threaded operation. 
 * @author falk
 */
public interface SDTConstructor {
    
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix,
            WordValuation values, PIV piv,
            Constants constants, SuffixValuation suffixValues);
    
    public MultiTheoryBranching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, ParValuation pval, 
            List<SDTGuard> guards, SDT ... sdts);    
}
