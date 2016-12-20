package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class DiscreteInequalityMerger extends ConcreteInequalityMerger{

	public DiscreteInequalityMerger() {
		super(new DiscreteInequalityGuardLogic());
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
		SDT res = null;
		if (guard instanceof EqualityGuard && withGuard instanceof EqualityGuard) {
			EqualityGuard equGuard = (EqualityGuard) guard;
			EqualityGuard equGuard2 = (EqualityGuard) withGuard;
			SDT equSdt = guardSdtMap.get(equGuard);
			SDT equSdt2 = guardSdtMap.get(equGuard2);
			if (equGuard.isEqualityWithSDV() && !equGuard2.isEqualityWithSDV()) {
				
				List<EqualityGuard> eqGuards = //Arrays.asList(equGuard); 
						equSdt2.getGuards(g -> g instanceof EqualityGuard && !((EqualityGuard) g).isEqualityWithSDV())
						.stream().map(g -> ((EqualityGuard) g)).collect(Collectors.toList());
				eqGuards.add(equGuard);
				if (equSdt.isEquivalentUnderEquality(equSdt2, eqGuards))
					res = equSdt2;
			} 
		} else {

			res = super.checkSDTEquivalence(guard, withGuard, guardSdtMap);
		}
		return res;
	}
}
