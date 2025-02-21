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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class IORandomWalk implements IOEquivalenceOracle {

    private final Random rand;
    private RegisterAutomaton hyp;
    private final DataWordSUL target;
    private final ParameterizedSymbol[] inputs;
    private final boolean uniform;
    private final double resetProbability;
    private final double newDataProbability;
    private final long maxRuns;
    private final boolean resetRuns;
    private long runs;
    private final int maxDepth;
    private final boolean seedTransitions;
    private final Constants constants;
    private final Map<DataType, Theory> teachers;

    private static Logger LOGGER = LoggerFactory.getLogger(IORandomWalk.class);

    private ParameterizedSymbol error = null;

    private ArrayList<Word<PSymbolInstance>> seeds = null;

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
    public IORandomWalk(Random rand, DataWordSUL target, boolean uniform,
            double resetProbability, double newDataProbability, long maxRuns, int maxDepth, Constants constants,
            boolean resetRuns, boolean seedTransitions, Map<DataType, Theory> teachers, ParameterizedSymbol... inputs) {

        this.resetRuns = resetRuns;
        this.rand = rand;
        this.target = target;
        this.inputs = inputs;
        this.uniform = uniform;
        this.resetProbability = resetProbability;
        this.maxRuns = maxRuns;
        this.constants = constants;
        this.maxDepth = maxDepth;
        this.teachers = teachers;
        this.newDataProbability = newDataProbability;
        this.seedTransitions = seedTransitions;
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(
            RegisterAutomaton a, Collection<? extends PSymbolInstance> clctn) {

        if (clctn != null && !clctn.isEmpty()) {
            LOGGER.warn(Category.QUERY, "set of inputs is ignored by this equivalence oracle");
        }

        if (this.seedTransitions) {
            if ((a instanceof Hypothesis)) {
                Hypothesis hypothesis = (Hypothesis) a;
                seeds = new ArrayList<>();
                for (Word<PSymbolInstance> u : hypothesis.getTransitionSequences().values()) {
                    if (u.lastSymbol().getBaseSymbol() instanceof OutputSymbol) {
                        seeds.add(u);
                    }
                }
            } else {
                seeds = null;
            }
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
                return new DefaultQuery<PSymbolInstance, Boolean>(ce, true);
            }
        }
        return null;
    }

    private Word<PSymbolInstance> run() {
        int depth = 0;
        runs++;
        target.pre();
        Word<PSymbolInstance> run = Word.epsilon();
        if (this.seedTransitions && seeds != null && !seeds.isEmpty()) {
            int trans = rand.nextInt(seeds.size());
            run.concat(seeds.get(trans));
        }
        PSymbolInstance out;
        do {
            PSymbolInstance next = nextInput(run);
            depth++;
            out = target.step(next);
            run = run.append(next).append(out);
            if (!hyp.accepts(run)) {
                LOGGER.debug(Category.COUNTEREXAMPLE, "Run with CE: {0}", run);
                target.post();
                return run;
            }
        } while (rand.nextDouble() > resetProbability && depth < maxDepth &&
                !out.getBaseSymbol().equals(error));

        LOGGER.debug(Category.COUNTEREXAMPLE, "Run /wo CE: {0}", run);
        target.post();
        return null;
    }

    public void setError(ParameterizedSymbol error) {
        this.error = error;
    }

    private PSymbolInstance nextInput(Word<PSymbolInstance> run) {
        ParameterizedSymbol ps = nextSymbol();
        PSymbolInstance psi = nextDataValues(run, ps);
        return psi;
    }

    private PSymbolInstance nextDataValues(
            Word<PSymbolInstance> run, ParameterizedSymbol ps) {

        DataValue[] vals = new DataValue[ps.getArity()];

        int i = 0;
        for (DataType t : ps.getPtypes()) {
            Theory teacher = teachers.get(t);
            // TODO: generics hack?
            Set<DataValue> oldSet = DataWords.valSet(run, t);
            for (int j = 0; j < i; j++) {
                if (vals[j].getDataType().equals(t)) {
                    oldSet.add(vals[j]);
                }
            }
            ArrayList<DataValue> old = new ArrayList<>(oldSet);

            Set<DataValue> newSet = new HashSet<>(
                teacher.getAllNextValues(old));

            // TODO: add constants in teacher?
            newSet.addAll(constants.values(t));

            newSet.removeAll(old);
            ArrayList<DataValue> newList = new ArrayList<>(newSet);

            double draw = rand.nextDouble();
            if (draw <= newDataProbability || old.isEmpty()) {
                int idx = rand.nextInt(newList.size());
                vals[i] = newList.get(idx);
            } else {
                int idx = rand.nextInt(old.size());
                vals[i] = old.get(idx);
            }

            i++;
        }
        return new PSymbolInstance(ps, vals);
    }

    private ParameterizedSymbol nextSymbol() {
        ParameterizedSymbol ps = null;
        Map<DataType, Integer> tCount = new LinkedHashMap<>();
        if (uniform) {
            ps = inputs[rand.nextInt(inputs.length)];
        } else {
            int MAX_WEIGHT = 0;
            int[] weights = new int[inputs.length];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1;
                for (DataType t : inputs[i].getPtypes()) {
                    Integer old = tCount.get(t);
                    if (old == null) {
                        // TODO: what about constants?
                        old = 0;
                    }
                    weights[i] *= (old + 1);
                    tCount.put(t, ++old);
                }
                MAX_WEIGHT += weights[i];
            }

            int idx = rand.nextInt(MAX_WEIGHT) + 1;
            int sum = 0;
            for (int i = 0; i < inputs.length; i++) {
                sum += weights[i];
                if (idx <= sum) {
                    ps = inputs[i];
                    break;
                }
            }
        }
        return ps;
    }

}
