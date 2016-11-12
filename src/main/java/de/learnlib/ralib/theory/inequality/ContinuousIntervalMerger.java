//package de.learnlib.ralib.theory.inequality;
//
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import de.learnlib.ralib.data.DataValue;
//import de.learnlib.ralib.data.VarMapping;
//import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
//import de.learnlib.ralib.oracles.mto.SDT;
//import de.learnlib.ralib.theory.SDTGuard;
//import de.learnlib.ralib.theory.SDTTrueGuard;
//import de.learnlib.ralib.theory.equality.DisequalityGuard;
//import de.learnlib.ralib.theory.equality.EqualityGuard;
//import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq.MergeResult;
//import net.automatalib.commons.util.Pair;
//
//public class ContinuousIntervalMerger<T extends Comparable<T>> implements IntervalMerger<T>{
//
//	 public Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> tempGuards, Map<SDTGuard, DataValue<T>> instantiations) {
//		if (tempGuards.size() == 1) { // for true guard do nothing
//			return tempGuards;
//		}
//		
//		List<SDTGuard> sortedGuards = tempGuards.keySet().stream().sorted(new Comparator<SDTGuard>() {
//			public int compare(SDTGuard o1, SDTGuard o2) {
//				DataValue<T> dv1 = instantiations.get(o1);
//				DataValue<T> dv2 = instantiations.get(o2);
//				int ret = ((java.lang.Comparable) dv1.getId()).compareTo((java.lang.Comparable) dv2.getId());
//				// the generated guards can never have the same dv instantiation. In case they do, it signals collision and needs to be addressed.
//				if (ret == 0) {
//					throw new DecoratedRuntimeException("Different guards are instantiated with equal Dv")
//					.addDecoration("guard1:", o1).addDecoration("dv1", dv1)
//					.addDecoration("guard2:", o2).addDecoration("dv2", dv2);
//				}
//				return ret;
//			}
//		}).collect(Collectors.toList());
//
//		Map<SDTGuard, SDT> merged = mergeByMaximizingIntervals(sortedGuards, tempGuards);
//		return merged;
//	}
//
//	/**
//	 * Merges intervals by making two runs through a list of guards, sorted
//	 * according to their corresponding sdts. On the first run it merges
//	 * intervals from left to right, replacing (head, equ, next) constructs by
//	 * single larger intervals, true, and eq/diseq guards where possible. The
//	 * second run finalizes merging for the case where an eq/diseq merger is
//	 * possible.
//	 */
//	private LinkedHashMap<SDTGuard, SDT> mergeByMaximizingIntervals(List<SDTGuard> sortedGuards,
//			Map<SDTGuard, SDT> guardSdtMap) {
//		LinkedHashMap<SDTGuard, SDT> mergedFinal = new LinkedHashMap<SDTGuard, SDT>();
//		Iterator<SDTGuard> test = sortedGuards.iterator();
//		boolean expectIntv = true;
//		while(test.hasNext()) {
//			SDTGuard guard = test.next();
//			if (expectIntv)
//				assert guard instanceof IntervalGuard;
//			else
//				assert guard instanceof EqualityGuard;
//			expectIntv = !expectIntv;  
//		}
//		Iterator<SDTGuard> iter = sortedGuards.iterator();
//		IntervalGuard head = (IntervalGuard) iter.next();
//		SDT refStd = guardSdtMap.get(head);
//
//		do {
//			EqualityGuard equ = (EqualityGuard) iter.next();
//			IntervalGuard nextInterval = (IntervalGuard) iter.next();
//			Pair<LinkedHashMap<SDTGuard, SDT>, MergeResult> merge = merge(head, equ, nextInterval, refStd, guardSdtMap);
//			MergeResult result = merge.getSecond();
//			LinkedHashMap<SDTGuard, SDT> mergeMap = merge.getFirst();
//			List<SDTGuard> mergeList = mergeMap.keySet().stream().collect(Collectors.toList());
//			switch (result) {
//			// the closing of an interval, beginning of a new
//			case OLD_INTERVAL_AND_OLD_EQU:
//			case OLD_INTERVAL_AND_NEW_EQU:
//				mergedFinal.putAll(mergeMap);
//				head = nextInterval;
//				refStd = guardSdtMap.get(nextInterval);
//				if (!iter.hasNext()) {
//					mergedFinal.put(nextInterval, refStd);
//				}
//				break;
//			// a new interval was formed from the merger, this interval can be
//			// further extended unless it's the last from the list
//			case NEW_INTERVAL:
//				head = (IntervalGuard) mergeList.get(0);
//				if (!iter.hasNext()) {
//					mergedFinal.put(head, refStd);
//				}
//				break;
//			// true or ==, != mergers
//			case TRUE:
//			case NEW_EQU_AND_DISEQ:
//				mergedFinal.putAll(mergeMap);
//				break;
//			}
//		} while (iter.hasNext());
//
//		// if it progressed to an < == >, we run the process again (as there
//		// could be an
//		// equ/diseq merge that wasn't detected in the first run)
//		if (mergedFinal.size() == 3 && guardSdtMap.size() > 3) {
//			mergedFinal = mergeByMaximizingIntervals(mergedFinal.keySet().stream().collect(Collectors.toList()),
//					mergedFinal);
//		}
//
//		return mergedFinal;
//	}
//
//	static enum MergeResult {
//		NEW_INTERVAL, NEW_EQU_AND_DISEQ, TRUE, OLD_INTERVAL_AND_OLD_EQU, OLD_INTERVAL_AND_NEW_EQU;
//	}
//
//	/**
//	 * Returns a pair comprising a mapping from the merged guards to their
//	 * corresponding sdts, and an enum element describing the result.
//	 */
//	private Pair<LinkedHashMap<SDTGuard, SDT>, MergeResult> merge(IntervalGuard head, EqualityGuard eqGuard,
//			IntervalGuard nextInterval, SDT sdtHead, Map<SDTGuard, SDT> guardSdtMap) {
//		LinkedHashMap<SDTGuard, SDT> resGuards = new LinkedHashMap<>(3);
//		MergeResult resMerge = null;
//		SDT sdtNext = guardSdtMap.get(nextInterval);
//		SDT sdtEquality = guardSdtMap.get(eqGuard);
//
//		boolean isHeadEquivToNext = sdtNext.isEquivalent(sdtHead, new VarMapping());
//		boolean isHeadEquivToEqu = sdtEquality.isEquivalentUnderEquality(sdtHead, Arrays.asList(eqGuard));
//
//		// attempt to merge head, next and equ into more compact guards
//		if (isHeadEquivToNext) {
//			boolean isBiggerSmaller = head.isSmallerGuard() && nextInterval.isBiggerGuard();
//
//			if (isHeadEquivToEqu) {
//				// if head is equiv to both equ and next, then we can merge them
//				// into either an interval guard or a true guard
//				if (isBiggerSmaller) {
//					resGuards.put(new SDTTrueGuard(head.getParameter()), sdtHead);
//					resMerge = MergeResult.TRUE;
//				} else {
//					resGuards.put(
//							new IntervalGuard(head.getParameter(), head.isSmallerGuard() ? null : head.getLeftExpr(),
//									nextInterval.isBiggerGuard() ? null : nextInterval.getRightExpr()),
//							sdtHead);
//					resMerge = MergeResult.NEW_INTERVAL;
//				}
//			} else
//			// the head is head is equiv to next but not to eq, they may be
//			// merged to = and != if neither head nor next are interval
//			// guards
//			if (isBiggerSmaller) {
//				resGuards.put(eqGuard, sdtEquality);
//				resGuards.put(new DisequalityGuard(head.getParameter(), head.getRightExpr()), sdtHead);
//				resMerge = MergeResult.NEW_EQU_AND_DISEQ;
//			}
//		}
//
//		boolean isNextEquivToEqu = sdtEquality.isEquivalentUnderEquality(sdtNext, Arrays.asList(eqGuard));
//		// if head and next cannot be merged in any way, it could still be the
//		// case that eq can be merged with either of the two,
//		// in which case equ would be assigned the corresponding sdt.
//		if (resMerge == null) {
//			// if (isHeadEquivToEqu)
//			// resGuards.put(head, sdtEquality);
//			// else
//			// resGuards.put(head, sdtHead);
//			// resGuards.put(eqGuard, sdtEquality);
//			resGuards.put(head, sdtHead);
//			// resGuards.put(eqGuard, sdtEquality);
//			if (isHeadEquivToEqu)
//				resGuards.put(eqGuard, sdtHead);
//			else if (isNextEquivToEqu)
//				resGuards.put(eqGuard, sdtNext);
//			else
//				resGuards.put(eqGuard, sdtEquality);
//
//			resMerge = (isNextEquivToEqu || isHeadEquivToEqu) ? MergeResult.OLD_INTERVAL_AND_NEW_EQU
//					: MergeResult.OLD_INTERVAL_AND_OLD_EQU; // cannot merge b <
//															// s < a and s > a
//															// if a == s isn't
//															// equivalent
//		}
//
//		return new Pair<>(resGuards, resMerge);
//	}
//}
