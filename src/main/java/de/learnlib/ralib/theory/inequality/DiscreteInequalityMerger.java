package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumCDataExpression;
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
			SDT equSdt = guardSdtMap.get(equGuard).transform(g -> makeIntGuardsConsistent(g));
			SDT equSdt2 = guardSdtMap.get(equGuard2).transform(g -> makeIntGuardsConsistent(g));
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
	
	// Replaces x+1 <= p with x < p and p <= x+1 with p < x
	private SDTGuard makeIntGuardsConsistent(SDTGuard sdtGuard) {
		SDTGuard ret = sdtGuard;
		if (sdtGuard instanceof IntervalGuard) {
			IntervalGuard intGuard = (IntervalGuard) sdtGuard;
			if (Boolean.FALSE.equals(intGuard.getLeftOpen()) && intGuard.getLeftExpr() instanceof SumCDataExpression) {
				SumCDataExpression sumExpr = (SumCDataExpression) intGuard.getLeftExpr();
				if (sumExpr.getConstant().equals(DataValue.ONE(sumExpr.getConstant().getType()))) 
					intGuard = new IntervalGuard(intGuard.getParameter(), sumExpr.getSDV(), Boolean.TRUE, intGuard.getRightExpr(), intGuard.getRightOpen());
			}
			
			if (Boolean.FALSE.equals(intGuard.getRightOpen()) && intGuard.getRightExpr() instanceof SumCDataExpression) {
				SumCDataExpression sumExpr = (SumCDataExpression) intGuard.getRightExpr();
				if (sumExpr.getConstant().equals(DataValue.ONE(sumExpr.getConstant().getType()))) 
					intGuard = new IntervalGuard(intGuard.getParameter(), intGuard.getLeftExpr(), intGuard.getLeftOpen(),  sumExpr.getSDV(), Boolean.TRUE);
			}
			ret = intGuard;
		}
		return ret;
	}
}
