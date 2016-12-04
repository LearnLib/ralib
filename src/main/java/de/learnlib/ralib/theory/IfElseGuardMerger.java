package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.equality.EqualityGuard;

/**
 * Provides a general implementation of merging a series of if guards with an
 * else guard.
 */
public class IfElseGuardMerger {
	
	
	/**
	 * Tries to merge each if guard with an else guard based on SDT equivalence.
	 * It assumes that the elseGuard is a conjuction over the negation of each
	 * of the if guards.
	 */
	public Map<SDTGuard, SDT> merge(Map<SDTGuard, SDT> ifGuards, SDTGuard elseGuard, SDT elseSDT) {
		Map<SDTGuard, SDT> merged = new LinkedHashMap<>();
		SDTGuard newElseGuard = elseGuard;
		for (SDTGuard ifGuard : ifGuards.keySet()) {
			SDT ifSdt = ifGuards.get(ifGuard);
			boolean equiv = checkSDTEquivalence(ifGuard, ifSdt, elseGuard, elseSDT);
			// are equivalent
			if (equiv) {
				if (ifGuards.size() == 1) {
					newElseGuard = new SDTTrueGuard(ifGuard.getParameter());
				} else {
					newElseGuard = merge(ifGuard, (SDTAndGuard) newElseGuard);
				}

			} else {
				merged.put(ifGuard, ifSdt);
			}
		}
		SDTGuard compressedElseGuard = compress(newElseGuard);

		merged.put(compressedElseGuard, elseSDT);
		return merged;
	}

	/**
	 * Returns true if the two subtrees are equivalent in the context of the
	 * guards and false otherwise.
	 */
	boolean checkSDTEquivalence(SDTGuard guard, SDT guardSdt, SDTGuard elseGuard, SDT elseGuardSdt) {
		boolean equiv = false;
		if (isSDVEquality(guard))
			equiv = guardSdt.isEquivalentUnderEquality(elseGuardSdt, Arrays.asList((EqualityGuard) guard));
		else
			equiv = guardSdt.isEquivalent(elseGuardSdt, new VarMapping());
		return equiv;
	}

	private boolean isSDVEquality(SDTGuard guard) {
		return guard instanceof EqualityGuard && ((EqualityGuard) guard).isEqualityWithSDV();
	}

	/**
	 * Returns the merged guard. The abstract class provides an implementation
	 * for the general case when the else guard is just a conjunction of negated
	 * if guards.
	 */
	protected SDTAndGuard merge(SDTGuard ifGuard, SDTAndGuard elseGuard) {
		SDTAndGuard conj = (SDTAndGuard) elseGuard;
		SDTAndGuard newElseGuard = null;
		SDTNotGuard negGuard = new SDTNotGuard(ifGuard);
		List<SDTGuard> newNegGuards = new ArrayList<SDTGuard>(conj.getGuards());
		assert newNegGuards.remove(negGuard);

		SDTGuard[] conjArray = newNegGuards.toArray(new SDTGuard[] {});
		newElseGuard = new SDTAndGuard(elseGuard.getParameter(), conjArray);
		return newElseGuard;
	}

	/**
	 * At the end of merging, the else Guard is compressed. Default compression
	 * is by transforming an empty conjunction to true, and a conjunction of one
	 * guard to the guard. If the else guard is of not of type SDTGuard, it is
	 * left unchanged.
	 */
	protected SDTGuard compress(SDTGuard elseGuard) {
		SDTGuard newElseGuard = elseGuard;
		if (elseGuard instanceof SDTAndGuard) {
			SDTAndGuard conj = (SDTAndGuard) elseGuard;

			List<SDTGuard> conjGuards = conj.getGuards();
			if (conjGuards.isEmpty())
				newElseGuard = new SDTTrueGuard(elseGuard.getParameter());
			else {
				if (conjGuards.size() == 1)
					newElseGuard = conjGuards.get(0);
			}
		}
		return newElseGuard;
	}
}
