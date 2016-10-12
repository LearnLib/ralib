/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory.inequality;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.commons.util.Pair;
import net.automatalib.words.Word;

/**
 *
 * @author Sofia Cassel
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T> implements Theory<T> {

	private static final LearnLogger log = LearnLogger.getLogger(InequalityTheoryWithEq.class);

	private void addAllSafely(Collection<SDTGuard> guards, Collection<SDTGuard> toAdd,
			List<SymbolicDataValue> regPotential) {
		// System.out.println("adding " + toAdd + " to " + guards);
		for (SDTGuard t : toAdd) {
			if (t instanceof SDTOrGuard) {
				// System.out.println("or guard");
				addAllSafely(guards, ((SDTOrGuard) t).getGuards(), regPotential);
				// System.out.println(guards);
			} else {
				addSafely(guards, t, regPotential);
			}
		}
		return;
	}

	private void addSafely(Collection<SDTGuard> guards, SDTGuard guard, List<SymbolicDataValue> regPotential) {
		boolean processed = false;
		if (guard instanceof SDTIfGuard) {
			SDTIfGuard oGuard = ((SDTIfGuard) guard).toDeqGuard();
			// System.out.println(guard + ": checking if guards contains " +
			// oGuard);
			if (guards.contains(oGuard)) {
				// System.out.println("yes, removing " + oGuard);
				guards.remove(oGuard);
				processed = true;
			}
		} else if (guard instanceof IntervalGuard) {
			// System.out.println("guard is intervalguard");
			IntervalGuard iGuard = (IntervalGuard) guard;
			if (!iGuard.isIntervalGuard()) {
				IntervalGuard flipped = iGuard.flip();
				// System.out.println("flipped: " + flipped);
				if (guards.contains(flipped)) {
					guards.remove(flipped);
					addAllSafely(guards, iGuard.mergeWith(flipped, regPotential), regPotential);
					processed = true;
				}
			}
		} else if (guard instanceof SDTOrGuard) {
			// System.out.println("found or guard");
			SDTOrGuard oGuard = (SDTOrGuard) guard;
			addAllSafely(guards, oGuard.getGuards(), regPotential);
		}

		if (processed == false) {
			guards.add(guard);
		}
		// System.out.println("added safely: " + guard + " to " + guards);
		return;
	}

	private void removeProhibited(Collection<SDTGuard> guards, Collection<SDTGuard> prohibited) {
		for (SDTGuard p : prohibited) {
			if (guards.contains(p)) {
				guards.remove(p);
			}
		}
		// System.out.println("guards after removing " + prohibited + " :: " +
		// guards);
		return;
	}

	private Set<SDTGuard> mergeOneWithSet(Set<SDTGuard> guardSet, SDTGuard target,
			List<SymbolicDataValue> regPotential) {
		List<Set<SDTGuard>> inAndOut = new ArrayList<Set<SDTGuard>>();
		inAndOut.add(guardSet);
		inAndOut.add(new LinkedHashSet<SDTGuard>());
		return mergeOneWithSet(inAndOut, target, regPotential).get(0);
	}

	private List<Set<SDTGuard>> mergeOneWithSet(List<Set<SDTGuard>> inAndOut, SDTGuard target,
			List<SymbolicDataValue> regPotential) {
		// merge target with the set guardSet
		Set<SDTGuard> guardSet = inAndOut.get(0);
		Set<SDTGuard> prohibited = inAndOut.get(1);

		Set<SDTGuard> newGuardSet = new LinkedHashSet<SDTGuard>();

		List<Set<SDTGuard>> newInAndOut = new ArrayList<Set<SDTGuard>>();
		newInAndOut.addAll(inAndOut);
		Boolean[] addedSomething = new Boolean[guardSet.size()];
		boolean processed = false;
		newGuardSet.addAll(guardSet);
		// System.out.println("target: " + target);
		int i = -1;
		if (!guardSet.isEmpty()) {
			for (SDTGuard m : guardSet) {
				// System.out.println("guards: " + newGuardSet + " target " +
				// target);
				i++;
				addedSomething[i] = false;
				// if target isn't in guardSet and isn't prohibited
				if (!(guardSet.contains(target)) && !(prohibited.contains(target))) {

					assert !(target instanceof SDTOrGuard && m instanceof SDTOrGuard);

					if (target instanceof SDTIfGuard) {
						// if it is an equality or disequality, remove the
						// opposite by adding both to prohibited
						SDTIfGuard oGuard = ((SDTIfGuard) target).toDeqGuard();
						// System.out.println(target + ": checking if guards
						// contains " + oGuard);
						if (guardSet.contains(oGuard)) {
							// System.out.println("yes, removing " + oGuard);
							prohibited.add(oGuard);
							prohibited.add(target);
							processed = true;
						}
					} else if (target instanceof IntervalGuard) {
						// System.out.println(target + " is iGuard");
						// if it is an interval guard, check if the set contains
						// flipped
						IntervalGuard iGuard = (IntervalGuard) target;
						if (!iGuard.isIntervalGuard()) {
							IntervalGuard flipped = iGuard.flip();
							// System.out.println("flipped: " + flipped);
							// System.out.println("guardSet " + guardSet + " " +
							// guardSet.contains(flipped));
							if (guardSet.contains(flipped)) {
								prohibited.add(flipped);
								prohibited.add(target);
								newInAndOut.add(0, newGuardSet);
								newInAndOut.add(1, prohibited);
								List<Set<SDTGuard>> nextInAndOut = mergeOneWithSet(newInAndOut, iGuard.toDeqGuard(),
										regPotential);
								newGuardSet = nextInAndOut.get(0);
								prohibited = nextInAndOut.get(1);//
								processed = true;
							}
						}
					}
					if (!processed) {
						Set<SDTGuard> refSet = new LinkedHashSet<>();
						refSet.add(target);
						refSet.add(m);
						Set<SDTGuard> mWithTarget = target.mergeWith(m, regPotential);
						// System.out.println("merging: " + target + " + " + m +
						// " --> " + mWithTarget + ", " +
						// refSet.containsAll(mWithTarget));
						if (!mWithTarget.contains(target)) {
							prohibited.add(target);
						}
						if (!mWithTarget.contains(m)) {
							prohibited.add(m);
						}
						addedSomething[i] = !(refSet.containsAll(mWithTarget));
						// System.out.println("g: " + guardSet);
						// System.out.println("n: " + newGuardSet);

						if (addedSomething[i]) { // &&
													// !(newGuardSet.containsAll(mWithTarget)))
													// {
							for (SDTGuard x : mWithTarget) {
								// List<Set<SDTGuard>> newInAndOut = new
								// ArrayList<Set<SDTGuard>>();
								// System.out.println("XXX: for " + x + ":
								// adding in: " + newGuardSet + " and out: " +
								// prohibited);
								newInAndOut.add(0, newGuardSet);
								newInAndOut.add(1, prohibited);
								List<Set<SDTGuard>> nextInAndOut = mergeOneWithSet(newInAndOut, x, regPotential);
								newGuardSet = nextInAndOut.get(0);
								prohibited = nextInAndOut.get(1);
								// System.out.println("XXX: after " + x + ": in:
								// " + newGuardSet + " and out: " + prohibited);
							}
						} else {
							newGuardSet.addAll(mWithTarget);
						}

						// newGuardSet.addAll(mWithTarget);
						// System.out.println("into: " + newGuardSet);
					}
				}

			}

		} else {
			newGuardSet.add(target);

		}
		newInAndOut.add(0, newGuardSet);
		newInAndOut.add(1, prohibited);
		// Set<SDTGuard> temp = new LinkedHashSet<>();
		// Set<SDTGuard> prohibited = new LinkedHashSet<>();
		// System.out.println("temp is: " + temp + " and prohibited: " +
		// prohibited);
		// for (SDTGuard m : merged) {
		// assert !(target instanceof SDTOrGuard && m instanceof SDTOrGuard);
		// Set<SDTGuard> mWithTarget = target.mergeWith(m);
		// System.out.println("merging: " + target + " + " + m + " --> " +
		// mWithTarget);
		// if (!mWithTarget.contains(target)) {
		// prohibited.add(target);
		// temp.remove(target);
		// }
		// if (!mWithTarget.contains(m)) {
		// prohibited.add(m);
		// temp.remove(target);
		// }
		// addAllSafely(temp, mWithTarget, prohibited);
		// System.out.println("... but after adding " + mWithTarget + " safely:
		// " + temp + " with " + prohibited);
		// }
		// System.out.println("return: " + temp);
		// return temp;
		// System.out.println("removing: " + prohibited);
		removeProhibited(newGuardSet, prohibited);
		// System.out.println("... and after removing :: " + newGuardSet);
		// System.out.println("modified is " + Arrays.toString(addedSomething));
		if (hasTrue(addedSomething) && !(guardSet.equals(newGuardSet))) {
			// System.out.println("try again");
			return mergeOneWithSet(newInAndOut, target, regPotential);
		} else {
			return newInAndOut;
		}
	}

	// Returns true if all elements of a boolean array are true.
	private boolean hasTrue(Boolean[] maybeArr) {
		boolean maybe = false;
		for (int c = 0; c < (maybeArr.length); c++) {
			// log.log(Level.FINEST,maybeArr[c]);
			if (maybeArr[c]) {
				maybe = true;
				break;
			}
		}
		return maybe;
	}

	private SDTGuard GuardSetFromList(Set<SDTGuard> merged, List<SDTGuard> remaining, SuffixValue currentParam,
			List<SymbolicDataValue> regPotential) {
		// merge list of guards with a set of guards. Initially, the set is
		// empty and the list is full
		// System.out.println("rem = " + remaining + ", merged = " + merged);

		// if the set is empty
		// and the set is also empty: return true guard
		// and the set is NOT empty: return either
		// if the set is empty
		if (remaining.isEmpty()) {
			// if (merged.isEmpty()) {
			// return new SDTTrueGuard(currentParam);
			// } else {
			SDTGuard[] guardArray = merged.toArray(new SDTGuard[] {});
			if ((merged.size() == 1)) { // || hasOnlyEqs(merged)) {
				return guardArray[0];
			} else {
				return new SDTOrGuard(currentParam, guardArray);
			}

			// }
		}
		if (merged.isEmpty()) {
			merged.add(remaining.remove(0));
			return GuardSetFromList(merged, remaining, currentParam, regPotential);
		}
		SDTGuard target = remaining.remove(0);
		Set<SDTGuard> temp = mergeOneWithSet(merged, target, regPotential);

		return GuardSetFromList(temp, remaining, currentParam, regPotential);
		// ensure that a new OrGuard is returned if there is nothing remaining
	}

	// private Set<SDTGuard> getHeads(Set<List<SDTGuard>> listSets) {
	// Set<SDTGuard> retSet = new LinkedHashSet<>();
	// for (List<SDTGuard> l : listSets) {
	// retSet.add(l.get(0));
	// }
	// return retSet;
	// }
	// private List<SDTGuard> getListFromHead(SDTGuard guard,
	// Set<List<SDTGuard>> listSets) {
	// Set<SDTGuard> heads = getHeads(listSets);
	// if (heads.contains(guard)) {
	// for (List<SDTGuard> l : listSets) {
	// if (l.get(0).equals(guard)) {
	// return l;
	// }
	// }
	// }
	// throw new IllegalStateException("not found");
	// }
	private Map<List<SDTGuard>, SDT> modGuardLists(SDTGuard refGuard, SDT refSDT,
			Map<List<SDTGuard>, SDT> partitionedMap) {
		boolean merged = false;
		Map<List<SDTGuard>, SDT> newParMap = new LinkedHashMap<>();
		// System.out.println("modGuardLists for refGuard " + refGuard + ",
		// refSDT " + refSDT + ", map: " + partitionedMap);
		for (Map.Entry<List<SDTGuard>, SDT> par : partitionedMap.entrySet()) {
			// merged = false;
			// System.out.println("par: " + par);
			List<SDTGuard> headList = par.getKey();
			List<SDTGuard> newHeadList = new ArrayList<>();
			newHeadList.addAll(headList);
			// addAllSafely(newHeadList, headList, new
			// LinkedHashSet<SDTGuard>());
			SDT headSDT = par.getValue();
			assert headSDT.getClass().equals(refSDT.getClass());
			// assert !(headSDT.isEmpty());
			// assert !(refSDT.isEmpty());
			SDTGuard headGuard = headList.get(0);
			SDT newSDT = headSDT;
			// System.out.println("head: " + newHeadList + " against " +
			// refGuard);
			if (headSDT instanceof SDTLeaf) {
				// System.out.println("sdt leaves");
				assert refSDT instanceof SDTLeaf;
				if (headSDT.isAccepting() == refSDT.isAccepting() && !merged) {
					merged = true;
					newHeadList.add(refGuard);
					// System.out.println("added ref " + refSDT + " eq to " +
					// headSDT);
				}
				// newParMap.put(newHeadList, newSDT);

			}
			if (refGuard instanceof IntervalGuard && !merged) {
				IntervalGuard iRefGuard = (IntervalGuard) refGuard;
				if (!iRefGuard.isIntervalGuard()) {
					EqualityGuard eqGuard = iRefGuard.toEqGuard();
					if (newHeadList.contains(eqGuard)) {
						// System.out.println("trying Deq " + refGuard + "
						// against " + newHeadList + " with " + eqGuard);
						SDT joinedSDT = getJoinedSDT(((IntervalGuard) refGuard).toEqGuard(), refSDT, headSDT);
						if (joinedSDT != null) {
							// System.out.println("can merge: " + refGuard + "
							// with EQ" + headList);
							newHeadList.add(refGuard);
							newSDT = joinedSDT;
							// newSDT = refSDT.relabelUnderEq((EqualityGuard)
							// headGuard);
							merged = true;
							// newParMap.put(newHeadList, newSDT);
						}
					}
				} else {
					assert iRefGuard.isIntervalGuard();
					EqualityGuard eqG = null;
					if (newHeadList.contains(new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftExpr()))) {
						eqG = new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftExpr());
					} else if (newHeadList
							.contains(new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getRightExpr()))) {
						eqG = new EqualityGuard(iRefGuard.getParameter(), iRefGuard.getLeftExpr());
					}
					if (eqG != null) {
						// System.out.println("trying Eq " + refGuard + "
						// against " + newHeadList + " with " + eqG);
						SDT joinedSDT = getJoinedSDT(eqG, refSDT, headSDT);
						if (joinedSDT != null) {
							// System.out.println("can merge: EQ" + headList + "
							// with " + refGuard);
							newHeadList.add(refGuard);
							newSDT = joinedSDT;
							merged = true;
							// newParMap.put(newHeadList, newSDT);
						}
					}
				}

			}
			if (refGuard instanceof EqualityGuard && !merged) {
				// System.out.println("trying Eq " + refGuard + " against " +
				// newHeadList);
				SDT joinedSDT = getJoinedSDT((EqualityGuard) refGuard, headSDT, refSDT);
				if (joinedSDT != null) {
					// System.out.println("can merge: EQ" + refGuard + " with "
					// + headList);
					newHeadList.add(0, refGuard);
					newSDT = joinedSDT;
					merged = true;
					// newParMap.put(newHeadList, newSDT);
				}
			}
			if (refSDT.canUse(headSDT) && headSDT.canUse(refSDT) && !merged) {
				// System.out.println("LAST RESORT: can use");
				newHeadList.add(refGuard);
				merged = true;
				// newParMap.put(newHeadList, newSDT);
			}

			newParMap.put(newHeadList, newSDT);

		}

		if (!merged) {
			// System.out.println(refGuard + " cannot merge with anything in " +
			// partitionedMap);
			List<SDTGuard> newUnmergedList = new ArrayList<>();
			newUnmergedList.add(refGuard);
			// newParMap.putAll(partitionedMap);
			// System.out.println("adding " + newUnmergedList + " ---> " +
			// refSDT);

			newParMap.put(newUnmergedList, refSDT);
			// System.out.println("newParMap is now " + newParMap);
		}

		// newParMap.putAll(partitionedMap);
		assert !newParMap.isEmpty();

		// System.out.println("RETURNED: newParMap " + newParMap);
		return newParMap;
	}

	private Map<List<SDTGuard>, SDT> partitionGuards(Map<SDTGuard, SDT> refMap) {

		// System.out.println("partitioning " + refMap);
		// start with an empty map
		Map<List<SDTGuard>, SDT> parMap = new LinkedHashMap<>();
		for (Map.Entry<SDTGuard, SDT> ref : refMap.entrySet()) {
			SDTGuard refGuard = ref.getKey();
			SDT refSDT = ref.getValue();

			// if (refGuard instanceof SDTTrueGuard) {
			// assert refMap.size() == 1;
			// List<SDTGuard> trueList = new ArrayList<SDTGuard>();
			// trueList.add(refGuard);
			// parMap = new LinkedHashMap<List<SDTGuard>, SDT>();
			// parMap.put(trueList, refSDT);
			// return parMap;
			// }
			// System.out.println("pre par map = " + parMap);
			parMap = modGuardLists(refGuard, refSDT, parMap);
			// get the heads of all lists in the partitioned map
			// System.out.println("partitioned map parMap: " + parMap);

		}
		// System.out.println("partitioned map parMap: " + parMap);

		return parMap;
	}

	// for each element in the map
	// check if it can merge with the head of any of the lists in the set
	// if yes, then add to that list
	// if no, then start a new list
	// }
	private SDT getJoinedSDT(EqualityGuard guard, SDT deqSDT, SDT eqSDT) {
		// boolean canJoin = false;

		EqualityGuard eqGuard = (EqualityGuard) guard;
		List<EqualityGuard> ds = new ArrayList<>();
		ds.add(eqGuard);
		// System.out.println("checking if T" + deqSDT + " is eq to O" + eqSDT +
		// " under " + eqGuard);

		SDT newTargetSDT = deqSDT.relabelUnderEq(ds);
		assert newTargetSDT != null;

		// System.out.println("checking if T" + deqSDT + " is eq to O" + eqSDT +
		// " under " + eqGuard);
		if (eqSDT.isEquivalentUnder(deqSDT, ds)) {
			// System.out.println("yes");
			// System.out.println("return target: " + deqSDT + " eq under " +
			// eqGuard);
			return deqSDT;

			// } else {
			// System.out.println("checking if O" + otherSDT + " is eq to NT" +
			// newTargetSDT + " under " + ds);
			//
			// if (newTargetSDT.canUse(otherSDT)) {
			// System.out.println("return newTarget: " + newTargetSDT + " canUse
			// ");
			//// System.out.println("yes");
			// return otherSDT;
			// }
			//
		}
		// } //else if (guard instanceof IntervalGuard) {
		// IntervalGuard iGuard = (IntervalGuard) guard;
		// List<SDTIfGuard> ds = new ArrayList();
		// if (!iGuard.isSmallerGuard()) {
		// ds.add(new EqualityGuard(iGuard.getParameter(),
		// iGuard.getLeftReg()));
		// }
		// if (!iGuard.isBiggerGuard()) {
		// ds.add(new EqualityGuard(iGuard.getParameter(),
		// iGuard.getRightReg()));
		// }
		// if (otherSDT.isEquivalentUnder(targetSDT, ds)) {
		// return true;
		// }
		// }
		return null;
	}

	// private Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> unmerged,
	// SuffixValue currentParam, List<SymbolicDataValue> regPotential) {
	// if (unmerged.keySet().size() == 1) {
	// assert unmerged.containsKey(new SDTTrueGuard(currentParam));
	// return unmerged;
	// }
	// Map<SDTGuard, SDT> merged = mergeGuards(new LinkedHashMap<SDTGuard,
	// SDT>(), currentParam, unmerged, regPotential);
	//
	// if (merged.keySet().size() == 1) {
	//
	//// System.out.println("unmerged: " + merged);
	// // hack??
	//// SDTGuard onlyGuard = merged.keySet().iterator().next();
	//// SDT onlySDT = merged.get(onlyGuard);
	//// if (!(onlyGuard instanceof SDTTrueGuard)) {
	//// assert onlyGuard instanceof SDTIfGuard;
	//// merged.remove(onlyGuard);
	//// merged.put(new SDTTrueGuard(currentParam), onlySDT);
	//// }
	// assert merged.containsKey(new SDTTrueGuard(currentParam));
	//
	// }
	//// System.out.println("merged: " + merged);
	// return merged;
	// }
	//
	// private Map<SDTGuard, SDT>
	// mergeGuards(Map<SDTGuard, SDT> first, SuffixValue currentParam,
	// Map<SDTGuard, SDT> second, List<SymbolicDataValue> regPotential) {
	//// System.out.println("comparing " + first + " with " + second);
	//// if (!first.keySet().isEmpty() && first.keySet().size() <=
	// second.keySet().size()) {
	//// System.out.println("return " + first);
	//// return first;
	//// } else {
	// // if they are not the same size, we want to keep merging them
	// Map<SDTGuard, SDT> newSecond = mgGuards(second, currentParam,
	// regPotential);
	//// return mergeGuards(second, currentParam, newSecond);
	// // }
	// return newSecond;
	// }
	// given a map from guards to SDTs, merge guards based on whether they can
	// use another SDT.
	private Map<SDTGuard, SDT> mgGuards(Map<SDTGuard, SDT> unmerged, SuffixValue currentParam,
			List<SymbolicDataValue> regPotential) {
		assert !unmerged.isEmpty();

		Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
		// System.out.println("unmerged: " + unmerged);
		Map<List<SDTGuard>, SDT> partitionedMap = partitionGuards(unmerged);
		// System.out.println("partitionedMap: " + partitionedMap);
		for (Map.Entry<List<SDTGuard>, SDT> par : partitionedMap.entrySet()) {

			// List<SDTGuard> preprocessed = new ArrayList<SDTGuard>();
			// preprocessed.addAll(par.getKey());
			// System.out.println("preprocessed: " + preprocessed);
			// addAllSafely(new ArrayList<SDTGuard>(), preprocessed, new
			// LinkedHashSet<SDTGuard>());
			// System.out.println("postprocessed: " + preprocessed);
			// System.out.println("partitioned map entry: " + par.getKey());
			SDTGuard newSDTGuard = GuardSetFromList(new LinkedHashSet<SDTGuard>(), par.getKey(), currentParam,
					regPotential);
			// System.out.println("--> Guard: " + newSDTGuard);
			if (newSDTGuard instanceof SDTOrGuard) {
				List<SDTGuard> subguards = ((SDTOrGuard) newSDTGuard).getGuards();
				if (subguards.isEmpty()) {
					retMap.put(new SDTTrueGuard(currentParam), par.getValue());
				} else {
					for (SDTGuard subguard : subguards) {
						retMap.put(subguard, par.getValue());
					}
				}
			} else {
				retMap.put(newSDTGuard, par.getValue());
			}
		}
		// System.out.println("retMap: " + retMap);
		assert !retMap.isEmpty();
		// System.out.println("-----------------------------------\n" +
		// partitionedMap +
		// "\n-----------------------PARTITIONING-------------------\n" + retMap
		// + "\n---------------------------------");
		return retMap;
	}

	private Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> tempGuards, Map<SDTGuard, DataValue<T>> instantiations) {
		if (tempGuards.size() == 1) { // for true guard do nothing
			return tempGuards;
		}
		List<SDTGuard> sortedGuards = tempGuards.keySet().stream().sorted(new Comparator<SDTGuard>() {
			public int compare(SDTGuard o1, SDTGuard o2) {
				DataValue<T> dv1 = instantiations.get(o1);
				DataValue<T> dv2 = instantiations.get(o2);
				return ((java.lang.Comparable) dv1.getId()).compareTo((java.lang.Comparable) dv2.getId());
			}
		}).collect(Collectors.toList());

		Map<SDTGuard, SDT> merged = mergeByMaximizingIntervals(sortedGuards, tempGuards);
		return merged;
	}

	/**
	 * Merges intervals by making two runs through a list of guards, sorted
	 * according to their corresponding sdts. On the first run it merges
	 * intervals from left to right, replacing (head, equ, next) constructs by
	 * single larger intervals, true, and eq/diseq guards where possible. The
	 * second run finalizes merging for the case where an eq/diseq merger is
	 * possible.
	 */
	private LinkedHashMap<SDTGuard, SDT> mergeByMaximizingIntervals(List<SDTGuard> sortedGuards,
			Map<SDTGuard, SDT> guardSdtMap) {
		LinkedHashMap<SDTGuard, SDT> mergedFinal = new LinkedHashMap<SDTGuard, SDT>();
		Iterator<SDTGuard> iter = sortedGuards.iterator();
		IntervalGuard head = (IntervalGuard) iter.next();
		SDT refStd = guardSdtMap.get(head);

		do {
			EqualityGuard equ = (EqualityGuard) iter.next();
			IntervalGuard nextInterval = (IntervalGuard) iter.next();
			Pair<LinkedHashMap<SDTGuard, SDT>, MergeResult> merge = merge(head, equ, nextInterval, refStd, guardSdtMap);
			MergeResult result = merge.getSecond();
			LinkedHashMap<SDTGuard, SDT> mergeMap = merge.getFirst();
			List<SDTGuard> mergeList = mergeMap.keySet().stream().collect(Collectors.toList());
			switch (result) {
			// the closing of an interval, beginning of a new
			case OLD_INTERVAL_AND_OLD_EQU:
			case OLD_INTERVAL_AND_NEW_EQU:
				mergedFinal.putAll(mergeMap);
				head = nextInterval;
				refStd = guardSdtMap.get(nextInterval);
				if (!iter.hasNext()) {
					mergedFinal.put(nextInterval, refStd);
				}
				break;
			// a new interval was formed from the merger, this interval can be
			// further extended unless it's the last from the list
			case NEW_INTERVAL:
				head = (IntervalGuard) mergeList.get(0);
				if (!iter.hasNext()) {
					mergedFinal.put(head, refStd);
				}
				break;
			// true or ==, != mergers
			case TRUE:
			case NEW_EQU_AND_DISEQ:
				mergedFinal.putAll(mergeMap);
				break;
			}
		} while (iter.hasNext());

		// if it progressed to an < == >, we run the process again (as there
		// could be an
		// equ/diseq merge that wasn't detected in the first run)
		if (mergedFinal.size() == 3 && guardSdtMap.size() > 3) {
			mergedFinal = mergeByMaximizingIntervals(mergedFinal.keySet().stream().collect(Collectors.toList()),
					mergedFinal);
		}

		return mergedFinal;
	}

	static enum MergeResult {
		NEW_INTERVAL, NEW_EQU_AND_DISEQ, TRUE, OLD_INTERVAL_AND_OLD_EQU, OLD_INTERVAL_AND_NEW_EQU;
	}

	/**
	 * Returns a pair comprising a mapping from the merged guards to their
	 * corresponding sdts, and an enum element describing the result.
	 */
	private Pair<LinkedHashMap<SDTGuard, SDT>, MergeResult> merge(IntervalGuard head, EqualityGuard eqGuard,
			IntervalGuard nextInterval, SDT sdtHead, Map<SDTGuard, SDT> guardSdtMap) {
		LinkedHashMap<SDTGuard, SDT> resGuards = new LinkedHashMap<>(3);
		MergeResult resMerge = null;
		SDT sdtNext = guardSdtMap.get(nextInterval);
		SDT sdtEquality = guardSdtMap.get(eqGuard);

		boolean isHeadEquivToNext = sdtNext.isEquivalent(sdtHead, new VarMapping());
		boolean isHeadEquivToEqu = sdtEquality.isEquivalentUnderEquality(sdtHead, Arrays.asList(eqGuard));

		// attempt to merge head, next and equ into more compact guards
		if (isHeadEquivToNext) {
			boolean isBiggerSmaller = head.isSmallerGuard() && nextInterval.isBiggerGuard();

			if (isHeadEquivToEqu) {
				// if head is equiv to both equ and next, then we can merge them
				// into either an interval guard or a true guard
				if (isBiggerSmaller) {
					resGuards.put(new SDTTrueGuard(head.getParameter()), sdtHead);
					resMerge = MergeResult.TRUE;
				} else {
					resGuards.put(
							new IntervalGuard(head.getParameter(), head.isSmallerGuard() ? null : head.getLeftExpr(),
									nextInterval.isBiggerGuard() ? null : nextInterval.getRightExpr()),
							sdtHead);
					resMerge = MergeResult.NEW_INTERVAL;
				}
			} else
			// the head is head is equiv to next but not to eq, they may be
			// merged to = and != if neither head nor next are interval
			// guards
			if (isBiggerSmaller) {
				resGuards.put(eqGuard, sdtEquality);
				resGuards.put(new DisequalityGuard(head.getParameter(), head.getRightExpr()), sdtHead);
				resMerge = MergeResult.NEW_EQU_AND_DISEQ;
			}
		}

		boolean isNextEquivToEqu = sdtEquality.isEquivalentUnderEquality(sdtNext, Arrays.asList(eqGuard));
		// if head and next cannot be merged in any way, it could still be the
		// case that eq can be merged with either of the two,
		// in which case equ would be assigned the corresponding sdt.
		if (resMerge == null) {
			// if (isHeadEquivToEqu)
			// resGuards.put(head, sdtEquality);
			// else
			// resGuards.put(head, sdtHead);
			// resGuards.put(eqGuard, sdtEquality);
			resGuards.put(head, sdtHead);
			// resGuards.put(eqGuard, sdtEquality);
			if (isHeadEquivToEqu)
				resGuards.put(eqGuard, sdtHead);
			else if (isNextEquivToEqu)
				resGuards.put(eqGuard, sdtNext);
			else
				resGuards.put(eqGuard, sdtEquality);

			resMerge = (isNextEquivToEqu || isHeadEquivToEqu) ? MergeResult.OLD_INTERVAL_AND_NEW_EQU
					: MergeResult.OLD_INTERVAL_AND_OLD_EQU; // cannot merge b <
															// s < a and s > a
															// if a == s isn't
															// equivalent
		}

		return new Pair<>(resGuards, resMerge);
	}

	// given a set of registers and a set of guards, keep only the registers
	// that are mentioned in any guard
	// private PIV keepMem(Set<SDTGuard> guardSet) {
	// PIV ret = new PIV();
	//
	// // 1. create guard list
	// Set<SDTGuard> ifGuards = new LinkedHashSet<>();
	// for (SDTGuard g : guardSet) {
	// if (g instanceof SDTIfGuard) {
	// ifGuards.add((SDTIfGuard) g);
	// } else if (g instanceof SDTOrGuard) {
	// ifGuards.addAll(((SDTOrGuard) g).getGuards());
	// }
	// }
	//
	// // 2. determine which registers to keep
	// Set<SymbolicDataValue> tempRegs = new LinkedHashSet<>();
	// for (SDTGuard g : ifGuards) {
	// if (g instanceof SDTAndGuard) {
	// tempRegs.addAll(((SDTAndGuard) g).getAllRegs());
	// } else if (g instanceof SDTIfGuard) {
	// tempRegs.add(((SDTIfGuard) g).getRegister());
	// }
	// }
	// for (SymbolicDataValue r : tempRegs) {
	// Parameter p = new Parameter(r.getType(), r.getId());
	// if (r instanceof Register) {
	// ret.put(p, (Register) r);
	// }
	// }
	// return ret;
	// }
	//
	private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
		PIV ret = new PIV();
		for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
			SDTGuard mg = e.getKey();
			if (mg instanceof SDTIfGuard) {
				log.log(Level.FINEST, mg.toString());
				SymbolicDataValue r = ((SDTIfGuard) mg).getRegister();
				Parameter p = new Parameter(r.getType(), r.getId());
				if (r instanceof Register) {
					ret.put(p, (Register) r);
				}
			} else if (mg instanceof IntervalGuard) {
				IntervalGuard iGuard = (IntervalGuard) mg;
				if (!iGuard.isBiggerGuard()) {
					SymbolicDataValue r = iGuard.getRightSDV();
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}

				}
				if (!iGuard.isSmallerGuard()) {
					SymbolicDataValue r = iGuard.getLeftSDV();
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}
				}
			} else if (mg instanceof SDTOrGuard) {
				Set<SymbolicDataValue> rSet = ((SDTOrGuard) mg).getAllRegs();
				for (SymbolicDataValue r : rSet) {
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}
				}
			} else if (!(mg instanceof SDTTrueGuard)) {
				throw new IllegalStateException("wrong kind of guard");
			}
		}
		return ret;
	}

	@Override
	public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, PIV piv,
			Constants constants, SuffixValuation suffixValues, SDTConstructor oracle) {

		int pId = values.size() + 1;
		List<SymbolicDataValue> regPotential = new ArrayList<>();
		SuffixValue sv = suffix.getDataValue(pId);
		DataType type = sv.getType();

		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

		DataValue<T>[] typedPrefixValues = DataWords.valsOf(prefix, type);
		WordValuation typedPrefixValuation = new WordValuation();
		for (int i = 0; i < typedPrefixValues.length; i++) {
			typedPrefixValuation.put(i + 1, typedPrefixValues[i]);
		}

		SuffixValue currentParam = new SuffixValue(type, pId);

		Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

		Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
				DataWords.<T>valSet(prefix, type), suffixValues.<T>values(type));

		List<DataValue<T>> potList = new ArrayList<>(potSet);
		List<DataValue<T>> potential = getPotential(potList);
		// WE ASSUME THE POTENTIAL IS SORTED
		int potSize = potential.size();
		Map<SDTGuard, DataValue<T>> guardDvs = new LinkedHashMap<>();
		
		// special case: fresh values in outputs
