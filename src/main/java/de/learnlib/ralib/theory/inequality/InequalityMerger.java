package de.learnlib.ralib.theory.inequality;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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
		Set<SDTGuard> mergedGuards = new HashSet<SDTGuard>();
		mergedGuards.add(head);
		for (int i=1; i<ineqGuards.length; i++) {
			SDTGuard next = ineqGuards[1];
			if (oracle.canMerge(head, next)) {
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
			if (oracle.canMerge(finalGuards[0], finalGuards[2])) {
				assert !oracle.canMerge(finalGuards[0], finalGuards[1]) && !oracle.canMerge(finalGuards[1], finalGuards[2]);
				mergedResult.clear();
				mergedResult.put(((EqualityGuard) finalGuards[1]).toDeqGuard(), new HashSet<SDTGuard>(sortedInequalityGuards));
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
			assert Boolean.logicalXor(intGuard.getRightOpen(), !intGuard2.getLeftOpen());
			if (intGuard2.isBiggerGuard())
				return new SDTTrueGuard(aGuard.getParameter());
			else
				return new IntervalGuard(aGuard.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(), intGuard2.getRightExpr(), intGuard2.getRightOpen());
		}
		
		throw new DecoratedRuntimeException("Invalid inequality merge pair").
		addDecoration("head guard", aGuard).addDecoration("next guard", withGuard);
	}
	
	
}
