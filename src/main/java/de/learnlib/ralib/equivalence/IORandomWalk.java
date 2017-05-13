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
package de.learnlib.ralib.equivalence;

import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
//
//IORandomWalk iowalk = new IORandomWalk(random,
//        sul,
//        false, // do not draw symbols uniformly 
//        0.1, // reset probability 
//        0.8, // prob. of choosing a fresh data value
//        1000, // 1000 runs 
//        100, // max depth
//        consts,
//        false, // reset runs 
//        teachers,
//        sul.getInputSymbols());
public class IORandomWalk extends BoundedIOEquivalenceOracle implements IOEquivalenceOracle {
	
	/**
     * creates an IO random walk
     * 
     */
    public IORandomWalk(Random rand, IOOracle target, 
            double stopProbability, int maxDepth, InputSelector inpSelector, long maxRuns,  Constants constants,
            boolean resetRuns, Map<DataType, Theory> teachers, TraceCanonizer traceCanonizer, ParameterizedSymbol... inputs) 
    {
    	super(target, traceCanonizer, maxRuns, resetRuns);
		TraceGenerator traceGen = new IORandomWalkTraceGenerator(rand,  
				stopProbability, maxDepth, inpSelector, constants, teachers, inputs);
		super.setTraceGenerator(traceGen);
    }


    // kept so we don't break any tests
	public IORandomWalk(Random random, DataWordSUL sul, boolean drawUniformly, double resetProb, double freshProb, long maxRuns, int maxDepth,
			Constants consts, boolean c, Map<DataType, Theory> teachers, ParameterizedSymbol ... inputSymbols) {
		this(random, new BasicSULOracle(sul, SpecialSymbols.ERROR), resetProb, maxDepth,
				new RandomSymbolSelector(random, teachers, consts, drawUniformly, (1-freshProb)/3, (1-freshProb)/3, (1-freshProb)/3, inputSymbols)
				, maxRuns, consts, c, teachers, (trace) -> trace, inputSymbols);
	}
}
