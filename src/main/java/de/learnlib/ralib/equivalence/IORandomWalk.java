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
	public IORandomWalk(Random rand, DataWordSUL target, boolean uniform,
            double stopProbability, double regProb, double hisProb, double relatedProb, long maxRuns, int maxDepth, Constants constants,
            boolean resetRuns, Map<DataType, Theory> teachers, ParameterizedSymbol... inputs) {
		this(rand, new BasicSULOracle(target, SpecialSymbols.ERROR), uniform, stopProbability, regProb, hisProb, relatedProb, maxRuns, maxDepth,
				constants,resetRuns, teachers, (trace) -> trace, inputs);
	}
	
	
	/**
     * creates an IO random walk
     * 
     */
    public IORandomWalk(Random rand, IOOracle target, boolean uniform,
            double stopProbability, double regProb, double hisProb, double relatedProb, long maxRuns, int maxDepth, Constants constants,
            boolean resetRuns, Map<DataType, Theory> teachers, TraceCanonizer traceCanonizer, ParameterizedSymbol... inputs) 
    {
    	super(target, traceCanonizer, maxRuns, resetRuns);
    	
		TraceGenerator traceGen = new IORandomWalkTraceGenerator(rand, uniform, stopProbability, regProb, hisProb, relatedProb, maxDepth, constants, teachers, inputs);
		super.setTraceGenerator(traceGen);
    }


	public IORandomWalk(Random random, DataWordSUL sul, boolean drawUniformly, double resetProb, double freshProb, long maxRuns, int maxDepth,
			Constants consts, boolean c, Map<DataType, Theory> teachers, ParameterizedSymbol ... inputSymbols) {
		this(random, sul, drawUniformly, resetProb, (1-freshProb)/3, (1-freshProb)/3, (1-freshProb)/3, maxRuns, maxDepth, 
				consts, c, teachers, inputSymbols);
	}
}
