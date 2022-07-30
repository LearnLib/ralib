package de.learnlib.ralib.dt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class DTHyp extends Hypothesis {
	
	private final DT dt;
	
	public DTHyp(Constants consts, DT dt) {
		super(consts);
		this.dt = dt;
	}
	
	@Override
	public boolean isAccessSequence(Word<PSymbolInstance> word) {
		if (super.isAccessSequence(word))
			return true;
		DTLeaf leaf = dt.getLeaf(word);
		if (leaf == null)
			return false;
		return leaf.getAccessSequence().equals(word) ||
				leaf.getShortPrefixes().contains(word);
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

	@Override
	public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word,
			Word<PSymbolInstance> location) {
		Word<PSymbolInstance> suffix = word.suffix(1);
		
		DTLeaf leaf = dt.getLeaf(location);
		assert leaf != null;
		
		if (leaf.getAccessSequence().equals(location) ||
				!leaf.getShortPrefixes().contains(location))
			return super.transformTransitionSequence(word);
		
		ParameterizedSymbol ps = suffix.firstSymbol().getBaseSymbol();
		
//		List<Transition> tseq = getTransitions(word);
//        Transition last = tseq.get(tseq.size() -1);
//		TransitionGuard transitionGuard = last.getGuard();
		
		ShortPrefix sp = (ShortPrefix)leaf.getShortPrefixes().get(location);
		Word<PSymbolInstance> ret = branchWithSameGuard(word, sp.getBranching(ps));
		assert ret != null;
		return ret;
//		Branching b = sp.getBranching(ps);
//		for (Word<PSymbolInstance> p : b.getBranches().keySet()) {
//			if (p.lastSymbol().getBaseSymbol().equals(ps)) {
//				tseq = getTransitions(p);
//				last = tseq.get(tseq.size()-1);
//				if (last.getGuard() == transitionGuard)
//					return p;
//			}
//		}
//		
//		throw new IllegalStateException("cannot be reached!");
	}
	
}
