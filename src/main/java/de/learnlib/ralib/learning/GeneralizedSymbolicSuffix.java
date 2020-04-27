/*
1q2q * Copyright (C) 2014-2015 The LearnLib Contributors
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
package de.learnlib.ralib.learning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class GeneralizedSymbolicSuffix implements SymbolicSuffix {

	private final Word<ParameterizedSymbol> actions;

	private final SuffixValue[] suffixValues;

	private final EnumSet<DataRelation>[][] suffixRelations;

	private final EnumSet<DataRelation>[] prefixRelations;

	public GeneralizedSymbolicSuffix(Word<ParameterizedSymbol> actions, EnumSet<DataRelation>[] prefixRelations,
			EnumSet<DataRelation>[][] suffixRelations) {

		this.actions = Word.fromList(actions.asList());
		this.prefixRelations = prefixRelations;
		this.suffixRelations = suffixRelations;

		SymbolicDataValueGenerator.SuffixValueGenerator valgen = new SymbolicDataValueGenerator.SuffixValueGenerator();

		int idx = 0;
		DataType[] types = DataWords.typesOf(actions);
		this.suffixValues = new SuffixValue[types.length];
		for (DataType t : types) {
			this.suffixValues[idx++] = valgen.next(t);
		}
	}

	public static GeneralizedSymbolicSuffix fullSuffix(Word<PSymbolInstance> suffix,
			Constants consts, Map<DataType, Theory> theories) {

		DataValue[] vals = DataWords.valsOf(suffix);
		EnumSet<DataRelation>[] prefixRelations = new EnumSet[vals.length];
		EnumSet<DataRelation>[][] suffixRelations = new EnumSet[vals.length][];

		for (int i = 0; i < vals.length; i++) {
			EnumSet<DataRelation> allRelations = EnumSet.of(DataRelation.ALL); // theories.get(vals[i].getType()).recognizedRelations();
			prefixRelations[i] = EnumSet.copyOf(allRelations);
			suffixRelations[i] = new EnumSet[i];
			for (int j = 0; j < i; j++)
				suffixRelations[i][j] = EnumSet.copyOf(allRelations);
		}
		Word<ParameterizedSymbol> suffixActs = DataWords.actsOf(suffix);

		return new GeneralizedSymbolicSuffix(suffixActs, prefixRelations, suffixRelations);
	}
	
	public static GeneralizedSymbolicSuffix fullSuffix(Word<ParameterizedSymbol> suffixActs,  Map<DataType, Theory> theories) {

		DataType[] types = DataWords.typesOf(suffixActs);
		int paramNum = types.length;
		EnumSet<DataRelation>[] prefixRelations = new EnumSet[paramNum];
		EnumSet<DataRelation>[][] suffixRelations = new EnumSet[paramNum][];

		for (int i = 0; i < paramNum; i++) {
			EnumSet<DataRelation> allRelations = EnumSet.of(DataRelation.ALL); // theories.get(vals[i].getType()).recognizedRelations();
			prefixRelations[i] = EnumSet.copyOf(allRelations);
			suffixRelations[i] = new EnumSet[i];
			for (int j = 0; j < i; j++) {
				if (types[i].equals(types[j])) 
					suffixRelations[i][j] = EnumSet.copyOf(allRelations);
				else
					suffixRelations[i][j] = EnumSet.noneOf(DataRelation.class);
			}
		}

		return new GeneralizedSymbolicSuffix(suffixActs, prefixRelations, suffixRelations);
	}

	public GeneralizedSymbolicSuffix(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts,
			Map<DataType, Theory> theories) {

		this.actions = DataWords.actsOf(suffix);
		DataValue[] concSuffixVals = DataWords.valsOf(suffix);
		this.suffixValues = new SuffixValue[concSuffixVals.length];
		this.prefixRelations = new EnumSet[concSuffixVals.length];
		this.suffixRelations = new EnumSet[concSuffixVals.length][];

		SymbolicDataValueGenerator.SuffixValueGenerator valgen = new SymbolicDataValueGenerator.SuffixValueGenerator();

		int psIdx = 0;
		int base = 0;

		int idx = 0;
		Map<DataType, List<DataValue>> groups = new HashMap<>();
		for (DataValue v : concSuffixVals) {
			this.suffixValues[idx] = valgen.next(v.getType());
			EnumSet<DataRelation> prefixRels = EnumSet.noneOf(DataRelation.class);

			while (idx >= base + actions.getSymbol(psIdx).getArity()) {
				base += actions.getSymbol(psIdx).getArity();
				psIdx++;
			}

			// find relations to previous suffix values
			Theory t = theories.get(v.getType());
			List<DataValue> prevSuffixValues = groups.get(v.getType());
			if (prevSuffixValues == null) {
				prevSuffixValues = new ArrayList<>();
				groups.put(v.getType(), prevSuffixValues);
			}

			List<DataValue> pvals = Arrays.asList(DataWords.valsOf(prefix, v.getType()));
			List<DataValue> cvals = new ArrayList<>(consts.values(v.getType()));

			List<EnumSet<DataRelation>> prels = t.getRelations(pvals, v);
			List<EnumSet<DataRelation>> crels = t.getRelations(cvals, v);
			List<EnumSet<DataRelation>> srels = t.getRelations(prevSuffixValues, v);
			// if (prels.size() > 0)
			// prels.set(0, EnumSet.of(DataRelation.ALL));

			// if (prefix.length() == 0 ||
			// actions.getSymbol(psIdx) instanceof OutputSymbol ||
			// psIdx == 0) {
			//
			// prefixRels.addAll(t.recognizedRelations());
			// }
			
			prels.stream().forEach((rels) -> {
				prefixRels.addAll(rels);
			});
			crels.stream().forEach((rels) -> {
				prefixRels.addAll(rels);
			});
			int lidx = 0;
			for (EnumSet<DataRelation> srel : srels) {
				// FIXME: not sure if this sufficient or if all prev. relations
				// needed
			}

			this.prefixRelations[idx] = prefixRels;
			this.suffixRelations[idx] = new EnumSet[idx];
			int typeIndex = 0;
			for (int i=0; i<idx; i++) {
				if(this.suffixValues[i].getType().equals(this.suffixValues[idx].getType())) {
					this.suffixRelations[idx][i] = srels.get(typeIndex);
					typeIndex++;
				} else
					this.suffixRelations[idx][i] = EnumSet.noneOf(DataRelation.class);
			}
			prevSuffixValues.add(v);
			idx++;
		}
	}

	public GeneralizedSymbolicSuffix(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix symSuffix,
			Constants consts, Map<DataType, Theory> theories) {

		// create suffix for prefix
		this.actions = Word.fromList(symSuffix.actions.asList()).prepend(DataWords.actsOf(prefix).lastSymbol());

		Word<PSymbolInstance> lastWord = prefix.suffix(1);
		prefix = prefix.prefix(-1);

		GeneralizedSymbolicSuffix pSuffix = new GeneralizedSymbolicSuffix(prefix, lastWord, consts, theories);

		// System.out.println("pSuffix: " + pSuffix);
		// System.out.println("symSuffix: " + symSuffix);

		int psLength = DataWords.valsOf(lastWord).length;
		int ssLength = symSuffix.suffixValues.length;

		this.suffixValues = new SuffixValue[psLength + ssLength];
		this.prefixRelations = new EnumSet[psLength + ssLength];
		this.suffixRelations = new EnumSet[psLength + ssLength][];

		SymbolicDataValueGenerator.SuffixValueGenerator valgen = new SymbolicDataValueGenerator.SuffixValueGenerator();

		for (int i = 0; i < psLength; i++) {
			this.suffixValues[i] = valgen.next(pSuffix.suffixValues[i].getType());
			this.prefixRelations[i] = pSuffix.prefixRelations[i];
			this.suffixRelations[i] = pSuffix.suffixRelations[i];
		}

		for (int i = 0; i < ssLength; i++) {
			this.suffixValues[psLength + i] = valgen.next(symSuffix.suffixValues[i].getType());

			this.prefixRelations[psLength + i] = symSuffix.prefixRelations[i];
			this.suffixRelations[psLength + i] = new EnumSet[psLength + i];
			
			for (int j = 0; j < psLength; j++) 
				this.suffixRelations[psLength + i][j] = EnumSet.noneOf(DataRelation.class); 
			
			for (int j = psLength; j < psLength + i; j++) 
				this.suffixRelations[psLength + i][j] = symSuffix.suffixRelations[i][j - psLength];
		}
	}

	public GeneralizedSymbolicSuffix(ParameterizedSymbol ps, Map<DataType, Theory> theories) {

		Map<Integer, DataValue> dvs = new HashMap<>();
		int idx = 1;
		for (DataType t : ps.getPtypes()) {
			DataValue dv = theories.get(t).getFreshValue(new ArrayList<>());
			// FIXME: maybe we have to keep track of generated values
			dvs.put(idx++, dv);
		}
		Word<PSymbolInstance> inst = DataWords.instantiate(Word.fromSymbols(ps), dvs);
		GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(inst, inst, new Constants(), theories);

		this.actions = Word.fromSymbols(ps);
		this.suffixValues = symSuffix.suffixValues;
		this.prefixRelations = symSuffix.prefixRelations;
		this.suffixRelations = symSuffix.suffixRelations;
		for (int i = 0; i < ps.getArity(); i++) {
			DataType t = ps.getPtypes()[i];
			this.prefixRelations[i] = theories.get(t).recognizedRelations();
			// this.prefixSources[i] = Sets.newHashSet(ParamSignature.ANY);
		}
	}
	
	/**
	 * Returns an exhaustive version of this suffix. All non-EMPTY and non-DEFAULT relation sets
	 * are replaced by ALL in the generated suffix.
	 */
	public GeneralizedSymbolicSuffix toExhaustiveSymbolicSuffix() {
		EnumSet<DataRelation>[][] suffixRelations = new EnumSet[this.size()][];
		EnumSet<DataRelation>[] prefixRelations = new EnumSet[this.size()];
		EnumSet<DataRelation> defRels = EnumSet.of(DataRelation.DEFAULT);
		for (int i=0; i<this.size(); i++) {
			if (!defRels.containsAll(this.prefixRelations[i]))
				prefixRelations[i] = EnumSet.of(DataRelation.ALL);
			else 
				prefixRelations[i] = EnumSet.copyOf(this.prefixRelations[i]);
			suffixRelations[i] = new EnumSet [i];
			for (int j=0; j<i; j++) 
				if (!defRels.containsAll(this.suffixRelations[i][j]))
					suffixRelations[i][j] = EnumSet.of(DataRelation.ALL);
				else 
					suffixRelations[i][j] = EnumSet.copyOf(this.suffixRelations[i][j]);
		}
		return new GeneralizedSymbolicSuffix(this.actions, prefixRelations, suffixRelations);
	}
	

	public SuffixValue getValue(int i) {
		return suffixValues[i - 1];
	}

	public Word<ParameterizedSymbol> getActions() {
		return actions;
	}

	public EnumSet<DataRelation> getPrefixRelations(int i) {
		return prefixRelations[i - 1];
	}
	
	/**
	 * Returns relation suffix s_j has with previous suffix s_i. j and i are
	 * from 1 to the number of suffix params.
	 */
	public EnumSet<DataRelation> getSuffixRelations(int i, int j) {
		DataType t = suffixValues[j - 1].getType();
		// have to count types to convert i
		int idx = -1;
		for (int c = 0; c < i; c++) {
			if (t.equals(suffixValues[c].getType())) {
				idx++;
			}
		}
		// System.out.println(i + "(" + idx+ ") : " + j);
		return suffixRelations[j - 1][i-1];
	}

	/**
	 * Returns all the relations suffix s_i has with past suffixes.
	 */
	public EnumSet<DataRelation> getSuffixRelations(int i) {
		// FIXME: support muliple types
		EnumSet<DataRelation> dset;
		if (i == 1) {
			dset = EnumSet.noneOf(DataRelation.class);
		} else {
			dset = EnumSet.noneOf(DataRelation.class);
			for (int j = 1; j < i; j++) {
				dset.addAll(this.getSuffixRelations(j, i));
			}
		}

		return dset;
	}

	/**
	 * Returns the left most suffix that is an the given relation with suffix
	 * s_pId.
	 */
	public SuffixValue findLeftMostRelatedSuffix(int pId, DataRelation rel) {
		// System.out.println("findLeftMostEqual (" + pId + "): " + suffix);
		DataType t = this.getDataValue(pId).getType();
		for (int i = 1; i < pId; i++) {
			if (!t.equals(this.getDataValue(i).getType())) {
				continue;
			}
			if (this.getSuffixRelations(i, pId).contains(rel))
				return this.getDataValue(i);
		}
		return null;
	}

	@Override
	public String toString() {
		Map<Integer, DataValue> instValues = new HashMap<>();
		int id = 1;
		for (DataValue v : suffixValues) {
			instValues.put(id++, v);
		}
		String suffixString = DataWords.instantiate(actions, instValues).toString();
		suffixString += "_P" + Arrays.deepToString(prefixRelations);
		suffixString += "_S" + Arrays.deepToString(suffixRelations);
		// if (this.prefixSources != null)
		// suffixString += "_PSrc" + Arrays.deepToString(prefixSources);
		return suffixString;
	}

	public SymbolicDataValue.SuffixValue getDataValue(int i) {
		return suffixValues[i - 1];
	}
	
	public int size() {
		return this.prefixRelations.length;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final GeneralizedSymbolicSuffix other = (GeneralizedSymbolicSuffix) obj;
		if (!Objects.equals(this.actions, other.actions)) {
			return false;
		}
		if (!Arrays.deepEquals(this.suffixValues, other.suffixValues)) {
			return false;
		}
		if (!Arrays.deepEquals(this.suffixRelations, other.suffixRelations)) {
			return false;
		}
		if (!Arrays.deepEquals(this.prefixRelations, other.prefixRelations)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 41 * hash + Objects.hashCode(this.actions);
		hash = 41 * hash + Arrays.deepHashCode(this.suffixValues);
		hash = 41 * hash + Arrays.deepHashCode(this.suffixRelations);
		hash = 41 * hash + Arrays.deepHashCode(this.prefixRelations);
		return hash;
	}

	public GeneralizedSymbolicSuffix suffix() {

		Word<ParameterizedSymbol> sActions = actions.suffix(actions.length() - 1);

		int arity = actions.firstSymbol().getArity();

		EnumSet[] sPrefixRels = deepCopy(prefixRelations, arity);
		EnumSet[][] sSuffixRels = new EnumSet[suffixRelations.length - arity][];

		for (int i = 0; i < sSuffixRels.length; i++) {
			sSuffixRels[i] = deepCopy(suffixRelations[i + arity], arity);

			for (int j = 0; j < arity; j++) {
				sPrefixRels[i].addAll(suffixRelations[i + arity][j]);
			}
		}

		return new GeneralizedSymbolicSuffix(sActions, sPrefixRels, sSuffixRels);
	}

	private EnumSet<DataRelation>[] deepCopy(EnumSet<DataRelation>[] array, int fromIndex) {
		EnumSet[] arr = new EnumSet[array.length - fromIndex];
		for (int i = fromIndex; i < array.length; i++)
			arr[i - fromIndex] = EnumSet.copyOf(array[i]);
		return arr;
	}

	private <T> Set<T>[] deepCopy(Set<T>[] array, int from) {
		if (array == null)
			return null;
		Set[] arr = new Set[array.length - from];
		for (int i = from; i < array.length; i++)
			arr[i - from] = new HashSet<>(array[i]);
		return arr;
	}
        
        
        public int rank() {
            int val = 0;
            for (EnumSet<DataRelation> dr : prefixRelations) {
                if (dr.contains(DataRelation.EQ)) {
                    val += 10;
                }
                if (dr.contains(DataRelation.EQ_SUMC1)) {
                    val += 10;
                }
                if (dr.contains(DataRelation.EQ_SUMC2)) {
                    val += 10;
                }
                if (dr.contains(DataRelation.LT)) {
                    val += 25;
                }
                if (dr.contains(DataRelation.ALL)) {
                    val += 55;
                }                
            }
            return val;
        }

		public boolean isFresh(int i) {
    		EnumSet<DataRelation> allRel = EnumSet.noneOf(DataRelation.class);
    		for (int j=0; j<i-1; j++)
    			allRel.addAll(suffixRelations[i-1][j]);
    		allRel.addAll(prefixRelations[i-1]);
    		return DataRelation.DEQ_DEF_RELATIONS.containsAll(allRel);
    	}
}
