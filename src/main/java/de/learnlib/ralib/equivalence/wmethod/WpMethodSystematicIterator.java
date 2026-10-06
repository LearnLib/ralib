package de.learnlib.ralib.equivalence.wmethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class WpMethodSystematicIterator implements Iterator<Word<PSymbolInstance>> {

    private final Iterator<Word<PSymbolInstance>> iterator;

    public WpMethodSystematicIterator(RegisterAutomaton model,
            int sizeDifference,
            Map<DataType, Theory> teachers,
            ConstraintSolver solver,
            Constants consts,
            boolean ioMode,
            ParameterizedSymbol ... inputs) {
        iterator = getTestSuite(model, sizeDifference, teachers, solver, consts, ioMode, inputs).iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Word<PSymbolInstance> next() {
        return iterator.next();
    }

    protected static Set<Word<PSymbolInstance>> getLocationCover(RegisterAutomaton model, Map<DataType, Theory> teachers, Constants consts, ConstraintSolver solver) {
        TransitionTree tt = new TransitionTree(model);
        TransitionTreeTraverser ttt = new TransitionTreeTraverser(tt, teachers, consts, solver);
        ttt.traverse();
        Set<Word<PSymbolInstance>> locCover = ttt.getLocationCover();
        Set<Word<PSymbolInstance>> locInputs = new LinkedHashSet<>();
        for (Word<PSymbolInstance> prefix : locCover) {
            locInputs.add(SequenceBuilder.getInputSequence(prefix));
        }
        return locInputs;
    }

    protected static List<Word<PSymbolInstance>> getTestSuite(RegisterAutomaton model,
            int sizeDifference,
            Map<DataType, Theory> teachers,
            ConstraintSolver solver,
            Constants consts,
            boolean ioMode,
            ParameterizedSymbol ... inputs) {
        List<Word<PSymbolInstance>> testSuite = new ArrayList<>();
        Set<Word<PSymbolInstance>> locCover = WpMethodSystematicIterator.getLocationCover(model, teachers, consts, solver);
        IdentificationSetBuilder suffixBuilder = new IdentificationSetBuilder(model, consts, solver, ioMode, inputs);
        for (Word<PSymbolInstance> prefix : locCover) {
            for (int size = 0; size <= sizeDifference; size++) {
                Set<Word<PSymbolInstance>> infixes = SequenceBuilder.buildInfixSet(prefix, size, inputs);
                for (Word<PSymbolInstance> infix : infixes) {
                    Word<PSymbolInstance> z = DataWords.concatenate(prefix, infix);
                    List<Word<PSymbolInstance>> suffixes = suffixBuilder.getIdentifyingSuffixes(z);
                    for (Word<PSymbolInstance> suffix : suffixes) {
                        Word<PSymbolInstance> test = DataWords.concatenate(z, suffix);
                        testSuite.add(test);
                    }
                }
            }
        }
        return testSuite;
    }
}
