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
package de.learnlib.ralib.learning;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * Container for output of an analysis of a counterexample.
 * 
 * @author falk
 */
public class CEAnalysisResult {
    
    private final Word<PSymbolInstance> prefix;
    
    private final GeneralizedSymbolicSuffix suffix;

    public CEAnalysisResult(Word<PSymbolInstance> prefix, 
            GeneralizedSymbolicSuffix suffix) {
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
    public GeneralizedSymbolicSuffix getSuffix() {
        return suffix;
    }
        
}
