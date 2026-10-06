package de.learnlib.ralib.equivalence.wmethod;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class WpMethodRandomIterator implements Iterator<Word<PSymbolInstance>> {

    private final Iterator<Word<PSymbolInstance>> iterator;

    public WpMethodRandomIterator(RegisterAutomaton model,
            int sizeDifference,
            Map<DataType, Theory> teachers,
            ConstraintSolver solver,
            Constants consts,
            boolean ioMode,
            ParameterizedSymbol ... inputs) {
        List<Word<PSymbolInstance>> testSuite = WpMethodSystematicIterator.getTestSuite(model, sizeDifference, teachers, solver, consts, ioMode, inputs);
        Collections.shuffle(testSuite);
        iterator = testSuite.iterator();
    }


    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Word<PSymbolInstance> next() {
        return iterator.next();
    }

}
