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
		type = theory.getType();
	}

	/**
	 * Computes the branching context for the given suffix parameter. 
	 * The branching context determines the strategy taken in exploring the parameter based on the generalized symbolic suffix associated with it.
	 * </p>
	 * For example, assume the parameter has only EQ/DEQ relations to previous suffix/prefix parameters. 
	 * Then it makes sense to only explore (dis-)equality with the values of these parameters, rather than also explore inequalities 
	 * (e.g. the suffix parameter taking a value that is in-between two parameters).
	 */
	public BranchingContext<T> computeBranchingContext(int pid, List<DataValue<T>> potential,
			Word<PSymbolInstance> prefix, Constants constants, SuffixValuation suffixValues,
			GeneralizedSymbolicSuffix suffix) {
		
		EnumSet<DataRelation> suffixRel = suffix.getSuffixRelations(pid);
		EnumSet<DataRelation> prefixRel = suffix.getPrefixRelations(pid);
		List<DataValue<T>> sumC = constants.getSumCs(type);
		
		
		Function<DataRelation, DataValue<T>> fromPrevSuffVal = (rel) -> {
			SuffixValue suffixValue = suffix.findLeftMostRelatedSuffix(pid, rel);
			DataValue<T> eqSuffix = (DataValue<T>) suffixValues.get(suffixValue);
			if (eqSuffix == null)
				eqSuffix = theory.getFreshValue(potential);
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

		BranchingContext<T> context = null;
		// if any of the pref/suff relations contains all, we do FULL and skip
		if (prefixRel.contains(DataRelation.ALL) || suffixRel.contains(DataRelation.ALL))
			context = new BranchingContext<>(BranchingStrategy.FULL, potential);
		else {
			// branching processing based on relations
			if (DataRelation.DEQ_DEF_RELATIONS.containsAll(prefixRel)) {
				if (DataRelation.DEQ_DEF_RELATIONS.containsAll(suffixRel)) 
					context = new BranchingContext<T>(BranchingStrategy.TRUE_FRESH);
				else if (suffixRel.equals(EnumSet.of(DataRelation.EQ)))
					context = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ));
				else if (suffixRel.equals(EnumSet.of(DataRelation.EQ_SUMC1)))
					context = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC1));
				else if (suffixRel.equals(EnumSet.of(DataRelation.EQ_SUMC2)))
					context = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC2));
			} 
			
			if (context == null) {
				if (DataRelation.EQ_DEQ_DEF_RELATIONS.containsAll(prefixRel)) {
					if (DataRelation.EQ_DEQ_DEF_RELATIONS.containsAll(suffixRel)) {
						List<DataValue<T>> newPotential = makeNewPots(pid, prefix, prefixRel, constants, suffixValues
								, suffix);
						context = new BranchingContext<T>(BranchingStrategy.IF_EQU_ELSE, newPotential);
					}
				}
			}
		}
		
		if (context == null)
			context = new BranchingContext<>(BranchingStrategy.FULL, new ArrayList<>(potential));
		
//		context.branchingValues.removeIf(dv -> dv.getId().compareTo(DataValue.ZERO(type).getId())<=0);

		return context;
	}
	
	private List<DataValue<T>> makeNewPots(int pid, Word<PSymbolInstance> prefix, EnumSet<DataRelation> prefixRel, Constants constants, SuffixValuation suffixValues
			, GeneralizedSymbolicSuffix suffix) {
		List<DataValue<T>> newPotential = new ArrayList<>();
		List<DataValue<T>> sumC = constants.getSumCs(type);
		List<DataValue<T>> regVals = new ArrayList<>(Arrays.asList(DataWords.valsOf(prefix, type)));
		Collection<DataValue<T>> sufVals = suffixValues.values(type);//this.getRelatedSuffixValues(suffix, pid, suffixValues);
		List<DataValue<T>> allVals = new ArrayList<DataValue<T>>(regVals);
		allVals.addAll(sufVals);
		
		allVals.removeAll(constants.getValues(type));
		sufVals.removeAll(constants.getValues(type));
		List<DataValue<T>> regPotential = pots(allVals, sumC, prefixRel);

		List<DataValue<T>> sufPotential = new ArrayList<DataValue<T>>();
		for (int i=1; i < pid; i++) {
			List<DataValue<T>> sufIPotential = pots(sufVals, sumC, suffix.getSuffixRelations(i, pid));
			sufPotential.addAll(sufIPotential);
		}
		
		newPotential.addAll(regPotential);
		newPotential.addAll(sufPotential);
	//	newPotential.addAll(constants.values(type));
		List<DataValue<T>> distinctPotential = newPotential.stream().distinct().collect(Collectors.toList());
		Collections.sort(distinctPotential, (dv1, dv2) -> dv1.getId().compareTo(dv2.getId()));
		return distinctPotential;
		
	}
	
	
	private List<DataValue<T>> pots(Collection<DataValue<T>> vals, List<DataValue<T>> sumConstants, EnumSet<DataRelation> equRels) {
		Set<DataValue<T>> newPots = new LinkedHashSet<>();
		if (equRels.contains(DataRelation.ALL) || equRels.contains(DataRelation.LT)) 
			newPots.addAll( theory.getPotential(new ArrayList<>(vals)));
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
			branchingValues = Collections.emptyList();
		}

		public BranchingContext(BranchingStrategy strategy, DataValue<T> potValue) {
			this(strategy, Arrays.asList(potValue));
		}

		public BranchingContext(BranchingStrategy strategy, List<DataValue<T>> potValues) {
			this.strategy = strategy;
			branchingValues = potValues;
		}

		@SafeVarargs
		public BranchingContext(BranchingStrategy strategy, List<DataValue<T>>... potValues) {
			this.strategy = strategy;
			branchingValues = Arrays.stream(potValues).flatMap(potV -> potV.stream()).collect(Collectors.toList());
		}

		public List<DataValue<T>> getBranchingValues() {
			return branchingValues;
		}

		public DataValue<T> getBranchingValue() {
			if (branchingValues != null && !branchingValues.isEmpty())
				return branchingValues.get(0);
			return null;
		}

		public BranchingStrategy getStrategy() {
			return strategy;
		}
		
		public String toString() {
			return "Strategy: " + strategy + (branchingValues.isEmpty()? "" : " Branching Values: " + branchingValues);
		}
	}

	public static enum BranchingStrategy {
		TRUE_FRESH, TRUE_PREV, IF_EQU_ELSE, IF_INTERVALS_ELSE, FULL, TRUE_SMALLER;
	}
}
