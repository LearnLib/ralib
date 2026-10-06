package de.learnlib.ralib.equivalence.wmethod;

import java.util.Iterator;
import java.util.Map;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class WpTestSuite implements Iterable<Word<PSymbolInstance>> {

    private final Iterator<Word<PSymbolInstance>> iterator;

    public WpTestSuite(RegisterAutomaton model,
            int sizeDifference,
            WpMethodStrategy strategy,
            Map<DataType, Theory> teachers,
            ConstraintSolver solver,
            Constants consts,
            boolean ioMode,
            ParameterizedSymbol ... inputs) {
        iterator = switch (strategy) {
            case WpMethodStrategy.SYSTEMATIC ->
                new WpMethodSystematicIterator(model, sizeDifference, teachers, solver, consts, ioMode, inputs);
            default ->
                throw new IllegalArgumentException("Unknown Wp strategy");
        };
    }

    @Override
    public Iterator<Word<PSymbolInstance>> iterator() {
        return iterator;
    }
}
