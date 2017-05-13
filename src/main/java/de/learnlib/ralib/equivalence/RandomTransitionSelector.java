package de.learnlib.ralib.equivalence;

import java.util.Map;
import java.util.Random;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * Generates an input by selecting first a transition then concretizing it in a smart way.
 */
public class RandomTransitionSelector extends InputSelector{

	public RandomTransitionSelector(Random rand, Map<DataType, Theory> teachers, Constants constants,
			ParameterizedSymbol[] inputs) {
		super(rand, teachers, constants, inputs);
	}

	protected PSymbolInstance nextInput(Word<PSymbolInstance> run, RegisterAutomaton hyp) {
		return null;
	}

}
