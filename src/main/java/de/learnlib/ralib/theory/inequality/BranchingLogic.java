package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.ParamSignature;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class BranchingLogic<T extends Comparable<T>> {
	private DataType<T> type;
	private TypedTheory<T> theory;

	public BranchingLogic(TypedTheory<T> theory) {
		this.theory = theory;
		this.type = theory.getType();
	}

	public BranchingContext<T> computeBranchingContext(int pid, List<DataValue<T>> potential,
			Word<PSymbolInstance> prefix, Constants constants, SuffixValuation suffixValues,
			GeneralizedSymbolicSuffix suffix) {
		EnumSet<DataRelation> suffixRel = suffix.getSuffixRelations(pid);
		EnumSet<DataRelation> prefixRel = suffix.getPrefixRelations(pid);
		Set<ParamSignature> prefixSource = suffix.getPrefixSources(pid);
		List<DataValue<T>> sumC = constants.getSumCs(this.type);
		List<DataValue<T>> prefVals = Arrays.asList(DataWords.valsOf(prefix, this.type));
		
		
		Function<DataRelation, DataValue<T>> fromPrevSuffVal = (rel) -> {
			SuffixValue suffixValue = suffix.findLeftMostRelatedSuffix(pid, rel);
			DataValue<T> eqSuffix = (DataValue<T>) suffixValues.get(suffixValue);
			if (eqSuffix == null)
				eqSuffix = this.theory.getFreshValue(potential);
			else {
				if (rel == DataRelation.EQ)
					return eqSuffix;
				if (rel == DataRelation.EQ_SUMC1)
					return new SumCDataValue<T>(eqSuffix, sumC.get(0));
				if (rel == DataRelation.EQ_SUMC2)
					return new SumCDataValue<T>(eqSuffix, sumC.get(1));
			}
			return eqSuffix;
		};

		BranchingContext<T> action = null;
		// if any of the pref/suff relations contains all, we do FULL and skip
		if (prefixRel.contains(DataRelation.ALL) || suffixRel.contains(DataRelation.ALL))
			action = new BranchingContext<>(BranchingStrategy.FULL, potential);
		else {
			// branching processing based on relations
			if (DataRelation.DEQ_DEF_RELATIONS.containsAll(prefixRel)) {
				if (DataRelation.DEQ_DEF_RELATIONS.containsAll(suffixRel)) 
					action = new BranchingContext<T>(BranchingStrategy.TRUE_FRESH);
				else if (suffixRel.equals(EnumSet.of(DataRelation.EQ)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ));
				else if (suffixRel.equals(EnumSet.of(DataRelation.EQ_SUMC1)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC1));
				else if (suffixRel.equals(EnumSet.of(DataRelation.EQ_SUMC2)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC2));
				else if (suffixRel.equals(EnumSet.of(DataRelation.LT))) 
					action = new BranchingContext<T>(BranchingStrategy.TRUE_SMALLER);
			} 
			
			if (action == null) {
				if (DataRelation.EQ_DEQ_DEF_RELATIONS.containsAll(prefixRel)) {
					if (DataRelation.EQ_DEQ_DEF_RELATIONS.containsAll(suffixRel)) {
						List<DataValue<T>> newPotential = makeNewPots(pid, prefix, prefixSource, prefixRel, constants, suffixValues
								, suffix);
						action = new BranchingContext<T>(BranchingStrategy.IF_EQU_ELSE, newPotential);
					}
				}
			}
		}
		
		if (action == null)
			action = new BranchingContext<>(BranchingStrategy.FULL, potential);

		return action;
	}
	
	private List<DataValue<T>> makeNewPots(int pid, Word<PSymbolInstance> prefix, Set<ParamSignature> prefixSource, EnumSet<DataRelation> prefixRel, Constants constants, SuffixValuation suffixValues
			, GeneralizedSymbolicSuffix suffix) {
		List<DataValue<T>> newPotential = new ArrayList<>();
		List<DataValue<T>> sumC = constants.getSumCs(this.type);
		List<DataValue<T>> regVals = Arrays.asList(DataWords.valsOf(prefix, this.type)); 
				
//				regVals = prefixSource.stream()
//			.map(src -> src.getDataValuesWithSignature(prefix))
//			.flatMap(vals -> vals.stream())
//			.map(dv -> (DataValue<T>) dv)
//			.collect(Collectors.toList());
		
		List<DataValue<T>> regPotential = pots(regVals, sumC, prefixRel);
		Collection<DataValue<T>> sufVals = suffixValues.values(type);//this.getRelatedSuffixValues(suffix, pid, suffixValues);
		List<DataValue<T>> sufPotential = new ArrayList<DataValue<T>>();
		for (int i=1; i < pid; i++) {
			List<DataValue<T>> sufIPotential = pots(sufVals, sumC, suffix.getSuffixRelations(i, pid));
			sufPotential.addAll(sufIPotential);
		}
		
		newPotential.addAll(regPotential);
		newPotential.addAll(sufPotential);
		newPotential.addAll(constants.values(type));
		List<DataValue<T>> distinctPotential = newPotential.stream().distinct().collect(Collectors.toList());
		Collections.sort(distinctPotential, (dv1, dv2) -> dv1.getId().compareTo(dv2.getId()));
		return distinctPotential;
		
	}
	
	
	private List<DataValue<T>> pots(Collection<DataValue<T>> vals, List<DataValue<T>> sumConstants, EnumSet<DataRelation> equRels) {
		Set<DataValue<T>> newPots = new LinkedHashSet<>();
		if (equRels.contains(DataRelation.ALL) || equRels.contains(DataRelation.LT)) 
			newPots.addAll( this.theory.getPotential(new ArrayList<>(vals)));
		else {
			if (equRels.contains(DataRelation.EQ) || equRels.contains(DataRelation.DEQ))
				newPots.addAll(vals);
			if (equRels.contains(DataRelation.EQ_SUMC1) || equRels.contains(DataRelation.DEQ_SUMC1))
				vals.forEach(val -> newPots.add(new SumCDataValue<T>(val, sumConstants.get(0))));
			if (equRels.contains(DataRelation.EQ_SUMC2) || equRels.contains(DataRelation.DEQ_SUMC2))
				vals.forEach(val -> newPots.add(new SumCDataValue<T>(val, sumConstants.get(1))));
		}
		
		return new ArrayList<>(newPots);
	}
	
	private List<DataValue<T>> getRelatedSuffixValues(GeneralizedSymbolicSuffix suffix, int pId,
			SuffixValuation suffixValues, DataRelation ... relations) {
		List<DataValue<T>> relatedValues = new ArrayList<DataValue<T>>();
		//List<DataRelation> rels = Arrays.asList(relations);
		DataType<?> t = suffix.getDataValue(pId).getType();
		for (int i = 1; i < pId; i++) {
			if (!t.equals(suffix.getDataValue(i).getType())) {
				continue;
			}
			EnumSet<DataRelation> suffRelations = suffix.getSuffixRelations(i, pId);
			if (!suffRelations.isEmpty()) { //&& rels.contains(suffRelations)) {
				DataValue<T> suffValue = (DataValue<T>) suffixValues.get(suffix.getDataValue(i));
				relatedValues.add(suffValue);
			}
		}
		return relatedValues;
	}

	public static class BranchingContext<T> {
		private final BranchingStrategy strategy;
		private final List<DataValue<T>> branchingValues;

		public BranchingContext(BranchingStrategy strategy) {
			this.strategy = strategy;
			this.branchingValues = null;
		}

		public BranchingContext(BranchingStrategy strategy, DataValue<T> potValue) {
			this(strategy, Arrays.asList(potValue));
		}

		public BranchingContext(BranchingStrategy strategy, List<DataValue<T>> potValues) {
			this.strategy = strategy;
			this.branchingValues = potValues;
		}

		@SafeVarargs
		public BranchingContext(BranchingStrategy strategy, List<DataValue<T>>... potValues) {
			this.strategy = strategy;
			this.branchingValues = Arrays.stream(potValues).flatMap(potV -> potV.stream()).collect(Collectors.toList());
		}

		public List<DataValue<T>> getBranchingValues() {
			return this.branchingValues;
		}

		public DataValue<T> getBranchingValue() {
			if (this.branchingValues != null && !this.branchingValues.isEmpty())
				return branchingValues.get(0);
			return null;
		}

		public BranchingStrategy getStrategy() {
			return this.strategy;
		}
	}

	public static enum BranchingStrategy {
		TRUE_FRESH, TRUE_PREV, IF_EQU_ELSE, IF_INTERVALS_ELSE, FULL, TRUE_SMALLER;
	}
}
