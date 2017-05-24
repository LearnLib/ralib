package de.learnlib.ralib.sul.tcp;

import de.learnlib.ralib.equivalence.TestPurpose;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TCPTestPurpose implements TestPurpose{

	public boolean isSatisfied(Word<PSymbolInstance> test) {
		if (test.isEmpty())
			return true;
		boolean meetsPurpose = !(test.toString().contains("IConnect") 
				&& !test.getSymbol(0).getBaseSymbol().getName().contains("IConnect"));
		return meetsPurpose;
	}

}
