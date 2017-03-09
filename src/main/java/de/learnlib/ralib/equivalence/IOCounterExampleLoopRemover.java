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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.equivalence.HypVerifier.PositiveResult;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class IOCounterExampleLoopRemover implements IOCounterExampleOptimizer {

    private static class Loop {

        private final int min;
        private final int max;

        public Loop(int min, int max) {
            this.min = min;
            this.max = max;
        }
        
        public String toString() {
        	return "min: " + min + " max:" + max;
        }
    }

    private final IOOracle sulOracle;
    private RegisterAutomaton hypothesis;
	private HypVerifier verifier;

    public IOCounterExampleLoopRemover(IOOracle sulOracle, HypVerifier verifier) {
        this.sulOracle = sulOracle;
        this.verifier = verifier;
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> optimizeCE(Word<PSymbolInstance> ce, Hypothesis hyp) {
        return new DefaultQuery<>(removeLoops(ce, hyp), true);
    }

    private Word<PSymbolInstance> removeLoops(
            Word<PSymbolInstance> ce, Hypothesis hyp) {

        this.hypothesis = hyp;
        Map<Integer, List<Loop>> loops = new LinkedHashMap<>();
        List<Integer> sizes = new ArrayList<>();
        RALocation[] trace = execute(ce);
        for (int i = 0; i < trace.length; i++) {
            for (int j = i + 1; j < trace.length; j++) {
                if (!trace[i].equals(trace[j])) {
                    continue;
                }
                int length = j - i;
                //System.out.println("Found loop of length " + length);
                List<Loop> list = loops.get(length);
                if (list == null) {
                    list = new LinkedList<>();
                    loops.put(length, list);
                    sizes.add(length);
                }
                list.add(new Loop(i, j));
            }
        }

        Collections.sort(sizes);
        Collections.reverse(sizes);
        for (Integer i : sizes) {
            //System.out.println("Checking length " + i);            
            List<Loop> list = loops.get(i);
            for (Loop loop : list) {
                Word<PSymbolInstance> shorter = shorten(ce, loop);
                System.out.println("shorter:" + shorter);
                Word<PSymbolInstance> candidate = sulOracle.trace(shorter);
                System.out.println("candidate:" + candidate);
                PositiveResult check = verifier.isCEForHyp(candidate, this.hypothesis);
                if (check != null) {
                    return removeLoops(check.testPrefix, hyp);
                }
            }
        }
        return sulOracle.trace(ce);
    }

    private Word<PSymbolInstance> shorten(Word<PSymbolInstance> ce, Loop loop) {

        Word<PSymbolInstance> prefix = ce.prefix(loop.min * 2);
        Word<PSymbolInstance> suffix = ce.subWord(loop.max * 2, ce.length());
        return prefix.concat(suffix);
    }

    private RALocation[] execute(Word<PSymbolInstance> ce) {
        List<RALocation> trace = new ArrayList<>();
        for (int i = 0; i < ce.length(); i += 2) {
            Word<PSymbolInstance> prefix = ce.prefix(i);
            trace.add(hypothesis.getLocation(prefix));
        }

        return trace.toArray(new RALocation[]{});
    }

}
