package de.learnlib.ralib.dt;

import java.util.LinkedHashSet;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DTHyp extends Hypothesis {
	
	private final DT dt;
	
	public DTHyp(Constants consts, DT dt) {
		super(consts);
		this.dt = dt;
	}
	
	@Override
	public Set<Word<PSymbolInstance>> possibleAccessSequences(Word<PSymbolInstance> word) {
		Set<Word<PSymbolInstance>> ret = new LinkedHashSet<Word<PSymbolInstance>>();
		Word<PSymbolInstance> as = super.transformAccessSequence(word);
		ret.add(super.transformAccessSequence(as));
		
		DTLeaf leaf = dt.getLeaf(as);
		assert leaf!=null;
		for(MappedPrefix mp : leaf.getShortPrefixes().get())
			ret.add(mp.getPrefix());
		return ret;
	}

}
