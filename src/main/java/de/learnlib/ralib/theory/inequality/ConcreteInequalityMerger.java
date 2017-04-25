package de.learnlib.ralib.theory.inequality;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.IfElseGuardMerger;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTEquivalenceChecker;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class ConcreteInequalityMerger implements InequalityGuardMerger{
	

	protected SDTGuardLogic logic;

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
	public LinkedHashMap<SDTGuard, SDT> merge(List<SDTGuard> sortedInequalityGuards, final Map<SDTGuard, SDT> sdtMap, SDTEquivalenceChecker sdtChecker) {
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
			SDT equivTest = checkSDTEquivalence(head, next, mergedTemp, sdtChecker);
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
				equivSDT = checkSDTEquivalence(intervalGuards[i-1], intervalGuards[i], mergedResult, sdtChecker);
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
				if (deq.length == 0)
					System.out.println(mergedResult);
				
				if (deq.length > 1)
					elseGuard = new SDTAndGuard(head.getParameter(), deq);
				else
					elseGuard = deq[0];
				
				// the if else branching might still be further merged, for which we use an if else merger.
				IfElseGuardMerger ifElse = new IfElseGuardMerger(this.logic);
				LinkedHashMap<SDTGuard, SDT> result = ifElse.merge(eqDeqMergedResult, elseGuard, equivSDT, sdtChecker);
				return result;
			}	
		}
		return mergedResult;
	}
	
	
	
	
	/**
	 * Checks if SDTs for guards are equivalent and if so, returns the preferred SDT.
	 * @param sdtChecker 
	 */
	SDT checkSDTEquivalence(SDTGuard guard, SDTGuard otherGuard, Map<SDTGuard, SDT> guardSdtMap, SDTEquivalenceChecker sdtChecker) {
		
		SDT sdt = guardSdtMap.get(guard);
		SDT otherSdt = guardSdtMap.get(otherGuard);
		boolean equiv = sdtChecker.checkSDTEquivalence(guard, sdt, otherGuard, otherSdt);
		SDT retSdt = null;
		if (equiv) 
			if (guard instanceof EqualityGuard && ((EqualityGuard) guard).isEqualityWithSDV())
				retSdt = otherSdt;
			else
				retSdt = sdt;
		return retSdt;	
	}
}
