package de.learnlib.ralib.theory.inequality;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class InequalityMerger {
	
	private MergeOracle oracle;

	public InequalityMerger(MergeOracle mergeOracle) {
		this.oracle = mergeOracle;
	}
	
	/**
	 * Merges a list of sorted inequality guards, which are EqualityGuards and IntervalGuards.
	 * Returns a mapping from merged guards to the sets of guards from which these merged guards originated.  
	 * Sorted in this case means that adjacent guards are connected. 
	 */
	public LinkedHashMap<SDTGuard, Set<SDTGuard>> merge(List<SDTGuard> sortedInequalityGuards) {
		LinkedHashMap<SDTGuard, Set<SDTGuard>> mergedResult = new LinkedHashMap<>();
		if (sortedInequalityGuards.isEmpty()) 
			return mergedResult;
		SDTGuard[] ineqGuards = sortedInequalityGuards.toArray(new SDTGuard [sortedInequalityGuards.size()]);
		SDTGuard head = ineqGuards[0];
		Set<SDTGuard> mergedGuards = new LinkedHashSet<SDTGuard>();
		mergedGuards.add(head);
		for (int i=1; i<ineqGuards.length; i++) {
			SDTGuard prev = ineqGuards[i-1];
			SDTGuard next = ineqGuards[i];
			if (oracle.canMerge(prev, next)) {
				mergedGuards.add(next);
				SDTGuard mergedGuard = merge(head, next);
				head = mergedGuard;
				if (i == ineqGuards.length-1)
					mergedResult.put(head, mergedGuards);
			} else {
				mergedResult.put(head, mergedGuards);
				head = next;
				mergedGuards.clear();
			}
		}
		
		if (mergedResult.size() == 3) {
			SDTGuard[] finalGuards = mergedResult.keySet().toArray(new SDTGuard [3]);
			assert finalGuards[0] instanceof IntervalGuard && finalGuards[1] instanceof EqualityGuard && finalGuards[2] instanceof IntervalGuard;
			// the first and last inequality guards were merged to form the first, respectively third final guard.
			if (oracle.canMerge(ineqGuards[0], ineqGuards[ineqGuards.length-1])) {
				mergedResult.clear();
				mergedResult.put(finalGuards[1], Sets.newHashSet(finalGuards[1]));
				mergedGuards = new LinkedHashSet<SDTGuard>(sortedInequalityGuards);
				mergedGuards.remove(finalGuards[1]);
				mergedResult.put(((EqualityGuard) finalGuards[1]).toDeqGuard(), mergedGuards);
				
			}
		} 
		
		return mergedResult;
	}
	
	/**
	 * Merges two inequality guards.  
	 */
	protected SDTGuard merge(SDTGuard aGuard, SDTGuard withGuard) {
		IntervalGuard intGuard;
		
		if (aGuard instanceof IntervalGuard && withGuard instanceof EqualityGuard) {
			intGuard = (IntervalGuard) aGuard;
			assert intGuard.getRightOpen();
			return new IntervalGuard(aGuard.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard.getRightExpr(), Boolean.FALSE); 
		} 
		
		if (aGuard instanceof EqualityGuard && withGuard instanceof IntervalGuard) {
			intGuard = (IntervalGuard) withGuard;
			assert intGuard.getLeftOpen();
			return new IntervalGuard(aGuard.getParameter(), intGuard.getLeftExpr(), Boolean.FALSE, intGuard.getRightExpr(), intGuard.getRightOpen());
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
