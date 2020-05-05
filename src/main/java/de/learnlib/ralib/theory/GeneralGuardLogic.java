package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.List;

/**
 * A guard logic that handles conjunction and disjunction in a theory-agnostic way.
 * For example, conjunction of two atomic guards results in an instance of a {@link SDTAndGuard} containing the guards.
 * Similarly, disjunction of two such guards produces an instance of a {@link SDTOrGuard} encapsulating them.
 * Guard logics of specific theories should refine these methods according to the theories.
 * 
 */
public class GeneralGuardLogic implements SDTGuardLogic{

	public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2) {
		assert guard1.getParameter().equals(guard2.getParameter());
		if (guard1.equals(guard2))
			return guard1;
		
		if (guard1 instanceof SDTTrueGuard) {
			return guard2;
		}
		
		if (guard2 instanceof SDTTrueGuard) {
			return guard1;
		}
		
		if (guard1 instanceof SDTAndGuard && guard2 instanceof SDTAndGuard) {
			List<SDTGuard> guards = new ArrayList<SDTGuard>(((SDTAndGuard) guard1).getGuards());
			guards.addAll(((SDTAndGuard) guard2).getGuards());
			return new SDTAndGuard(guard1.getParameter(), guards.toArray(new SDTGuard []{}));
		}
		
		if (guard1 instanceof SDTAndGuard  || guard2 instanceof SDTAndGuard) {
			SDTAndGuard andGuard = guard1 instanceof SDTAndGuard? 
					(SDTAndGuard) guard1 : (SDTAndGuard) guard2;
			SDTGuard otherGuard  = guard2 instanceof SDTAndGuard? 
					guard1 : guard2;
			SDTGuard[] conjuncts = andGuard.getGuards().toArray(new SDTGuard [andGuard.getGuards().size() + 1]);
			conjuncts[conjuncts.length - 1] = otherGuard;
			return new SDTAndGuard(guard1.getParameter(), conjuncts);
		}
		return new SDTAndGuard(guard1.getParameter(), guard1, guard2);
	}

	@Override
	public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2) {
		assert guard1.getParameter().equals(guard2.getParameter());
		if (guard1.equals(guard2))
			return guard1;
		
		if (guard1 instanceof SDTTrueGuard) {
			return guard1;
		}
		
		if (guard2 instanceof SDTTrueGuard) {
			return guard2;
		}
		
		if (guard1 instanceof SDTOrGuard && guard2 instanceof SDTOrGuard) {
			List<SDTGuard> guards = new ArrayList<SDTGuard>(((SDTOrGuard) guard1).getGuards());
			guards.addAll(((SDTOrGuard) guard2).getGuards());
			return new SDTOrGuard(guard1.getParameter(), guards.toArray(new SDTOrGuard []{}));
		}
		
		if (guard1 instanceof SDTOrGuard  || guard2 instanceof SDTOrGuard) {
			SDTOrGuard orGuard = guard1 instanceof SDTOrGuard? 
					(SDTOrGuard) guard1 : (SDTOrGuard) guard2;
			SDTGuard otherGuard  = guard2 instanceof SDTOrGuard? 
					guard1 : guard2;
			SDTGuard[] disjuncts = orGuard.getGuards().toArray(new SDTGuard [orGuard.getGuards().size() + 1]);
			disjuncts[disjuncts.length - 1] = otherGuard;
			return new SDTOrGuard(guard1.getParameter(), disjuncts);
		}
		
		if (guard1 instanceof SDTAndGuard  || guard2 instanceof SDTAndGuard) {
			SDTAndGuard andGuard = guard1 instanceof SDTAndGuard? 
					(SDTAndGuard) guard1 : (SDTAndGuard) guard2;
			SDTGuard otherGuard  = guard2 instanceof SDTAndGuard? 
					guard1 : guard2;


			List<SDTGuard> conjunctList = new ArrayList<>(andGuard.getGuards());
			int size = conjunctList.size();
			conjunctList.removeIf(el -> disjunction(el, otherGuard) instanceof SDTTrueGuard);
			if (size != conjunctList.size()) {
				if (conjunctList.size() == 0) 
					return new SDTTrueGuard(guard1.getParameter());
				else if (conjunctList.size() == 1) 
					return conjunctList.get(0); 
				else {
					SDTGuard[] conjuncts = conjunctList.toArray(new SDTGuard [] {});
					return new SDTAndGuard(guard1.getParameter(), conjuncts);
				}
			}
		}

		return new SDTOrGuard(guard1.getParameter(), guard1, guard2);
	}

}
