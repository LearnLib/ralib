package de.learnlib.ralib.learning;

import de.learnlib.ralib.data.RegisterAssignment;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public interface PrefixContainer {
    Word<PSymbolInstance> getPrefix();
    RegisterAssignment getAssignment();
}