//        if (freshValues) {
//
//            ParameterizedSymbol ps = computeSymbol(suffix, pId);
//
//            if (ps instanceof OutputSymbol && ps.getArity() > 0) {
//
//                int idx = computeLocalIndex(suffix, pId);
//                Word<PSymbolInstance> query = buildQuery(
//                        prefix, suffix, values);
//                Word<PSymbolInstance> trace = ioOracle.trace(query);
//                PSymbolInstance out = trace.lastSymbol();
//
//                if (out.getBaseSymbol().equals(ps)) {
//
//                    DataValue d = out.getParameterValues()[idx];
//
//                    if (d instanceof FreshValue) {
//                        d = getFreshValue(potential);
//                        values.put(pId, d);
//                        WordValuation trueValues = new WordValuation();
//                        trueValues.putAll(values);
//                        SuffixValuation trueSuffixValues
//                                = new SuffixValuation();
//                        trueSuffixValues.putAll(suffixValues);
//                        trueSuffixValues.put(sv, d);
//                        SDT sdt = oracle.treeQuery(
//                                prefix, suffix, trueValues,
//                                piv, constants, trueSuffixValues);
//
//                        log.log(Level.FINEST,
//                                " single deq SDT : " + sdt.toString());
//
//                        Map<SDTGuard, SDT> merged = mergeGuards(tempKids,
//                                new SDTAndGuard(currentParam), sdt);
//
//                        log.log(Level.FINEST,
//                                "temporary guards = " + tempKids.keySet());
//                        log.log(Level.FINEST,
//                                "merged guards = " + merged.keySet());
//                        log.log(Level.FINEST,
//                                "merged pivs = " + piv.toString());
//
//                        return new SDT(merged);
//                    }
//                }
//            }
//        }

		// System.out.println("potential " + potential);
		if (potential.isEmpty()) {
			// System.out.println("empty potential");
			WordValuation elseValues = new WordValuation();
			DataValue<T> fresh = getFreshValue(potential);
			elseValues.putAll(values);
			elseValues.put(pId, fresh);

			// this is the valuation of the suffixvalues in the suffix
			SuffixValuation elseSuffixValues = new SuffixValuation();
			elseSuffixValues.putAll(suffixValues);
			elseSuffixValues.put(sv, fresh);

			SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, piv, constants, elseSuffixValues);
			tempKids.put(new SDTTrueGuard(currentParam), elseOracleSdt);
		} // process each '<' case
		else {
			// Parameter p = new Parameter(
			// currentParam.getType(), currentParam.getId());

			// smallest case
			WordValuation smValues = new WordValuation();
			smValues.putAll(values);
			SuffixValuation smSuffixValues = new SuffixValuation();
			smSuffixValues.putAll(suffixValues);

			Valuation smVal = new Valuation();
			DataValue<T> dvRight = potential.get(0);
			IntervalGuard sguard = makeSmallerGuard(dvRight, prefixValues, currentParam, smValues, piv, constants);
			SymbolicDataValue rsm = (SymbolicDataValue) sguard.getRightExpr();
			// System.out.println("setting valuation, symDV: " +
			// rsm.toVariable() + " dvright: " + dvRight);
			smVal.setValue(toVariable(rsm), dvRight.getId());
			DataValue<T> smcv = instantiate(sguard, smVal, constants, potential);
			smValues.put(pId, smcv);
			smSuffixValues.put(sv, smcv);

			SDT smoracleSdt = oracle.treeQuery(prefix, suffix, smValues, piv, constants, smSuffixValues);

			tempKids.put(sguard, smoracleSdt);
			guardDvs.put(sguard, smcv);

			// biggest case
			WordValuation bgValues = new WordValuation();
			bgValues.putAll(values);
			SuffixValuation bgSuffixValues = new SuffixValuation();
			bgSuffixValues.putAll(suffixValues);

			Valuation bgVal = new Valuation();

			DataValue<T> dvLeft = potential.get(potSize - 1);
			IntervalGuard bguard = makeBiggerGuard(dvLeft, prefixValues, currentParam, bgValues, piv, constants);
			updateValuation(bgVal, bguard.getLeftExpr(), dvLeft);
			DataValue<T> bgcv = instantiate(bguard, bgVal, constants, potential);
			bgValues.put(pId, bgcv);
			bgSuffixValues.put(sv, bgcv);

			SDT bgoracleSdt = oracle.treeQuery(prefix, suffix, bgValues, piv, constants, bgSuffixValues);

			tempKids.put(bguard, bgoracleSdt);
			guardDvs.put(bguard, bgcv);

			if (potSize > 1) { // middle cases
				for (int i = 1; i < potSize; i++) {

					WordValuation currentValues = new WordValuation();
					currentValues.putAll(values);
					SuffixValuation currentSuffixValues = new SuffixValuation();
					currentSuffixValues.putAll(suffixValues);
					// SDTGuard guard;
					Valuation val = new Valuation();
					DataValue<T> dvMRight = potential.get(i);
					DataValue<T> dvMLeft = potential.get(i - 1);

					// IntervalGuard smallerGuard = makeSmallerGuard(
					// dvMRight, prefixValues,
					// currentParam, currentValues, piv);
					// IntervalGuard biggerGuard = makeBiggerGuard(
					// dvMLeft, prefixValues, currentParam,
					// currentValues, piv);
					IntervalGuard intervalGuard = makeIntervalGuard(dvMLeft, dvMRight, prefixValues, currentParam,
							currentValues, piv, constants);

					// IntervalGuard guard = new IntervalGuard(
					// currentParam, biggerGuard.getLeftReg(),
					// smallerGuard.getRightReg());
					SymbolicDataValue rs = intervalGuard.getRightSDV();
					SymbolicDataValue rb = intervalGuard.getLeftSDV();
					updateValuation(val, intervalGuard.getRightExpr(), dvMRight);
					updateValuation(val, intervalGuard.getLeftExpr(), dvMLeft);

					DataValue<T> cv = instantiate(intervalGuard, val, constants, potential);
					currentValues.put(pId, cv);
					currentSuffixValues.put(sv, cv);

					SDT oracleSdt = oracle.treeQuery(prefix, suffix, currentValues, piv, constants,
							currentSuffixValues);

					tempKids.put(intervalGuard, oracleSdt);
					guardDvs.put(intervalGuard, cv);
					regPotential.add(i - 1, rb);
					regPotential.add(i, rs);
				}
			}
			// System.out.println("eq potential is: " + potential);
			for (DataValue<T> newDv : potential) {
				// log.log(Level.FINEST, newDv.toString());

				// this is the valuation of the suffixvalues in the suffix
				SuffixValuation ifSuffixValues = new SuffixValuation();
				ifSuffixValues.putAll(suffixValues); // copy the suffix
														// valuation

				// construct the equality guard. Depending on newDv, a certain
				// type of equality is instantiated (newDv can be a SumCDv, in
				// which case a SumC equality is instantiated)
				EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, constants);
				// log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());

				// we normalize newDv so that we only store plain data values in
				// the if suffix
				newDv = new DataValue<T>(newDv.getType(), newDv.getId());
				// construct the equality guard
				// find the data value in the prefix
				// this is the valuation of the positions in the suffix
				WordValuation ifValues = new WordValuation();
				ifValues.putAll(values);
				ifValues.put(pId, newDv);
				SDT eqOracleSdt = oracle.treeQuery(prefix, suffix, ifValues, piv, constants, ifSuffixValues);

				tempKids.put(eqGuard, eqOracleSdt);
				guardDvs.put(eqGuard, newDv);
			}

		}

		System.out.println("TEMPKIDS for " + prefix + " + " + suffix + " = " + tempKids);
		Map<SDTGuard, SDT> merged = mgGuards(tempKids, currentParam, regPotential);
		//Map<SDTGuard, SDT> merged = mergeGuards(tempKids, guardDvs);
		// Map<SDTGuard, SDT> merged = tempKids;
		// only keep registers that are referenced by the merged guards
		System.out.println("MERGED = " + merged);
		assert !merged.keySet().isEmpty();

		// System.out.println("MERGED = " + merged);
		piv.putAll(keepMem(merged));

		log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
		log.log(Level.FINEST, "merged guards = " + merged.keySet());
		log.log(Level.FINEST, "merged pivs = " + piv.toString());

		// clear the temporary map of children
		tempKids = new LinkedHashMap<SDTGuard, SDT>();

		for (SDTGuard g : merged.keySet()) {
			assert !(g == null);
			if (g instanceof SDTTrueGuard) {
				if (merged.keySet().size() != 1) {
					throw new IllegalStateException("only one true guard allowed: \n" + prefix + " + " + suffix);
				}
				// assert merged.keySet().size() == 1;
			}
		}
		System.out.println("MERGED = " + merged);
		SDT returnSDT = new SDT(merged);
		return returnSDT;

	}

	private DataValue<T> updateValuation(Valuation valuation, SymbolicDataExpression expr, DataValue<T> concValue) {
		SymbolicDataValue sdvForExpr = expr.getSDV();
		DataValue<T> sdvValuation;
		if (expr instanceof SymbolicDataValue) {
			sdvValuation = concValue;
		} else {
			if (expr instanceof SumCDataExpression) {
				sdvValuation = ((SumCDataValue<T>) concValue).getOperand();
			} else {
				throw new RuntimeException(
						"Cannot update valuation for expression " + expr + " assigned data value " + concValue);
			}
		}
		valuation.setValue(toVariable(sdvForExpr), sdvValuation.getId());
		return sdvValuation;
	}

	private IntervalGuard makeIntervalGuard(DataValue<T> biggerDv, DataValue<T> smallerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		IntervalGuard smallerGuard = makeSmallerGuard(smallerDv, prefixValues, currentParam, ifValues, pir, constants);
		IntervalGuard biggerGuard = makeBiggerGuard(biggerDv, prefixValues, currentParam, ifValues, pir, constants);
		return new IntervalGuard(currentParam, biggerGuard.getLeftExpr(), smallerGuard.getRightExpr());
	}

	private IntervalGuard makeBiggerGuard(DataValue<T> biggerDv, List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(biggerDv, prefixValues, currentParam, ifValues,
				constants);
		IntervalGuard bg = new IntervalGuard(currentParam, regOrSuffixExpr, null);
		return bg;
	}

	private IntervalGuard makeSmallerGuard(DataValue<T> smallerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(smallerDv, prefixValues, currentParam, ifValues,
				constants);
		IntervalGuard sg = new IntervalGuard(currentParam, null, regOrSuffixExpr);
		return sg;
	}

	private EqualityGuard pickupDataValue(DataValue<T> newDv, List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation ifValues, Constants constants) {
		DataType type = currentParam.getType();
		SymbolicDataExpression sdvExpr = getSDExprForDV(newDv, prefixValues, currentParam, ifValues, constants);
		return new EqualityGuard(currentParam, sdvExpr);
	}

	private SymbolicDataExpression getSDExprForDV(DataValue<T> dv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, Constants constants) {
		SymbolicDataValue SDV;
		if (dv instanceof SumCDataValue) {
			SumCDataValue<T> sumDv = (SumCDataValue<T>) dv;
			SDV = getSDVForDV(sumDv.toRegular(), prefixValues, currentParam, ifValues, constants);
			// if there is no previous value equal to the summed value, we pick
			// the data value referred by the sum
			// by this structure, we always pick equality before sumc equality
			// when the option is available
			if (SDV == null) {
				DataValue<T> constant = sumDv.getConstant();
				DataValue<T> prevDV = sumDv.getOperand();
				SymbolicDataValue prevSDV = getSDVForDV(prevDV, prefixValues, currentParam, ifValues, constants);
				return new SumCDataExpression(prevSDV, sumDv.getConstant());
			} else {
				return SDV;
			}
		} else {
			SDV = getSDVForDV(dv, prefixValues, currentParam, ifValues, constants);
			return SDV;
		}
	}

	private SymbolicDataValue getSDVForDV(DataValue<T> dv, List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation ifValues, Constants constants) {
		int newDv_i;
		DataType type = currentParam.getType();
		if (prefixValues.contains(dv)) {
			newDv_i = prefixValues.indexOf(dv) + 1;
			Register newDv_r = new Register(type, newDv_i);
			return newDv_r;
		}

		if (ifValues.containsValue(dv)) {
			int smallest = Collections.min(ifValues.getAllKeys(dv));
			return new SuffixValue(type, smallest);
		}

		if (constants.containsValue(dv)) {
			for (SymbolicDataValue.Constant c : constants.keySet()) {
				if (constants.get(c).equals(dv)) {
					return c;
				}
			}
		}

		return null;
	}

	public abstract List<DataValue<T>> getPotential(List<DataValue<T>> vals);

	private DataValue getRegisterValue(SymbolicDataValue r, PIV piv, List<DataValue> prefixValues, Constants constants,
			ParValuation pval) {
		if (r.isRegister()) {
			log.log(Level.FINEST, "piv: " + piv + " " + r.toString() + " " + prefixValues);
			Parameter p = piv.getOneKey((Register) r);
			log.log(Level.FINEST, "p: " + p.toString());
			int idx = p.getId();
			return prefixValues.get(idx - 1);
		} else if (r.isSuffixValue()) {
			Parameter p = new Parameter(r.getType(), r.getId());
			return pval.get(p);
		} else if (r.isConstant()) {
			return constants.get((SymbolicDataValue.Constant) r);
		} else {
			throw new IllegalStateException("this can't possibly happen");
		}
	}

	public abstract DataValue<T> instantiate(SDTGuard guard, Valuation val, Constants constants,
			Collection<DataValue<T>> alreadyUsedValues);

	private DataValue<T> instantiateSDExpr(SymbolicDataExpression sdExpr, DataType type, List<DataValue> prefixValues,
			PIV piv, ParValuation pval, Constants constants) {
		DataValue<T> returnThis = null;
		if (sdExpr.isRegister()) {
			Parameter p = piv.getOneKey((Register) sdExpr);
			int idx = p.getId();
			returnThis = prefixValues.get(idx - 1);
		} else if (sdExpr.isSuffixValue()) {
			Parameter p = new Parameter(type, ((SymbolicDataValue) sdExpr).getId());
			returnThis = (DataValue<T>) pval.get(p);
		} else if (sdExpr.isConstant()) {
			returnThis = (DataValue<T>) constants.get((SymbolicDataValue.Constant) sdExpr);
		} else if (sdExpr instanceof SumCDataExpression) {
			DataValue<T> opDv = instantiateSDExpr(sdExpr.getSDV(), type, prefixValues, piv, pval, constants);
			returnThis = (DataValue<T>) DataValue.add(opDv, ((SumCDataExpression) sdExpr).getConstant());
		}
		return returnThis;
	}

	@Override
	public DataValue<T> instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
			Constants constants, SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs) {

		DataType type = param.getType();
		DataValue<T> returnValue = null;
		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
		log.log(Level.FINEST, "prefix values : " + prefixValues.toString());

		if (guard instanceof EqualityGuard) {
			EqualityGuard eqGuard = (EqualityGuard) guard;
			returnValue = instantiateSDExpr(eqGuard.getExpression(), type, prefixValues, piv, pval, constants);
			assert returnValue != null;
		} else if (guard instanceof SDTTrueGuard || guard instanceof DisequalityGuard) {
			// might be a problem, what if we select an increment as a fresh
			// value ?
			Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
					DataWords.<T>valSet(prefix, type), pval.<T>values(type));

			returnValue = this.getFreshValue(new ArrayList<>(potSet));
		} else {
			Collection<DataValue<T>> alreadyUsedValues = DataWords.<T>joinValsToSet(constants.<T>values(type),
					DataWords.<T>valSet(prefix, type), pval.<T>values(type));
			Valuation val = new Valuation();
			// System.out.println("already used = " + alreadyUsedValues);
			if (guard instanceof IntervalGuard) {
				IntervalGuard iGuard = (IntervalGuard) guard;
				if (!iGuard.isBiggerGuard()) {
					SymbolicDataValue r = (SymbolicDataValue) iGuard.getRightSDV();
					DataValue<T> regVal = getRegisterValue(r, piv, prefixValues, constants, pval);

					val.setValue(toVariable(r), regVal.getId());
				}
				if (!iGuard.isSmallerGuard()) {
					SymbolicDataValue l = (SymbolicDataValue) iGuard.getLeftSDV();
					DataValue regVal = getRegisterValue(l, piv, prefixValues, constants, pval);

					val.setValue(toVariable(l), regVal.getId());
				}
				// instantiate(guard, val, param, constants);
			} else if (guard instanceof SDTIfGuard) {
				SymbolicDataValue r = ((SDTIfGuard) guard).getRegister();
				DataValue<T> regVal = getRegisterValue(r, piv, prefixValues, constants, pval);
				val.setValue(toVariable(r), regVal.getId());
			} else if (guard instanceof SDTMultiGuard) {
				List<SDTGuard> allGuards = ((SDTMultiGuard) guard).getGuards();
				List<SDTGuard> eqGuards = allGuards.stream().filter(g -> g instanceof EqualityGuard).distinct()
						.collect(Collectors.toList());
				if (!eqGuards.isEmpty()) {
					// if the end guard contains two equ guards it should not be
					// instantiateable
					if (eqGuards.size() > 1 && guard instanceof SDTAndGuard) {
						assert !instantiateSDExpr(((SDTIfGuard) eqGuards.get(0)).getExpression(), type, prefixValues,
								piv, pval, constants)
										.equals(instantiateSDExpr(((SDTIfGuard) eqGuards.get(1)).getExpression(), type,
												prefixValues, piv, pval, constants));
						return null;
					}

					SDTGuard firstEqGuard = eqGuards.get(0);
					DataValue<T> equValue = instantiateSDExpr(((SDTIfGuard) firstEqGuard).getExpression(), type,
							prefixValues, piv, pval, constants);
					assert equValue != null;
					// for an OR guard, we pick equality whenever there's an
					// equality guard contained
					if (guard instanceof SDTOrGuard)
						return equValue;
					// for an AND guard, we set the suffix so that it satisfied
					// the equality guard and then we check if this relation can
					// be satisfied
					else {
						val.setValue(toVariable(guard.getParameter()), equValue.getId());
						alreadyUsedValues.remove(equValue);
					}

				}

				Set<SymbolicDataValue> regs = ((SDTAndGuard) guard).getAllRegs();
				regs.forEach(reg -> {
					DataValue<T> regVal = getRegisterValue(reg, piv, prefixValues, constants, pval);
					val.setValue(toVariable(reg), regVal.getId());
				});

			} else {
				throw new IllegalStateException("only =, !=, intervals, AND and OR are allowed");
			}

			if (!(oldDvs.isEmpty())) {
				// System.out.println("old dvs: " + oldDvs);
				for (DataValue<T> oldDv : oldDvs) {
					Valuation newVal = new Valuation();
					newVal.putAll(val);
					newVal.setValue(toVariable(new SuffixValue(param.getType(), param.getId())), oldDv.getId());
					// System.out.println("instantiating " + guard + " with " +
					// newVal);
					DataValue inst = instantiate(guard, newVal, constants, alreadyUsedValues);
					if (inst != null) {
						return inst;
					}
				}
			}

			if (returnValue == null)
				returnValue = instantiate(guard, val, constants, alreadyUsedValues);

		}
		return returnValue;
	}
	


    private ParameterizedSymbol computeSymbol(SymbolicSuffix suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return a;
            }
        }
        return suffix.getActions().size() > 0
                ? suffix.getActions().firstSymbol() : null;
    }

    private int computeLocalIndex(SymbolicSuffix suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return pId - (idx - a.getArity()) - 1;
            }
        }
        return pId - 1;
    }

    private Word<PSymbolInstance> buildQuery(Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix, WordValuation values) {

        Word<PSymbolInstance> query = prefix;
        int base = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            if (base + a.getArity() > values.size()) {
                break;
            }
            DataValue[] vals = new DataValue[a.getArity()];
            for (int i = 0; i < a.getArity(); i++) {
                vals[i] = values.get(base + i + 1);
            }
            query = query.append(new PSymbolInstance(a, vals));
            base += a.getArity();
        }
        return query;
    }
}
