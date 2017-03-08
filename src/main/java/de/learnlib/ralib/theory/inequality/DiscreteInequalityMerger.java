package de.learnlib.ralib.theory.inequality;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.theory.IfElseGuardMerger;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class DiscreteInequalityMerger extends ConcreteInequalityMerger{

	public DiscreteInequalityMerger() {
		super(new DiscreteInequalityGuardLogic());
	}
	
	public LinkedHashMap<SDTGuard, SDT> merge(List<SDTGuard> sortedInequalityGuards, final Map<SDTGuard, SDT> sdtMap) {
		LinkedHashMap<SDTGuard, SDT> mergedResult = new LinkedHashMap<>(); // contains the final merged result
		LinkedHashMap<SDTGuard, SDT> mergedTemp= new LinkedHashMap<>(sdtMap); // contains initial and all intermediate merging entries.
		if (sortedInequalityGuards.size() <= 2)  {
			for (SDTGuard g : sortedInequalityGuards)
				mergedResult.put(g, sdtMap.get(g));
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
		
		// compared to the concrete merger, we need these two extra steps (perhaps it should be a function
		// of the disjunction algorithm)
		
		// we turn or guards to intervals where possible, that is whenever they contain IntervalGuards. 
		LinkedHashMap<SDTGuard, SDT> compressedResult = changeOrGuardsWithIntervalsToIntervals(mergedResult);
		
		// the other OR guards (which should only contain unmerge-able equality guards), are split
		LinkedHashMap<SDTGuard, SDT> noOrResult = splitOrGuardsToConstituents(compressedResult);
		
		mergedResult = noOrResult;
		
		if (mergedResult.size() > 1) {
			// we need to check if the merged guard fit into a pattern where all interval SDTs are equivalent, in which case, 
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
			
			// equivalence means we can build using only equ and dequ guards
			if (areEquivalent) {
				final Map<SDTGuard, SDT> eqDeqMergedResult = new LinkedHashMap<>();
				
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
				
				// the if else branching might still be further merged, for which we use an if else merger.
				IfElseGuardMerger ifElse = new IfElseGuardMerger(this.logic);
				LinkedHashMap<SDTGuard, SDT> result = ifElse.merge(eqDeqMergedResult, elseGuard, equivSDT);
				return result;
			}	
		}
		return mergedResult;
	}
	
	private LinkedHashMap<SDTGuard, SDT> splitOrGuardsToConstituents(LinkedHashMap<SDTGuard, SDT> mergedResult) {
		final LinkedHashMap<SDTGuard, SDT> noOrGateResult = new LinkedHashMap<>();
		mergedResult.forEach((g,sdt)
				-> { if (g instanceof SDTOrGuard) 
					for (SDTGuard dis : ((SDTOrGuard) g).getGuards()) 
						noOrGateResult.put(dis, sdt);
				else 
					noOrGateResult.put(g, sdt);
				});
		return noOrGateResult;
	}
	
	private LinkedHashMap<SDTGuard, SDT> changeOrGuardsWithIntervalsToIntervals(LinkedHashMap<SDTGuard, SDT> mergedResult) {
		final LinkedHashMap<SDTGuard, SDT> compressedResult = new LinkedHashMap<>();
		mergedResult.forEach((g,sdt) 
				-> compressedResult.put(compress(g), sdt));
		return compressedResult;
	}
	
	/**
	 * Transforms an OR gate over contiguous guards into an interval, provided there is
	 * at least one Interval Guard. 
	 */
	private SDTGuard compress(SDTGuard sdtGuard) {
		if (! (sdtGuard instanceof SDTOrGuard) )
			return sdtGuard;
		SDTGuard newGuard = sdtGuard;
		List<SDTGuard> oldGuards = ((SDTOrGuard) newGuard).getGuards();
		if (oldGuards.stream().anyMatch( guard -> guard instanceof IntervalGuard)) {
			SDTGuard first = oldGuards.get(0);
			SDTGuard last = oldGuards.get(oldGuards.size()-1);
			Boolean leftOpen = null; 
			SymbolicDataExpression leftExpr = null;
			Boolean rightOpen = null; 
			SymbolicDataExpression rightExpr = null;
			if (first instanceof EqualityGuard) {
				leftExpr = ((EqualityGuard) first).getExpression();
				leftOpen = Boolean.FALSE;
			} else if (first instanceof IntervalGuard) {
				leftExpr= ((IntervalGuard) first).getLeftExpr();
				leftOpen = ((IntervalGuard) first).getLeftOpen();
			}
			
			if (last instanceof EqualityGuard) {
				rightExpr = ((EqualityGuard) last).getExpression();
				rightOpen = Boolean.FALSE;
			} else if (last instanceof IntervalGuard) {
				rightExpr= ((IntervalGuard) last).getRightExpr();
				rightOpen = ((IntervalGuard) last).getRightOpen();
			} else {
				throw new RuntimeException("Case not handled");
			}
			
			newGuard = new IntervalGuard(sdtGuard.getParameter(), leftExpr, leftOpen, rightExpr, rightOpen);
		}
		return newGuard;
	}
	
	
	SDT checkSDTEquivalence(SDTGuard guard, SDTGuard withGuard, Map<SDTGuard, SDT> guardSdtMap) {
		SDT res = null;
		if (guard instanceof EqualityGuard && withGuard instanceof EqualityGuard) {
			EqualityGuard equGuard = (EqualityGuard) guard;
			EqualityGuard equGuard2 = (EqualityGuard) withGuard;
			SDT equSdt = guardSdtMap.get(equGuard);//.transform(g -> makeIntGuardsConsistent(g));
			SDT equSdt2 = guardSdtMap.get(equGuard2);//.transform(g -> makeIntGuardsConsistent(g));
			if (equGuard.isEqualityWithSDV() && !equGuard2.isEqualityWithSDV()) {	
				List<EqualityGuard> eqGuards = Arrays.asList(equGuard, equGuard2);
				if ( equSdt.isEquivalentUnderEquality(equSdt2, eqGuards))
					res = equSdt2;  
			}
			else if (!equGuard.isEqualityWithSDV() && equGuard2.isEqualityWithSDV()) {	
				List<EqualityGuard> eqGuards = Arrays.asList(equGuard, equGuard2); 
				if ( equSdt2.isEquivalentUnderEquality(equSdt, eqGuards))
					res = equSdt;  
			} else if (!equGuard.isEqualityWithSDV() && !equGuard2.isEqualityWithSDV()) {
				if (equSdt.isEquivalent(equSdt2, new VarMapping())) 
					res = equSdt;
			} else {
//				throw new RuntimeException("Comparison of two eq guards not handled as of now. Situation: " + guard + " with sdt: \n " + equSdt + "\n " 
//			+ withGuard + " with sdt: \n" + equSdt2);
				return null;
				//res = super.checkSDTEquivalence(guard, withGuard, guardSdtMap);
			}
				
		} else {
			res = super.checkSDTEquivalence(guard, withGuard, guardSdtMap);
		}
	//	if (res == null)
	//		System.out.println("Not equivalent: \n" + guardSdtMap.get(guard) + " and \n" + guardSdtMap.get(withGuard));
		return res;
	}
}
