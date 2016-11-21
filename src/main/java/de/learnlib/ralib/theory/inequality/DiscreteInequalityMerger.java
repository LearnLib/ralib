package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class DiscreteInequalityMerger extends ContinuousInequalityMerger{

	public DiscreteInequalityMerger() {
	}
	
	/**
	 * For discrete domains it is possible to have multiple adjacent equality guards. Since their respective SDTs cannot
	 * be merged in a single iteration of the merging algorithm, multiple iterations are run until progress can no longer be made.
	 */
	public LinkedHashMap<SDTGuard, SDT> merge(List<SDTGuard> sortedInequalityGuards, Map<SDTGuard, SDT> sdtMap) {
		LinkedHashMap<SDTGuard, SDT> mergedGuards = super.merge(sortedInequalityGuards, sdtMap);
		List<SDTGuard> newIneqGuards = new ArrayList<SDTGuard>(mergedGuards.keySet());
		int refSize = sdtMap.size();
		while (refSize > mergedGuards.size()) {
			refSize = mergedGuards.size();
			mergedGuards= super.merge(newIneqGuards, mergedGuards);
			newIneqGuards = new ArrayList<SDTGuard>(mergedGuards.keySet());
		}
		
		return mergedGuards;
	}
	
	SDT checkSDTEquivalence(SDTGuard guard, SDTGuard withGuard, Map<SDTGuard, SDT> guardSdtMap) {
		if (guard instanceof EqualityGuard && withGuard instanceof EqualityGuard) {
			EqualityGuard equGuard = (EqualityGuard) guard;
			EqualityGuard equGuard2 = (EqualityGuard) guard;
			SDT equSdt = guardSdtMap.get(equGuard);
			SDT equSdt2 = guardSdtMap.get(equGuard2);
			if (equGuard.isEqualityWithSDV() && !equGuard2.isEqualityWithSDV()) {
				if (equSdt.isEquivalentUnderEquality(equSdt2, Arrays.asList(equGuard))) {
					return equSdt2;
				}
			}
			if (equGuard2.isEqualityWithSDV() && !equGuard.isEqualityWithSDV()) {
				if (equSdt2.isEquivalentUnderEquality(equSdt, Arrays.asList(equGuard2))) {
					return equSdt;
				}
			}
			return null;
		} else {
			return super.checkSDTEquivalence(guard, withGuard, guardSdtMap);
		}
	}
	
	protected SDTGuard merge(SDTGuard aGuard, SDTGuard withGuard) {
		EqualityGuard equGuard;
		IntervalGuard intGuard;
		if (aGuard instanceof EqualityGuard && withGuard instanceof EqualityGuard) {
			equGuard = (EqualityGuard) aGuard;
			EqualityGuard equGuard2 = (EqualityGuard) withGuard;
			return new IntervalGuard(aGuard.getParameter(), equGuard.getExpression(), Boolean.FALSE, equGuard2.getExpression(), Boolean.FALSE);
		}
		if (aGuard instanceof IntervalGuard && withGuard instanceof EqualityGuard) {
			equGuard = (EqualityGuard) withGuard;
			intGuard = (IntervalGuard) aGuard;
			return new IntervalGuard(aGuard.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), equGuard.getExpression(), Boolean.FALSE); 
		} 
		
		if (aGuard instanceof EqualityGuard && withGuard instanceof IntervalGuard) {
			equGuard = (EqualityGuard) aGuard;
			intGuard = (IntervalGuard) withGuard;
			return new IntervalGuard(aGuard.getParameter(), equGuard.getExpression(), Boolean.FALSE, intGuard.getRightExpr(), intGuard.getRightOpen());
		}
		
		if (aGuard instanceof IntervalGuard && withGuard instanceof IntervalGuard) {
			intGuard = (IntervalGuard) aGuard;
			IntervalGuard intGuard2 = (IntervalGuard) withGuard;
			assert Boolean.logicalXor(intGuard.getRightOpen(), intGuard2.getLeftOpen());
			if (intGuard2.isBiggerGuard() && intGuard.isSmallerGuard())
				return new SDTTrueGuard(aGuard.getParameter());
			else
				return new IntervalGuard(aGuard.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard2.getRightExpr(), intGuard2.getRightOpen());
		}
		
		throw new DecoratedRuntimeException("Invalid inequality merge pair").
		addDecoration("head guard", aGuard).addDecoration("next guard", withGuard);
	}
	
}
