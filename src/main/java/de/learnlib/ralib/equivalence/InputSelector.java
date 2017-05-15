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
 * A bit of over-engineering
 */
public abstract class InputSelector {
	protected Map<DataType, Theory> teachers;
	protected Random rand;
	protected ParameterizedSymbol[] inputs;
	protected Constants constants;

	public InputSelector(Random rand, Map<DataType, Theory> teachers, Constants constants, ParameterizedSymbol... inputs) {
		this.teachers = teachers;
		this.rand = rand;
		this.inputs = inputs;
		this.constants = constants;
		
	}
	
	
	protected abstract PSymbolInstance nextInput(Word<PSymbolInstance> run,  RegisterAutomaton hyp);
}
