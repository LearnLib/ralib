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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class NewIORandomWalk implements IOEquivalenceOracle {

    private final Random rand;
    private RegisterAutomaton hyp;
    private final DataWordSUL target;
    private final ParameterizedSymbol[] inputs;
    private final boolean uniform;
    private final double resetProbability;
    private final long maxRuns;
    private final boolean resetRuns;
    private long runs;
    private final Constants constants;
    private final Map<DataType, Theory> teachers;
    
    private static LearnLogger log = LearnLogger.getLogger(NewIORandomWalk.class);

    private ParameterizedSymbol error = null;
	private HypVerifier hypVerifier;
	private RandomWalkRunGenerator runGenerator;
    
    /**
     * creates an IO random walk
     * 
     * @param rand Random 
     * @param target SUL
     * @param uniform draw from inputs uniformly or according to possible concrete instances
     * @param resetProbability prob. to reset after each step
     * @param newDataProbability prob. for using a fresh data value
     * @param maxRuns max. number of runs
     * @param maxDepth max length of a run
     * @param constants constants
     * @param resetRuns reset runs every time findCounterExample is called
     * @param teachers teachers for creating fresh data values
     * @param inputs inputs
     */
    public NewIORandomWalk(Random rand, DataWordSUL target, boolean uniform,
            double resetProbability, double newDataProbability, long maxRuns, int maxDepth, Constants constants,
            boolean resetRuns, Map<DataType, Theory> teachers, ParameterizedSymbol... inputs) {

        this.resetRuns = resetRuns;
        this.rand = rand;
        this.target = target;
        this.hypVerifier = new IOHypVerifier(teachers, constants);
        this.inputs = inputs;
        this.uniform = uniform;
        this.resetProbability = resetProbability;
        this.maxRuns = maxRuns;
        this.constants = constants;
        this.teachers = teachers;
        this.runGenerator = new RandomWalkRunGenerator(maxDepth, newDataProbability, rand);
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(
            RegisterAutomaton a, Collection<? extends PSymbolInstance> clctn) {

        if (clctn != null && !clctn.isEmpty()) {
            log.warning("set of inputs is ignored by this equivalence oracle");
        }
        
        this.hyp = a;
        // reset the counter for number of runs?
        if (resetRuns) {
            runs = 0;
        }
        // find counterexample ...
        while (runs < maxRuns) {
            Word<PSymbolInstance> ce = run();
            if (ce != null) {
                return new DefaultQuery<>(ce, true);
            }
        }
        return null;
    }

    private Word<PSymbolInstance> run() {
        runs++;
        target.pre();
        Word<PSymbolInstance> run = Word.epsilon();
        PSymbolInstance out = null;
        List<Transition> transitions = this.runGenerator.generateRun(this.hyp);
        transitions.removeIf( transition -> transition instanceof OutputTransition); // we remove output transitions
        
        for (Transition transition : transitions) {

        	VarValuation regValuation = hyp.getRegisterValuation(run);
        	PSymbolInstance next = this.computeRandomValuationForTrans(run, transition.getLabel(), teachers, 
					transition.getGuard(), regValuation, new ParValuation(), constants, 0);
            out = null;
            run = run.append(next);
            out = target.step(next);
            run = run.append(out);

            if (this.hypVerifier.isCEForHyp(run, hyp) != null) {
                log.log(Level.FINE, "Run with CE: {0}", run);     
                System.out.format("Run with CE: {0}", run);
                target.post();
                return run;
            }
        } 
//        System.out.println(run);
        log.log(Level.FINE, "Run /wo CE: {0}", run);
        target.post();
        return null;
    }
    
    

	private PSymbolInstance computeRandomValuationForTrans(Word<PSymbolInstance> run, ParameterizedSymbol ps, Map<DataType, Theory> teachers, 
			TransitionGuard transGuard, VarValuation regValuation, ParValuation currentValuation, Constants consts, int crtParam) {
		if (crtParam == ps.getArity()) {
			transGuard.getCondition();
			if (transGuard.isSatisfied(regValuation, currentValuation, consts)) {
				return new PSymbolInstance(ps, currentValuation.values().toArray(new DataValue[]{}));
			} else {
				return null;
			}
			
		} else {
			DataType paramType = ps.getPtypes()[crtParam];
			Set<DataValue<Object>> valsOfType = DataWords.valSet(run, paramType);
			valsOfType.addAll(currentValuation.values(paramType));
			Collection<DataValue> nextValues = teachers.get(paramType).getAllNextValues(new ArrayList(valsOfType));
			List<DataValue> shuffledValues = new ArrayList(nextValues);
			Parameter param = new SymbolicDataValue.Parameter(paramType, crtParam + 1);
			Collections.shuffle(shuffledValues, this.rand);
			for (DataValue value : shuffledValues) {
				currentValuation.put(param, value);
				PSymbolInstance inst = computeRandomValuationForTrans(run, ps, teachers, transGuard, regValuation, currentValuation, consts, crtParam+1);
				if (inst != null)
					return inst;
			}
		}
		
		return null;
	}
    
    public void setError(ParameterizedSymbol error) {
        this.error = error;
    }
}
