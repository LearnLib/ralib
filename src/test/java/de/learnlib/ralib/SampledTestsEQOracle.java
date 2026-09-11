package de.learnlib.ralib;

import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

public class SampledTestsEQOracle implements IOEquivalenceOracle {

    /** Stores tests. */
    protected List<Word<PSymbolInstance>> tests;
	private DataWordSUL sul;


    /**
     * Constructs a new instance from the given parameters.
     *
     * @param tests the list of tests to be sampled
     * @param sul   the SUL to be used
     */
    public SampledTestsEQOracle(List<Word<PSymbolInstance>> tests, DataWordSUL sul) {
        this.tests = tests;
        this.sul = sul;
    }

    /**
     * Tries to find a counterexample using the sampled tests technique.
     *
     * @param  hypothesis the hypothesis to be searched
     * @param  inputs     the inputs to be used
     *
     * @return            the counterexample or null
     */

    @Override
    public @Nullable DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
        Collection<? extends PSymbolInstance> inputs) {
        for (Word<PSymbolInstance> test: tests) {
            // we build a trace capturing a valid execution of the SUL, which should be accepted by the hypothesis
            WordBuilder<PSymbolInstance> builder = new WordBuilder<>();
            sul.pre();
            for (PSymbolInstance input : test) {
            	builder.append(input);
            	PSymbolInstance output = sul.step(input);
            	builder.append(output);
            	Word<PSymbolInstance> trace = builder.toWord();
            	if (!hypothesis.accepts(trace)) {
            		return new DefaultQuery<>(trace, true);
            	}
            }
            sul.post();
        }
        return null;
    }
}
