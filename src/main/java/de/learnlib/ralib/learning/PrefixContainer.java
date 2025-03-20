package de.learnlib.ralib.learning;

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.expressions.Constant;
import net.automatalib.word.Word;

import java.util.Map;

public interface PrefixContainer {
	Word<PSymbolInstance> getPrefix();
	RegisterAssignment getAssignment();
}
