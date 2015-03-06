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
package de.learnlib.ralib.equivalence;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class IORandomWalk {

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
    private final Constants constants;
    private final Map<DataType, Theory> teachers;
    
    private static LearnLogger log = LearnLogger.getLogger(IORandomWalk.class);

    /**
     * creates an IO random walk
     * 
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
            boolean resetRuns, Map<DataType, Theory> teachers, ParameterizedSymbol... inputs) {

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
    }

    public Word<PSymbolInstance> findCounterExample(RegisterAutomaton hyp) {
        this.hyp = hyp;
        // reset the counter for number of runs?
        if (resetRuns) {
            runs = 0;
        }
        // find counterexample ...
        while (runs < maxRuns) {
            Word ce = run();
            if (ce != null) {
                return ce;
            }
        }
        return null;
    }

    private Word<PSymbolInstance> run() {
        int depth = 0;
        runs++;
        target.pre();
        Word<PSymbolInstance> run = Word.epsilon();

        do {
            PSymbolInstance next = nextInput(run);
            depth++;
            PSymbolInstance out = target.step(next);

            run = run.append(next).append(out);

            if (!hyp.accepts(run)) {
                log.log(Level.FINE, "Run with CE: {0}", run);                
                target.post();
                return run;
            }

        } while (rand.nextDouble() > resetProbability && depth < maxDepth);
        log.log(Level.FINE, "Run /wo CE: {0}", run);
        target.post();
        return null;
    }

    private PSymbolInstance nextInput(Word<PSymbolInstance> run) {
        ParameterizedSymbol ps = nextSymbol(run);
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
            // TODO: add constants?
            Set<DataValue<Object>> oldSet = DataWords.valSet(run, t);
            for (int j = 0; j < i; j++) {
                if (vals[j].getType().equals(t)) {
                    oldSet.add(vals[j]);
                }
            }
            ArrayList<DataValue<Object>> old = new ArrayList<>(oldSet);

            double draw = rand.nextDouble();
            if (draw <= newDataProbability || old.isEmpty()) {
                DataValue v = teacher.getFreshValue(old);
                vals[i] = v;
            } else {
                int idx = rand.nextInt(old.size());
                vals[i] = old.get(idx);
            }

            i++;
        }
        return new PSymbolInstance(ps, vals);
    }

    private ParameterizedSymbol nextSymbol(Word<PSymbolInstance> run) {
        ParameterizedSymbol ps = null;
        Map<DataType, Integer> tCount = new HashMap<>();
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
