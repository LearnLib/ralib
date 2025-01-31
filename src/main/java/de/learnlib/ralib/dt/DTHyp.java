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
import net.automatalib.word.Word;

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
        List<Word<PSymbolInstance>> tseq = new ArrayList<>();
        for (PSymbolInstance psi : dw) {

            ParValuation pars = new ParValuation(psi);

            Map<Word<PSymbolInstance>, TransitionGuard> candidates =
            		current.getBranching(psi.getBaseSymbol()).getBranches();

            if (candidates == null) {
                return null;
            }

            boolean found = false;
            for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : candidates.entrySet()) {
            	TransitionGuard g = e.getValue();
            	if (g.isSatisfied(vars, pars, this.constants)) {
            		Word<PSymbolInstance> w = e.getKey();
            		vars = current.getAssignment(w, dt.getLeaf(w)).compute(vars, pars, this.constants);
            		current = dt.getLeaf(w);
            		tseq.add(w);
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
        List<Word<PSymbolInstance>> tseq = getDTTransitions(word);
		if (tseq == null)
			return dt.getLeaf(word).getAccessSequence();
		return tseq.get(tseq.size() - 1);
	}

	@Override
	public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word,
			Word<PSymbolInstance> location) {
		Word<PSymbolInstance> suffix = word.suffix(1);

		DTLeaf leaf = dt.getLeaf(location);
		assert leaf != null;
		assert leaf.getAccessSequence().equals(location) || leaf.getShortPrefixes().contains(location);

		if (leaf.getAccessSequence().equals(location)) {
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

		ShortPrefix sp = (ShortPrefix)leaf.getShortPrefixes().get(location);
		Word<PSymbolInstance> ret = branchWithSameGuard(word, sp.getBranching(ps));
		assert ret != null;
		return ret;
	}

	public Word<PSymbolInstance> branchWithSameGuard(Word<PSymbolInstance> word, MappedPrefix src_id, Branching branching) {
		return branching.transformPrefix(word);

	}

	/*
	 * Get all transitions u.alpha in the same leaf as uAlpha, where alpha is the last symbol of uAlpha
	 */
	@Override
	public Set<Word<PSymbolInstance>> getAlphaTransitions(Word<PSymbolInstance> uAlpha) {
		DTLeaf uAlphaLeaf = dt.getLeaf(uAlpha);
		assert uAlphaLeaf != null;
		ParameterizedSymbol ps = uAlpha.lastSymbol().getBaseSymbol();
		Word<PSymbolInstance> u = uAlpha.prefix(uAlpha.length() - 1);
		DTLeaf uLeaf = dt.getLeaf(u);
		MappedPrefix uMapped = uLeaf.getPrefix(u);
		Branching branching = uMapped instanceof ShortPrefix ? ((ShortPrefix) uMapped).getBranching(ps) : uLeaf.getBranching(ps);
		Set<Word<PSymbolInstance>> transitions = new LinkedHashSet<>();
		for (Word<PSymbolInstance> ua : branching.getBranches().keySet()) {
			DTLeaf uaLeaf = dt.getLeaf(ua);
			if (uaLeaf.equals(uAlphaLeaf)) {
				transitions.add(ua);
			}
		}
		return transitions;
	}

	@Override
	public Set<Word<PSymbolInstance>> getTransitions(Word<PSymbolInstance> prefix, ParameterizedSymbol ps) {
		DTLeaf leaf = dt.getLeaf(prefix);
		MappedPrefix mp = leaf.getPrefix(prefix);
		Branching branching = mp instanceof ShortPrefix ? ((ShortPrefix) mp).getBranching(ps) : leaf.getBranching(ps);
		return branching.getBranches().keySet();
	}
}
