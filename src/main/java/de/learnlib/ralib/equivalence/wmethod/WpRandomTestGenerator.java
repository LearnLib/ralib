package de.learnlib.ralib.equivalence.wmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.RandomWalkSymbolGenerator;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class WpRandomTestGenerator {

    private final Random rand;

    private final int minimalSize;

    private final int rndLength;

    private final List<Word<PSymbolInstance>> locCover;

    private final RandomWalkSymbolGenerator symbolGen;

    private final IdentificationSetBuilder isb;

    public WpRandomTestGenerator(RegisterAutomaton model,
            Random rand,
            int minimalSize,
            int rndLength,
            boolean uniform,
            double newDataProbability,
            boolean ioMode,
            Map<DataType, Theory> teachers,
            Constants consts,
            ConstraintSolver solver,
            ParameterizedSymbol ... inputs) {
        this.rand = rand;
        this.minimalSize = minimalSize;
        this.rndLength = rndLength;

        TransitionTree tt = new TransitionTree(model);
        TransitionTreeTraverser ttt = new TransitionTreeTraverser(tt, teachers, consts, solver);
        ttt.traverse();
        Set<Word<PSymbolInstance>> locCover = ttt.getLocationCover();
        this.locCover = new ArrayList<>();
        for (Word<PSymbolInstance> prefix : locCover) {
            this.locCover.add(SequenceBuilder.getInputSequence(prefix));
        }

        isb = new IdentificationSetBuilder(model, consts, solver, ioMode, inputs);

        symbolGen = new RandomWalkSymbolGenerator(rand, uniform, newDataProbability, consts, teachers, inputs);
    }

    public Word<PSymbolInstance> next() {
        // pick random access sequence
        Word<PSymbolInstance> test = locCover.get(rand.nextInt(locCover.size()));

        int size = minimalSize;
        while (size > 0 || rand.nextDouble() > 1 / (rndLength + 1.0)) {
            PSymbolInstance next = symbolGen.nextInput(test);
            test = test.append(next);
            if (size > 0) {
                size--;
            }
        }

        List<Word<PSymbolInstance>> suffixes = isb.getIdentifyingSuffixes(test);
        // TODO: probability to draw suffix separating largest number of states
        Word<PSymbolInstance> suffix = suffixes.get(rand.nextInt(suffixes.size()));
        return DataWords.concatenate(test, suffix);
    }

}
