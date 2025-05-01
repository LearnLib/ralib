package de.learnlib.ralib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

/**
 * RandomWalk equivalence oracle for DataWordOracle
 */
public class RandomWalk implements IOEquivalenceOracle  {

	private final Map<DataType, Theory> teachers;
	private final Random rand;
	private final long maxRuns;
	private int depth = 10;
	private double resetProbability = 0.1;
	private double freshProbability = 0.5;
	private final List<ParameterizedSymbol> symbols;
	private final DataWordOracle wordOracle;
	private final Constants consts;

	public RandomWalk(Random rand, DataWordOracle membershipOracle, double resetProbability, double newDataProbability, long maxRuns, int maxDepth,
			Map<DataType, Theory> teachers, Constants consts,
			List<ParameterizedSymbol> symbols) {
		this.rand = rand;
		this.resetProbability = resetProbability;
		this.freshProbability = resetProbability;
		this.depth = maxDepth;
		this.maxRuns = maxRuns;
		this.wordOracle = membershipOracle;
		this.teachers = teachers;
		this.consts = consts;
		this.symbols = symbols;
	}

	@Override
	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
			Collection<? extends PSymbolInstance> inputs) {
		for (int i = 0; i < maxRuns; i++) {
			Word<PSymbolInstance> randomWord = generateRandomWord(symbols);
			DefaultQuery<PSymbolInstance, Boolean> query = new DefaultQuery<PSymbolInstance, Boolean>(randomWord);
			wordOracle.processQuery(query);
			boolean hypResponse = hypothesis.accepts(query.getInput());
			if (hypResponse != query.getOutput()) {
				return query;
			}
		}
		return null;
	}

	public Word<PSymbolInstance> generateRandomWord(List<ParameterizedSymbol> symbols) {
		WordBuilder<PSymbolInstance> wordBuilder = new WordBuilder<PSymbolInstance>();
		for (int i = 0; i < depth; i++) {
			ParameterizedSymbol randSym = symbols.get(rand.nextInt(symbols.size()));
			PSymbolInstance input = nextDataValues(wordBuilder.toWord(), randSym);
			wordBuilder.add(input);
			if (rand.nextDouble() < resetProbability) {
				break;
			}
		}
		return wordBuilder.toWord();
	}


    private PSymbolInstance nextDataValues(Word<PSymbolInstance> run, ParameterizedSymbol ps) {
        DataValue[] vals = new DataValue[ps.getArity()];

        int i = 0;
        for (DataType t : ps.getPtypes()) {
            Theory teacher = teachers.get(t);
            // TODO: generics hack?
            // TODO: add constants?
            Set<DataValue> oldSet = DataWords.valSet(run, t);
            for (int j = 0; j < i; j++) {
                if (vals[j].getDataType().equals(t)) {
                    oldSet.add(vals[j]);
                }
            }
            oldSet.addAll(consts.values(t));

            ArrayList<DataValue> old = new ArrayList<>(oldSet);

            Set<DataValue> newSet = new HashSet<>(teacher.getAllNextValues(old));

            newSet.removeAll(old);
            ArrayList<DataValue> newList = new ArrayList<>(newSet);

            double draw = rand.nextDouble();
            if (draw <= freshProbability || old.isEmpty()) {
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
}
