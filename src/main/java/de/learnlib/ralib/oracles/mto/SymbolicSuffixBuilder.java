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
package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SymbolicSuffixBuilder {

	public static GeneralizedSymbolicSuffix suffixFromSlice(Word<ParameterizedSymbol> actions, Slice slice) {

		System.out.println("Suffix from slice: " + slice);

		EnumSet<DataRelation>[] prefixRelations = new EnumSet[DataWords.paramLength(actions)];
		EnumSet<DataRelation>[][] suffixRelations = new EnumSet[DataWords.paramLength(actions)][];

		List<SuffixValue> svals = new ArrayList<>();

		SuffixValueGenerator sgen = new SuffixValueGenerator();
		int arityFirst = actions.firstSymbol().getArity();
		int idx = 0;
		for (DataType type : DataWords.typesOf(actions)) {
			SuffixValue sv = sgen.next(type);
			prefixRelations[idx] = close(slice.getPrefixRelationssFor(sv));
			suffixRelations[idx] = new EnumSet[svals.size()];
			int jdx = 0;
			for (SuffixValue prev : svals) {
				if (prefixRelations[idx].isEmpty()) {
					suffixRelations[idx][jdx] = (idx < arityFirst || isOutputIdx(idx, actions))
							? close(slice.getSuffixRelationsFor(prev, sv)) : slice.getSuffixRelationsFor(prev, sv);
				} else {
					suffixRelations[idx][jdx] = EnumSet.noneOf(DataRelation.class);
				}
				jdx++;
			}

			svals.add(sv);
			idx++;
		}

		GeneralizedSymbolicSuffix suffix = new GeneralizedSymbolicSuffix(actions, prefixRelations, suffixRelations);

		System.out.println("Suffix: " + suffix);
		return suffix;
	}

	private static EnumSet<DataRelation> close(EnumSet<DataRelation> set) {
		// if (!EnumSet.of(DataRelation.DEFAULT).containsAll(set)) {
		// return EnumSet.of(DataRelation.ALL);
		// } else {
		// return EnumSet.copyOf(set);
		// }
		EnumSet<DataRelation> lt = EnumSet.copyOf(set);
		lt.retainAll(DataRelation.LT_RELATIONS);
		if (!lt.isEmpty()) {
			return EnumSet.of(DataRelation.ALL);
		}
		EnumSet<DataRelation> ret = EnumSet.copyOf(set);
		if (ret.contains(DataRelation.EQ) || ret.contains(DataRelation.DEQ)) {
			ret.add(DataRelation.EQ);
			ret.add(DataRelation.DEQ);
		}

		if (ret.contains(DataRelation.EQ_SUMC1) || ret.contains(DataRelation.DEQ_SUMC1)) {
			ret.add(DataRelation.EQ_SUMC1);
			ret.add(DataRelation.DEQ_SUMC1);
		}

		if (ret.contains(DataRelation.EQ_SUMC2) || ret.contains(DataRelation.DEQ_SUMC2)) {
			ret.add(DataRelation.EQ_SUMC2);
			ret.add(DataRelation.DEQ_SUMC2);
		}

		return ret;
	}

	public static GeneralizedSymbolicSuffix suffixFromSliceRetainBranching(Word<ParameterizedSymbol> acts, Slice slice,
			SDT sdt) {

		System.out.println("Acts:" + acts);
		System.out.println("Slice:" + slice);
		System.out.println("Sdt:" + sdt);

		throw new UnsupportedOperationException("Not supported yet."); // To
																		// change
																		// body
																		// of
																		// generated
																		// methods,
																		// choose
																		// Tools
																		// |
																		// Templates.
	}

	private static boolean isOutputIdx(int idx, Word<ParameterizedSymbol> actions) {
		int offset = 0;
		for (ParameterizedSymbol a : actions) {
			int arity = a.getArity();
			offset += arity;
			if (idx < offset) {
				return (a instanceof OutputSymbol);
			}
		}
		throw new IllegalStateException("Should be unreachable.");
	}

}
