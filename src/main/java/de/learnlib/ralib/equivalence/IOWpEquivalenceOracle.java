package de.learnlib.ralib.equivalence;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.wmethod.SequenceBuilder;
import de.learnlib.ralib.equivalence.wmethod.WpMethodStrategy;
import de.learnlib.ralib.equivalence.wmethod.WpTestSuite;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class IOWpEquivalenceOracle implements IOEquivalenceOracle {

    private final DataWordSUL target;

    private final WpMethodStrategy strategy;

    private final int sizeDifference;

    private final Map<DataType, Theory> teachers;

    private final Constants consts;

    private final ParameterizedSymbol[] inputs;

    private final ConstraintSolver solver;

    private static final Logger LOGGER = LoggerFactory.getLogger(IOWpEquivalenceOracle.class);

    public IOWpEquivalenceOracle(DataWordSUL target,
            WpMethodStrategy strategy,
            int sizeDifference,
            Map<DataType, Theory> teachers,
            Constants consts,
            ParameterizedSymbol ... inputs) {
        this.target = target;
        this.strategy = strategy;
        this.sizeDifference = sizeDifference;
        this.teachers = teachers;
        this.consts = consts;
        this.inputs = inputs;
        solver = new ConstraintSolver();
    }

    @Override
    public @Nullable DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
            Collection<? extends PSymbolInstance> clctn) {
        if (clctn != null && !clctn.isEmpty()) {
            LOGGER.warn(Category.QUERY, "set of inputs is ignored by this equivalence oracle");
        }

        WpTestSuite testSuite = new WpTestSuite(hypothesis, sizeDifference, strategy, teachers, solver, consts, true, inputs);

        for (Word<PSymbolInstance> test : testSuite) {
            Word<PSymbolInstance> hypOutput = SequenceBuilder.getOutputSequence(test, hypothesis, consts);
            Iterator<PSymbolInstance> outIt = hypOutput.iterator();

            Word<PSymbolInstance> trace = Word.epsilon();

            target.pre();
            for (PSymbolInstance in : test) {
                trace = trace.append(in);
                PSymbolInstance out = target.step(in);
                trace = trace.append(out);
                if (!outIt.hasNext() ||
                        !outIt.next().equals(out)) {
                    target.post();
                    return new DefaultQuery<>(trace, true);
                }
            }
            target.post();
        }

        return null;
    }

}
