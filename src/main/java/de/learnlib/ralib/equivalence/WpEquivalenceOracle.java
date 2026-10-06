package de.learnlib.ralib.equivalence;

import java.util.Collection;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.wmethod.WpMethodStrategy;
import de.learnlib.ralib.equivalence.wmethod.WpTestSuite;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class WpEquivalenceOracle implements IOEquivalenceOracle {

    private final RegisterAutomaton target;

    private final WpMethodStrategy strategy;

    private final int sizeDifference;

    private final Map<DataType, Theory> teachers;

    private final Constants consts;

    private final boolean ioMode;

    private final ParameterizedSymbol[] inputs;

    private final ConstraintSolver solver;

    private static final Logger LOGGER = LoggerFactory.getLogger(IOWpEquivalenceOracle.class);

    public WpEquivalenceOracle(RegisterAutomaton target,
            WpMethodStrategy strategy,
            int sizeDifference,
            Map<DataType, Theory> teachers,
            Constants consts,
            boolean ioMode,
            ParameterizedSymbol ... inputs) {
        this.target = target;
        this.strategy = strategy;
        this.sizeDifference = sizeDifference;
        this.teachers = teachers;
        this.consts = consts;
        this.inputs = inputs;
        this.ioMode = ioMode;
        solver = new ConstraintSolver();
    }

    @Override
    public @Nullable DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
            Collection<? extends PSymbolInstance> clctn) {

        WpTestSuite testSuite = new WpTestSuite(hypothesis, sizeDifference, strategy, teachers, solver, consts, ioMode, inputs);

        for (Word<PSymbolInstance> test : testSuite) {
            boolean hypAcc = hypothesis.accepts(test);
            boolean sulAcc = target.accepts(test);
            if (hypAcc != sulAcc) {
                return new DefaultQuery<>(test, sulAcc);
            }
        }

        return null;
    }

}
