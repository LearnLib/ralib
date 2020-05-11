package de.learnlib.ralib.theory.inequality;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.IfElseGuardMerger;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTEquivalenceChecker;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

/*
 * TODO This likely needs to be reviewed/reworked.
 */
public class DiscreteDomainInequalityMerger implements InequalityGuardMerger{

	private SDTGuardLogic logic;

	public DiscreteDomainInequalityMerger() {
		this.logic = new InequalityGuardLogic();
	}
	
	public LinkedHashMap<SDTGuard, SDT> merge(List<SDTGuard> sortedInequalityGuards, final Map<SDTGuard, SDT> sdtMap, SDTEquivalenceChecker sdtChecker, Mapping<SymbolicDataValue, DataValue<?>>  valuation) {
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
			SDT equivTest = checkSDTEquivalence(head, next, mergedTemp, sdtChecker, valuation);
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
		// of the disjunction logic though that's narrowing down the scope of the logic, as you have to account concrete values)
		
		// we turn or guards to intervals where possible, that is whenever they contain IntervalGuards. 
		LinkedHashMap<SDTGuard, SDT> compressedResult = replaceOrGuardsWithIntervalsByIntervals(mergedResult);
		
		// we turn or guards with 3 or more equalities into interval guards. This ensures we can handle a case of an in-between
		// guard where the upper bound is incrementally increasing. 
		LinkedHashMap<SDTGuard, SDT> furtherCompressedResult = replaceOrGuardsWithEqualitiesByIntervals(compressedResult);
		
		// the other OR guards (which should only contain unmerge-able equality guards), are split
		LinkedHashMap<SDTGuard, SDT> noOrResult = splitOrGuardsToConstituents(furtherCompressedResult);
		
		// at this point we should have maximal intervals along with equality guards
		// it could be that some guards could be expressed by expressions in the adjacent guards above them 
		// for example, suppose guards <=r1 and ==r2 are adjacent (r2=r1+1). <= r1 can be expressed by
		// <r2, eliminating the need to keep r1 as a register
		// Another example, suppose == r1 and >= r1+1 are adjacent. We replace >= r1+1 by > r1. This maintains
		// consistency with the tree oracle implementation.
		LinkedHashMap<SDTGuard, SDT> regReducedResult = relabelByExpressingLowerExprByUpper(noOrResult);
		
		mergedResult = regReducedResult;
		
		if (mergedResult.size() > 1) {
			// we need to check if the merged guard fit into a pattern where all interval SDTs are equivalent, in which case, 
			// we can replace all interval guards by a conjunction of disequalities.
			SDTGuard [] intervalGuards = mergedResult.keySet().stream()
					.filter(sdt -> sdt instanceof IntervalGuard).toArray(SDTGuard []::new);
			boolean areEquivalent = true;
			SDT equivSDT = null;
			for (int i=1; i<intervalGuards.length; i++) {  
				equivSDT = checkSDTEquivalence(intervalGuards[i-1], intervalGuards[i], mergedResult, sdtChecker, valuation);
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
				LinkedHashMap<SDTGuard, SDT> result = ifElse.merge(eqDeqMergedResult, elseGuard, equivSDT, sdtChecker);
				return result;
			}	
		}
		return mergedResult;
	}
	
