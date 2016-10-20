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

import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public abstract class IOOracle extends QueryCounter {
    
	/**
	 * Transforms a query into a trace of the system by preserving the input 
	 * methods, while relabeling the parameters and changing the outputs according
	 * to the system behavior for the inputs. 
	 * </p>
	 * Used for counterexample reduction operations, whereby a section of a trace is cut out.
	 * What is left might not belong to the system's traces. This operation "repairs" the trace.
	 *  
	 */
    public abstract Word<PSymbolInstance> trace(Word<PSymbolInstance> query);

}
