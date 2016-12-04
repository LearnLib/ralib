package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class DiscreteInequalityMerger extends ConcreteInequalityMerger{

	public DiscreteInequalityMerger(InequalityGuardLogic ineqGuardLogic) {
		super(new DiscreteInequalityGuardLogic(ineqGuardLogic));
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
	
	
	/**
	 * Logic class for discrete domains. Note, this logic is only valid in the context of merging,
	 * where the any disjoined guards are adjacent. 
	 */
	private static class DiscreteInequalityGuardLogic implements SDTGuardLogic {
		

		private InequalityGuardLogic ineqGuardLogic;


		public DiscreteInequalityGuardLogic(InequalityGuardLogic ineqGuardLogic) {
			this.ineqGuardLogic = ineqGuardLogic;
		}

		public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2) {
			EqualityGuard equGuard;
			IntervalGuard intGuard;
			if (guard1 instanceof EqualityGuard && guard2 instanceof EqualityGuard) {
				equGuard = (EqualityGuard) guard1;
				EqualityGuard equGuard2 = (EqualityGuard) guard2;
				return new IntervalGuard(guard1.getParameter(), equGuard.getExpression(), Boolean.FALSE, equGuard2.getExpression(), Boolean.FALSE);
			}
			if (guard1 instanceof IntervalGuard && guard2 instanceof EqualityGuard) {
				equGuard = (EqualityGuard) guard2;
				intGuard = (IntervalGuard) guard1;
				return new IntervalGuard(guard1.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), equGuard.getExpression(), Boolean.FALSE); 
			} 
			
			if (guard1 instanceof EqualityGuard && guard2 instanceof IntervalGuard) {
				equGuard = (EqualityGuard) guard1;
				intGuard = (IntervalGuard) guard2;
				return new IntervalGuard(guard1.getParameter(), equGuard.getExpression(), Boolean.FALSE, intGuard.getRightExpr(), intGuard.getRightOpen());
			}
			
			SDTGuard ineqDisjunction = this.ineqGuardLogic.disjunction(guard1, guard2);
			
			return ineqDisjunction;
		}
		
		public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2) {
			return this.ineqGuardLogic.conjunction(guard1, guard2);
		}
	}
	
}