	private LinkedHashMap<SDTGuard, SDT> relabelByExpressingLowerExprByUpper(
			LinkedHashMap<SDTGuard, SDT> mergedResult) {
		final LinkedHashMap<SDTGuard, SDT> relabelledResult = new LinkedHashMap<>();
		SDTGuard[] guards = mergedResult.keySet().toArray(new SDTGuard []{});
		for (int i=0; i<guards.length-1; i++) {
			SDTGuard crtGuard = guards[i];
			SDTGuard nxtGuard = guards[i+1];
			SDTGuard newGuard;
			if (crtGuard instanceof IntervalGuard && 
					Boolean.FALSE.equals(((IntervalGuard) crtGuard).getRightOpen())) {
				SymbolicDataExpression nxtExpr;
				if (nxtGuard instanceof EqualityGuard)
					nxtExpr = ((EqualityGuard) nxtGuard).getExpression();
				else if (nxtGuard instanceof IntervalGuard) {
					nxtExpr = ((IntervalGuard) nxtGuard).getLeftExpr();
					//assert Boolean.FALSE.equals(((IntervalGuard) nxtGuard).getLeftOpen());
				} else {
					throw new DecoratedRuntimeException("Unexpected guard type").addDecoration("guard", nxtGuard);
				}
				// if the expressions are different, the second is a successor, we formulate the first guard in terms of the second expression
				// and change <= to < 
				if (!nxtExpr.equals(((IntervalGuard) crtGuard).getRightExpr()))
					newGuard = new IntervalGuard(crtGuard.getParameter(), ((IntervalGuard) crtGuard).getLeftExpr(), 
								((IntervalGuard) crtGuard).getLeftOpen(), nxtExpr, Boolean.TRUE);
				else 
					newGuard = crtGuard;
			} else 
				newGuard = crtGuard;
			
			SDT sdt = mergedResult.get(crtGuard);
			relabelledResult.put(newGuard, sdt);
		}
		relabelledResult.put(guards[guards.length-1], mergedResult.get(guards[guards.length-1]));
		return relabelledResult;
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
	
	private LinkedHashMap<SDTGuard, SDT> replaceOrGuardsWithIntervalsByIntervals(LinkedHashMap<SDTGuard, SDT> mergedResult) {
		final LinkedHashMap<SDTGuard, SDT> compressedResult = new LinkedHashMap<>();
		mergedResult.forEach((g,sdt) 
				-> compressedResult.put(compressOrWithInterval(g), sdt));
		return compressedResult;
	}
	
	private LinkedHashMap<SDTGuard, SDT> replaceOrGuardsWithEqualitiesByIntervals(
			LinkedHashMap<SDTGuard, SDT> mergedResult) {
		final LinkedHashMap<SDTGuard, SDT> compressedResult = new LinkedHashMap<>();
		mergedResult.forEach((g,sdt) 
				-> { 
					SDTGuard newGuard = compressOrWithEqualities(g);
					compressedResult.put(newGuard, sdt);
				}
				);
		return compressedResult;
	}
	
	
	/**
	 * Transforms an OR gate over ordered contiguous guards into an interval, provided the
	 * or's disjuncts comprise three or more equalities.
	 * 
	 *  E.g. s1 == r1 /\ s1 == r2 /\ s1 == r2+1 => r1 <= s1 <= r2+1
	 */
	private SDTGuard compressOrWithEqualities(SDTGuard sdtGuard) {
		if (! (sdtGuard instanceof SDTOrGuard) )
			return sdtGuard;
		SDTGuard newGuard = sdtGuard;
		List<SDTGuard> oldGuards = ((SDTOrGuard) newGuard).getGuards();
		if (oldGuards.stream().allMatch( guard -> guard instanceof EqualityGuard) && 
				(oldGuards.size() >= 2
				// TODO this should work even without the below condition. It doesn't ATM (something to do with eliminating or not the first input of the suffix)
				 && !((EqualityGuard)oldGuards.get(0)).getRegister().equals(((EqualityGuard)oldGuards.get(oldGuards.size()-1)).getRegister())
				 )) {
			
			SDTGuard first = oldGuards.get(0);
			SDTGuard last = oldGuards.get(oldGuards.size()-1);
			SymbolicDataExpression leftExpr = ((EqualityGuard) first).getExpression();
			SymbolicDataExpression rightExpr = ((EqualityGuard) last).getExpression();
			newGuard = new IntervalGuard(sdtGuard.getParameter(), leftExpr, Boolean.FALSE, rightExpr, Boolean.FALSE );
		}
		return newGuard;
	}
	
	/**
	 * Transforms an OR gate over ordered contiguous guards into an interval, provided there is
	 * at least one Interval Guard.
	 */
	private SDTGuard compressOrWithInterval(SDTGuard sdtGuard) {
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
			
			if (leftExpr == null && rightExpr == null)
				newGuard = new SDTTrueGuard(sdtGuard.getParameter());
			else
				newGuard = new IntervalGuard(sdtGuard.getParameter(), leftExpr, leftOpen, rightExpr, rightOpen);
		}
		return newGuard;
	}
	
	/**
	 * Checks if SDTs for guards are equivalent and if so, returns the preferred SDT.
	 * @param sdtChecker 
	 * @param valuation 
	 */
	SDT checkSDTEquivalence(SDTGuard guard, SDTGuard otherGuard, Map<SDTGuard, SDT> guardSdtMap, SDTEquivalenceChecker sdtChecker, Mapping<SymbolicDataValue,DataValue<?>> valuation) {
		
		
		SDT sdt = guardSdtMap.get(guard);
		SDT otherSdt = guardSdtMap.get(otherGuard);
		if (guard instanceof EqualityGuard)
			sdt = this.relabelEqualitySDT(sdt, (EqualityGuard)guard, valuation);
		if (otherGuard instanceof EqualityGuard)
			otherSdt = this.relabelEqualitySDT(otherSdt, (EqualityGuard)otherGuard, valuation);
		boolean equiv = sdtChecker.checkSDTEquivalence(guard, sdt, otherGuard, otherSdt);
		SDT retSdt = null;
		if (equiv) 
			if (guard instanceof EqualityGuard) //&& ((EqualityGuard) guard).isEqualityWithSDV())
				retSdt = otherSdt;
			else
				retSdt = sdt;
//		if (!equiv ) {
//			if (guard instanceof EqualityGuard && otherGuard instanceof EqualityGuard &&
//					!(sdt instanceof SDTLeaf)) {
//				SDT relSdt = this.relabelEqualitySDT(sdt, (EqualityGuard) guard, valuation);
//				SDT relOtherSdt = this.relabelEqualitySDT(otherSdt, (EqualityGuard) otherGuard, valuation);
//				equiv = sdtChecker.checkSDTEquivalence(guard, relSdt, otherGuard, relOtherSdt);
//				if (equiv) {
//					if (relSdt.getNumberOfLeaves() > relOtherSdt.getNumberOfLeaves())
//						return relSdt;
//					else 
//						return relOtherSdt;
//				}
//			}
//		}
		return retSdt;	
	}
	
	private SDT relabelEqualitySDT(SDT sdt, EqualityGuard equalityGuard, Mapping<SymbolicDataValue,DataValue<?>> valuation) {
		if (sdt instanceof SDTLeaf)
			return sdt;
		SymbolicDataExpression expr = equalityGuard.getExpression();
		DataValue<?> suffVal = expr.instantiateExprForValuation(valuation);
		Replacement repl = new Replacement();
		for (SymbolicDataValue sdv : valuation.keySet()) {
			//if (sdv.isRegister()) {
				DataValue regVal = valuation.get(sdv);
				if (regVal.equals(suffVal)) 
					repl.put(sdv, equalityGuard.getParameter());
				else 
					if (regVal instanceof SumCDataValue) {
					if (((SumCDataValue)regVal).getOperand().equals(suffVal)) 
						repl.put(sdv, new SumCDataExpression(equalityGuard.getParameter(), ((SumCDataValue) regVal).getConstant()));
				}
			//}
		}
		return (SDT) sdt.replace(repl);
	}
	
}
