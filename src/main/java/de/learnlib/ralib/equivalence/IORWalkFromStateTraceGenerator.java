package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class IORWalkFromStateTraceGenerator extends IORandomWalkTraceGenerator implements TraceGenerator {
	private AccessSequenceProvider accSeqProvider;

	public IORWalkFromStateTraceGenerator(Random rand, boolean uniform, double stopProbability,
			double regProb, double hisProb, double relatedProb,  int maxDepth, Constants constants,
			Map<DataType, Theory> teachers, AccessSequenceProvider accessSequenceProvider,
			ParameterizedSymbol... inputs) {

		super(rand, uniform, stopProbability, regProb, hisProb, relatedProb, maxDepth, constants, teachers, inputs);
		this.accSeqProvider = accessSequenceProvider;
	}

	@Override
	public Word<PSymbolInstance> generateTrace(RegisterAutomaton hyp) {
		Word<PSymbolInstance> accessSequence = pickRandomAccessSequence(hyp, this.accSeqProvider, super.rand);
		Word<PSymbolInstance>  runFromAccessSequence = super.randomTraceFromPrefix(accessSequence, hyp);
		Word<PSymbolInstance> run = accessSequence.concat(runFromAccessSequence);		
		return run;
	}

	private Word<PSymbolInstance> pickRandomAccessSequence(RegisterAutomaton hyp, AccessSequenceProvider accSeqProvider,
			Random rand) {
		Collection<RALocation> locations = hyp.getInputStates();
		RALocation randLocation = new ArrayList<>(locations).get(this.rand.nextInt(locations.size()));
		Word<PSymbolInstance> randAccSeq = accSeqProvider.getAccessSequence(hyp, randLocation);
		return randAccSeq;
	}
}
