package de.learnlib.ralib.dt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.VarValuation;
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
	public boolean accepts(Word<PSymbolInstance> word) {
		Word<PSymbolInstance> as = transformAccessSequence(word);
		DTLeaf l = dt.getLeaf(as);
		assert l != null;

		return l.isAccepting();
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
    public Word<PSymbolInstance> transformAccessSequence(Word<PSymbolInstance> word) {
    	List<Word<PSymbolInstance>> tseq = getDTTransitions(word);
        if (tseq == null) {
            return null;
        }
        if (tseq.isEmpty()) {
            return Word.epsilon();
        } else {
        	return dt.getLeaf(tseq.get(tseq.size() - 1)).getAccessSequence();
        }
    }

	@Override
	public Set<Word<PSymbolInstance>> possibleAccessSequences(Word<PSymbolInstance> word) {
		Set<Word<PSymbolInstance>> ret = new LinkedHashSet<Word<PSymbolInstance>>();
		//Word<PSymbolInstance> as = super.transformAccessSequence(word);
		//ret.add(super.transformAccessSequence(as));
		Word<PSymbolInstance> as = transformAccessSequence(word);
		ret.add(as);

		DTLeaf leaf = dt.getLeaf(as);
		assert leaf!=null;
		for(MappedPrefix mp : leaf.getShortPrefixes().get())
			ret.add(mp.getPrefix());
		return ret;
	}

    protected List<Word<PSymbolInstance>> getDTTransitions(Word<PSymbolInstance> dw) {
        VarValuation vars = new VarValuation(getInitialRegisters());
        DTLeaf current = dt.getLeaf(Word.epsilon());
        //RALocation current = initial;
        List<Word<PSymbolInstance>> tseq = new ArrayList<>();
        //List<Transition> tseq = new ArrayList<>();
        for (PSymbolInstance psi : dw) {

            ParValuation pars = new ParValuation(psi);

            Map<Word<PSymbolInstance>, TransitionGuard> candidates =
            		current.getBranching(psi.getBaseSymbol()).getBranches();
            //Collection<Transition> candidates =
            //        current.getOut(psi.getBaseSymbol());

            if (candidates == null) {
                return null;
            }

            boolean found = false;
            for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : candidates.entrySet()) {
            //for (Transition t : candidates) {
            	TransitionGuard g = e.getValue();
            	if (g.isSatisfied(vars, pars, this.constants)) {
                //if (t.isEnabled(vars, pars, this.constants)) {
            		Word<PSymbolInstance> w = e.getKey();
            		vars = current.getAssignment(w, dt.getLeaf(w)).compute(vars, pars, this.constants);
                    //vars = t.execute(vars, pars, this.constants);
            		current = dt.getLeaf(w);
                    //current = t.getDestination();
            		tseq.add(w);
                    //tseq.add(t);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }
        return tseq;
    }

	@Override
	public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word) {
		//Word<PSymbolInstance> tseq = super.transformTransitionSequence(word);

        List<Word<PSymbolInstance>> tseq = getDTTransitions(word);
		if (tseq == null)
			return dt.getLeaf(word).getAccessSequence();
		return tseq.get(tseq.size() - 1);

		//if (tseq == null)
		//	return dt.getLeaf(word).getAccessSequence();
		//return tseq;
	}

	@Override
	public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word,
			Word<PSymbolInstance> location) {
		Word<PSymbolInstance> suffix = word.suffix(1);

		DTLeaf leaf = dt.getLeaf(location);
		assert leaf != null;

		if (leaf.getAccessSequence().equals(location) ||
				!leaf.getShortPrefixes().contains(location)) {
//			Word<PSymbolInstance> tseq = super.transformTransitionSequence(word);
			Word<PSymbolInstance> tseq = transformTransitionSequence(word);
			if (tseq == null) {
				ParameterizedSymbol ps = suffix.firstSymbol().getBaseSymbol();
				for (Word<PSymbolInstance> p : leaf.getBranching(ps).getBranches().keySet()) {
					DTLeaf l = dt.getLeaf(p);
					if (l != null && l == dt.getSink())
						return p;
				}
			}
			return tseq;
		}

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

	public Word<PSymbolInstance> branchWithSameGuard(Word<PSymbolInstance> word, MappedPrefix src_id, Branching branching) {
//    	Map<Word<PSymbolInstance>, TransitionGuard> branches = branching.getBranches();
//
//    	TransitionGuard guard = AutomatonBuilder.findMatchingGuard(word, src_id.getParsInVars(), branches, this.constants);
//    	for (Entry<Word<PSymbolInstance>, TransitionGuard> e : branches.entrySet()) {
//    		if (e.getValue().equals(guard)) {
//    			return e.getKey();
//    		}
//    	}
//    	return null;
		return branching.transformPrefix(word);

	}
}
