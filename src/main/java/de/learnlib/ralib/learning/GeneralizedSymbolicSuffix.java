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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;

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

	private Set<ParamSignature>[] prefixSources;

	public GeneralizedSymbolicSuffix(Word<ParameterizedSymbol> actions, EnumSet<DataRelation>[] prefixRelations,
			EnumSet<DataRelation>[][] suffixRelations, Set<ParamSignature>[] prefixSources) {

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
		this.prefixSources = prefixSources;
		// if( this.prefixRelations.length > 0) //&&
		// prefixRelations[0].isEmpty()) {
		// {
		// int symInd = 0;
		// for (symInd = 0; actions.getSymbol(symInd).getArity() == 0; symInd
		// ++);
		// int paramsInAction = actions.getSymbol(symInd).getArity();
		//
		// //removeUnneededRelations(this.prefixRelations, this.suffixRelations,
		// paramsInAction);
		// for (int i = 0; i < paramsInAction; i ++) {
		// this.prefixRelations[i] = EnumSet.of(DataRelation.ALL);
		// //extendRelationSet(this.prefixRelations[i]);
		// for (int sind = 0; sind < i; sind ++)
		// this.suffixRelations[i][sind] = EnumSet.of(DataRelation.ALL);
		// //extendRelationSet(this.suffixRelations[i][sind]);
		// }
		// }
	}

	public GeneralizedSymbolicSuffix(Word<ParameterizedSymbol> actions, EnumSet<DataRelation>[] prefixRelations,
			EnumSet<DataRelation>[][] suffixRelations) {
		this(actions, prefixRelations, suffixRelations, null);
	}

	public static GeneralizedSymbolicSuffix fullSuffix(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix,
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
		Set<ParamSignature>[] prefixSources = new Set[vals.length];
		Arrays.fill(prefixSources, Sets.newHashSet(ParamSignature.ANY));

		return new GeneralizedSymbolicSuffix(suffixActs, prefixRelations, suffixRelations, prefixSources);
	}

	public GeneralizedSymbolicSuffix(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts,
			Map<DataType, Theory> theories) {

		this.actions = DataWords.actsOf(suffix);
		DataValue[] concSuffixVals = DataWords.valsOf(suffix);
		this.suffixValues = new SuffixValue[concSuffixVals.length];
		this.prefixRelations = new EnumSet[concSuffixVals.length];
		this.suffixRelations = new EnumSet[concSuffixVals.length][];
		this.prefixSources = new Set[concSuffixVals.length];

		SymbolicDataValueGenerator.SuffixValueGenerator valgen = new SymbolicDataValueGenerator.SuffixValueGenerator();

		int psIdx = 0;
		int base = 0;

		int idx = 0;
		Map<DataType, List<DataValue>> groups = new HashMap<>();
		for (DataValue v : concSuffixVals) {
			this.suffixValues[idx] = valgen.next(v.getType());
			EnumSet<DataRelation> prefixRels = EnumSet.noneOf(DataRelation.class);
			Set<ParamSignature> prefixSrc = new LinkedHashSet<>();

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
			for (PSymbolInstance sym : prefix) {
				int index = 0;
				for (DataValue dv : sym.getParameterValues()) {
					if (dv.getType().equals(v.getType()) && !t.getRelations(Arrays.asList(dv), v).isEmpty()) {
						prefixSrc.add(new ParamSignature(sym.getBaseSymbol(), index));
					}
					index++;
				}
			}

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
			this.suffixRelations[idx] = srels.toArray(new EnumSet[] {});
			this.prefixSources[idx] = prefixSrc;
			prevSuffixValues.add(v);
			idx++;
		}
		
		this.extendRelationsOfFirstSuffixAction();
	}

	/**
	 * Extends the relation sets for the parameters of the first suffix action.
	 * Relations for these parameters should be such that branching based on
	 * these relations covers the whole parameter domain. This is required for
	 * building initial branchings of components. A simple implementation would
	 * replace existing pref/suff sets by the set {DataRelation.ALL} .
	 * 
	 */
	private void extendRelationsOfFirstSuffixAction() {
		if (this.prefixRelations.length > 0) // && prefixRelations[0].isEmpty())
												// {
		{
			int symInd = 0;
			for (symInd = 0; actions.getSymbol(symInd).getArity() == 0; symInd++);
			int paramsInAction = actions.getSymbol(symInd).getArity();

			for (int i = 0; i < paramsInAction; i++) {
				extendRelationSet(this.prefixRelations[i]);
				for (int sind = 0; sind < i; sind++)
					extendRelationSet(this.suffixRelations[i][sind]);
			}
		}
	}

	private void extendRelationSet(EnumSet<DataRelation> rels) {
		if (DataRelation.EQ_DEQ_DEF_RELATIONS.containsAll(rels)) {
			if (rels.contains(DataRelation.EQ))
				rels.add(DataRelation.DEQ);
			if (rels.contains(DataRelation.EQ_SUMC1))
				rels.add(DataRelation.DEQ_SUMC1);
			if (rels.contains(DataRelation.EQ_SUMC2))
				rels.add(DataRelation.DEQ_SUMC2);
		} else {
			rels.clear();
			rels.add(DataRelation.ALL);
		}

	}

	private void removeUnneededRelations(EnumSet<DataRelation>[] prefixRelations,
			EnumSet<DataRelation>[][] suffixRelations, int fromIndex) {
		for (int ind = fromIndex; ind < prefixRelations.length; ind++) {
			EnumSet<DataRelation> prefRel = prefixRelations[ind];
			EnumSet<DataRelation> suffRel = EnumSet.noneOf(DataRelation.class);
			for (int sind = 0; sind < ind; sind++)
				suffRel.addAll(suffixRelations[ind][sind]);
			EnumSet<DataRelation> allRel = EnumSet.copyOf(prefRel);
			allRel.addAll(suffRel);
			DataRelation eqRel = allRel.contains(DataRelation.EQ) ? DataRelation.EQ
					: allRel.contains(DataRelation.EQ_SUMC1) ? DataRelation.EQ_SUMC1
							: allRel.contains(DataRelation.EQ_SUMC2) ? DataRelation.EQ_SUMC2 : null;
			// if there is an equality relations, all other relations are
			// removed, as equality is enough to reproduce the value
			if (eqRel != null) {
				for (int sind = 0; sind < ind; sind++) {
					suffixRelations[ind][sind].removeIf(rel -> eqRel != rel);
				}
				prefRel.removeIf(rel -> eqRel != rel);
			}
		}
	}

	public GeneralizedSymbolicSuffix(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix symSuffix,
			Constants consts, Map<DataType, Theory> theories) {

		// create suffix for prefix
		this.actions = Word.fromList(symSuffix.actions.asList()).prepend(DataWords.actsOf(prefix).lastSymbol());

		Word<PSymbolInstance> suffix = prefix.suffix(1);
		prefix = prefix.prefix(-1);

		GeneralizedSymbolicSuffix pSuffix = new GeneralizedSymbolicSuffix(prefix, suffix, consts, theories);

		// System.out.println("pSuffix: " + pSuffix);
		// System.out.println("symSuffix: " + symSuffix);

		int psLength = DataWords.valsOf(suffix).length;
		int ssLength = symSuffix.suffixValues.length;

		this.suffixValues = new SuffixValue[psLength + ssLength];
		this.prefixRelations = new EnumSet[psLength + ssLength];
		this.suffixRelations = new EnumSet[psLength + ssLength][];
		this.prefixSources = new Set[psLength + ssLength];

		SymbolicDataValueGenerator.SuffixValueGenerator valgen = new SymbolicDataValueGenerator.SuffixValueGenerator();

		for (int i = 0; i < psLength; i++) {
			this.suffixValues[i] = valgen.next(pSuffix.suffixValues[i].getType());
			this.prefixRelations[i] = pSuffix.prefixRelations[i];
			this.suffixRelations[i] = pSuffix.suffixRelations[i];
			this.prefixSources[i] = pSuffix.prefixSources[i];
		}

		for (int i = 0; i < ssLength; i++) {
			this.suffixValues[psLength + i] = valgen.next(symSuffix.suffixValues[i].getType());

			this.prefixRelations[psLength + i] = symSuffix.prefixRelations[i];
			this.prefixSources[psLength + i] = symSuffix.prefixSources[i];

			int sameTypePrefix = DataWords.valsOf(suffix, this.suffixValues[psLength + i].getType()).length;

			int sameTypeSuffix = symSuffix.suffixRelations[i].length;

			this.suffixRelations[psLength + i] = new EnumSet[sameTypePrefix + sameTypeSuffix];

			for (int j = 0; j < sameTypePrefix; j++)
				this.suffixRelations[psLength + i][j] = EnumSet.noneOf(DataRelation.class);
			for (int j = sameTypePrefix; j < sameTypePrefix + sameTypeSuffix; j++)
				this.suffixRelations[psLength + i][j] = symSuffix.suffixRelations[i][j - sameTypePrefix];
		}
		
		this.extendRelationsOfFirstSuffixAction();
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
		this.prefixSources = symSuffix.prefixSources;
		for (int i = 0; i < ps.getArity(); i++) {
			DataType t = ps.getPtypes()[i];
			this.prefixRelations[i] = theories.get(t).recognizedRelations();
			// this.prefixSources[i] = Sets.newHashSet(ParamSignature.ANY);
		}
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

	public void setPrefixSources(Set<ParamSignature>[] prefixSources) {
		this.prefixSources = prefixSources;
	}

	/**
	 * Numbering from 1 to the number of parameters
	 */
	public ParamSignature getParamSignature(int pid) {
		if (this.actions.isEmpty())
			return null;
		else {
			Iterator<ParameterizedSymbol> iter = this.actions.iterator();
			ParameterizedSymbol act = null;
			int ind = 0;
			while (ind < pid & iter.hasNext()) {
				act = iter.next();
				ind += act.getArity();
			}
			if (ind < pid)
				return null;
			else {
				int indInAct = pid - (ind - act.getArity());
				return new ParamSignature(act, indInAct);
			}
		}

	}

	public Set<ParamSignature>[] getPrefixSources() {
		return this.prefixSources;
	}

	public Set<ParamSignature> getPrefixSources(int i) {
		if (prefixSources != null)
			return prefixSources[i - 1];
		return Collections.emptySet();
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
		return suffixRelations[j - 1][idx];
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
		if (!Arrays.deepEquals(this.prefixSources, other.prefixSources)) {
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
		hash = 41 * hash + Arrays.deepHashCode(this.prefixSources);
		return hash;
	}

	public GeneralizedSymbolicSuffix suffix() {

		Word<ParameterizedSymbol> sActions = actions.suffix(actions.length() - 1);

		int arity = actions.firstSymbol().getArity();

		EnumSet[] sPrefixRels = deepCopy(prefixRelations, arity);
		EnumSet[][] sSuffixRels = new EnumSet[suffixRelations.length - arity][];
		Set[] sPrefixSource = deepCopy(prefixSources, arity);

		for (int i = 0; i < sSuffixRels.length; i++) {
			sSuffixRels[i] = deepCopy(suffixRelations[i + arity], arity);

			for (int j = 0; j < arity; j++) {
				sPrefixRels[i].addAll(suffixRelations[i + arity][j]);
			}
		}

		ParameterizedSymbol first = actions.firstSymbol();
		for (int i = 0; i < sSuffixRels.length; i++) {
			for (int j = 0; j < arity; j++) {
				EnumSet<DataRelation> sufRel = suffixRelations[i + arity].length > 0 ? suffixRelations[i + arity][j]
						: EnumSet.noneOf(DataRelation.class);
				if (!sufRel.isEmpty()) {
					sPrefixSource[i].add(new ParamSignature(first, j));
				}

				sPrefixRels[i].addAll(sufRel);
			}
		}

		return new GeneralizedSymbolicSuffix(sActions, sPrefixRels, sSuffixRels, sPrefixSource);
	}

	private EnumSet<DataRelation>[] deepCopy(EnumSet<DataRelation>[] array, int fromIndex) {
		EnumSet[] arr = new EnumSet[array.length - fromIndex];
		for (int i = fromIndex; i < array.length; i++)
			arr[i - fromIndex] = EnumSet.copyOf(array[i]);
		return arr;
	}

	private <T> Set<T>[] deepCopy(Set<T>[] array, int from) {
		Set[] arr = new Set[array.length - from];
		for (int i = from; i < array.length; i++)
			arr[i - from] = new HashSet<>(array[i]);
		return arr;
	}
}
