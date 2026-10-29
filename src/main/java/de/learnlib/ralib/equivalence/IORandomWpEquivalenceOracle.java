package de.learnlib.ralib.equivalence;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.wmethod.WpRandomTestGenerator;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class IORandomWpEquivalenceOracle implements IOEquivalenceOracle {

    public static record Options(Random random,
            int minimalSize,
            int rndLength,
            boolean uniform,
            double freshProbability,
            long maxRuns) {}

    private final Options options;

    private final DataWordSUL target;

    private final Map<DataType, Theory> teachers;

    private final Constants consts;

    private final ParameterizedSymbol[] inputs;

    private final ConstraintSolver solver;

    private static final Logger LOGGER = LoggerFactory.getLogger(IORandomWpEquivalenceOracle.class);

    public IORandomWpEquivalenceOracle(DataWordSUL target,
            Map<DataType, Theory> teachers,
            Constants consts,
            Options options,
            ParameterizedSymbol ... inputs) {
        this.target = target;
        this.teachers = teachers;
        this.consts = consts;
        this.options = options;
        this.inputs = inputs;
        solver = new ConstraintSolver();
    }

    @Override
    public @Nullable DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
            Collection<? extends PSymbolInstance> clctn) {
        if (clctn != null && !clctn.isEmpty()) {
            LOGGER.warn(Category.QUERY, "set of inputs is ignored by this equivalence oracle");
        }

        WpRandomTestGenerator testGen = new WpRandomTestGenerator(hypothesis,
                options.random,
                options.minimalSize,
                options.rndLength,
                options.uniform,
                options.freshProbability,
                true,
                teachers,
                consts,
                solver,
                inputs);

        int run = 0;
        while (run < options.maxRuns) {
            Word<PSymbolInstance> test = testGen.next();
            Word<PSymbolInstance> ce = run(test, hypothesis);
            if (ce != null) {
                LOGGER.debug(Category.COUNTEREXAMPLE, "Run with CE: {0}", run);
                return new DefaultQuery<>(ce, true);
            }
            run++;
        }

        LOGGER.debug(Category.COUNTEREXAMPLE, "Run /wo CE: {0}", run);
        return null;
    }

    private Word<PSymbolInstance> run(Word<PSymbolInstance> inputs, RegisterAutomaton hyp) {
        Word<PSymbolInstance> word = Word.epsilon();
        target.pre();
        for (PSymbolInstance in : inputs) {
            word = word.append(in);
            PSymbolInstance out = target.step(in);
            word = word.append(out);
            if (!hyp.accepts(word)) {
                target.post();
                return word;
            }
        }
        target.post();
        return null;
    }
}
