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
package de.learnlib.ralib.oracles.io;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public interface IOOracle {
    
	/**
	 * Transforms a i/o query into a valid trace of the system by preserving the input symbols, 
	 * while relabeling their parameters and changing the outputs according to the system's response. 
	 * </p>
	 * Used for counterexample reduction operations, wherein a section of a trace is cut out.
	 * What is left might not belong to the system's traces. 
	 * This operation "repairs" the trace.
	 * 
	 * @param query - an input/output word
	 *  
	 */
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query);
    
    /**
     * Batch traces. By default, traces are executed sequentially, one at a time.
     */
    public default List<Word<PSymbolInstance>> traces(List<Word<PSymbolInstance>> queries) {
    	List<Word<PSymbolInstance>> traces = new ArrayList<>(queries.size());
    	for (Word<PSymbolInstance> q : queries) {
    		Word<PSymbolInstance> trace = this.trace(q);
    		traces.add(trace);
    	}
    	
    	return traces;
    }

}
