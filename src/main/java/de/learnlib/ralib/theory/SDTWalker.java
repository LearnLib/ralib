package de.learnlib.ralib.theory;

import java.util.List;
import java.util.Map.Entry;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.oracles.mto.SDT;

public class SDTWalker {
	private SDTGuard sdt;
	public SDTWalker(SDTGuard sdt) {
		this.sdt = sdt;
	}
	
//	public SDTGuard walk(List<SDTGuard> guards) {
//		SDTGuard currentSubTree = this.sdt;
//		for (SDTGuard  guard : guards) {
//			currentSubTree = guard.unwrap().
//		}
//	}
	
	public SDTGuard walk(WordValuation suffixValuation) {
//		SDTGuard currentSubTree = this.sdt;
//		for (Entry<SuffixValue, DataValue<?>> entry : suffixValuation.entrySet()) {
//			for (SDTGuard  guard : this.sdt.unwrap()) {
//				guard.
//				if (guard.toExpr().isSatisfied(suffixValuation)) {
//					
//				}
//			}
//		}
		return null;
	}
}
