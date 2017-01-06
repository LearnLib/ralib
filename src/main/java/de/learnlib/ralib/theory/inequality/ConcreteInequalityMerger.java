package de.learnlib.ralib.theory.inequality;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class ConcreteInequalityMerger implements InequalityGuardMerger{
	

	private SDTGuardLogic logic;

	public ConcreteInequalityMerger(SDTGuardLogic logic) {
		this.logic = logic;
	}
	
	/**
	 * Merges a list of sorted inequality guards, which are EqualityGuards and IntervalGuards, based on equivalence of 
	 * their associated SDT. Returns an ordered mapping from merged guards to SDTs. The basic implementation does a full run
	 * of the ordered guards, and then an additional run, to check the case of the >,=,< to !=,== merger.
	 *   </p>
	 * Sorted guards in this case means that any two adjacent guards are connected. Connectivity depends on whether the
	 * domain is discrete or continuous. (in discrete domains, two equality guards can also be connected)  
	 */
	public LinkedHashMap<SDTGuard, SDT> merge(List<SDTGuard> sortedInequalityGuards, final Map<SDTGuard, SDT> sdtMap) {
		final LinkedHashMap<SDTGuard, SDT> mergedResult = new LinkedHashMap<>(); // contains the final merged result
		LinkedHashMap<SDTGuard, SDT> mergedTemp= new LinkedHashMap<>(sdtMap); // contains initial and all intermediate merging entries.
		if (sortedInequalityGuards.size() <= 2)  {
			sortedInequalityGuards.forEach( g -> mergedResult.put(g, sdtMap.get(g))); 
			return mergedResult;
		}
			
		SDTGuard[] ineqGuards = sortedInequalityGuards.toArray(new SDTGuard [sortedInequalityGuards.size()]);
		SDTGuard head = ineqGuards[0];
		SDT sdtHead = sdtMap.get(head);
		
		for (int i=1; i<ineqGuards.length; i++) {
			SDTGuard next = ineqGuards[i];
			SDT equivTest = checkSDTEquivalence(head, next, mergedTemp);
			if (equivTest != null) {
				SDTGuard mergedGuard = this.logic.disjunction(head, next);
				mergedTemp.put(mergedGuard, equivTest);
				head = mergedGuard;
				sdtHead = equivTest;
			} else {
				mergedResult.put(head, sdtHead);
				head = next;
				sdtHead = sdtMap.get(head);
			}
			
			if (i == ineqGuards.length-1) { 
				mergedResult.put(head, sdtHead);
			}
		}
		
		if (mergedResult.size() > 1) {
			// we need to check if the merged guard don't fit into a pattern where all interval SDTs are equivalent, in which case, 
			// we can replace all interval guards by a conjunction of disequalities.
			SDTGuard [] intervalGuards = mergedResult.keySet().stream()
					.filter(sdt -> sdt instanceof IntervalGuard).toArray(SDTGuard []::new);
			boolean areEquivalent = true;
			SDT equivSDT = null;
			for (int i=1; i<intervalGuards.length; i++) {  
				equivSDT = checkSDTEquivalence(intervalGuards[i-1], intervalGuards[i], mergedResult);
				if (equivSDT == null) {
					areEquivalent = false;
					break;
				}
			}
			
			if (areEquivalent) {
				LinkedHashMap<SDTGuard, SDT> eqDeqMergedResult = new LinkedHashMap<>();
				
				mergedResult.forEach((guard, sdt) -> {
					if (guard instanceof EqualityGuard)
						eqDeqMergedResult.put(guard, sdt);
				});
				
				SDTGuard elseGuard;
			
				SDTGuard[] deq = eqDeqMergedResult.keySet().stream()
					.map(eq -> ((EqualityGuard) eq).toDeqGuard())
					.toArray(SDTGuard []::new);
				if (deq.length > 1)
					elseGuard = new SDTAndGuard(head.getParameter(), deq);
				else
					elseGuard = deq[0];
				
				eqDeqMergedResult.put(elseGuard, equivSDT);
				return eqDeqMergedResult;
			}	
		}
		return mergedResult;
	}
	
	
	
	
	/**
	 * Checks if SDTs for guards are equivalent and if so, returns the preferred SDT.
	 */
	SDT checkSDTEquivalence(SDTGuard guard, SDTGuard withGuard, Map<SDTGuard, SDT> guardSdtMap) {
		IntervalGuard intGuard = null;
		EqualityGuard equGuard = null;
		if (guard instanceof EqualityGuard && withGuard instanceof IntervalGuard) { 
			equGuard = (EqualityGuard) guard;
			intGuard = (IntervalGuard) withGuard;
		} else if (guard instanceof IntervalGuard && withGuard instanceof EqualityGuard) {
			equGuard = (EqualityGuard) withGuard;
			intGuard = (IntervalGuard) guard;
		} else if (guard instanceof EqualityGuard && withGuard instanceof EqualityGuard) {
			return null; 
		}
		
		if (equGuard != null) {
			SDT intSdt = guardSdtMap.get(intGuard);
			SDT equSdt = guardSdtMap.get(equGuard);
			List<EqualityGuard> eqGuards = //Arrays.asList(equGuard); 
					intSdt.getGuards(g -> g instanceof EqualityGuard && !((EqualityGuard) g).isEqualityWithSDV())
					.stream().map(g -> ((EqualityGuard) g)).collect(Collectors.toList());
			eqGuards.add(equGuard);
			if (equSdt.isEquivalentUnderEquality(intSdt, eqGuards))
				return intSdt;
		} else { // two interval guards
			SDT sdt = guardSdtMap.get(guard);
			SDT otherSdt = guardSdtMap.get(withGuard);
			if (sdt.isEquivalent(otherSdt, new VarMapping()))
				return sdt;
		}
		return null;
	}
}
