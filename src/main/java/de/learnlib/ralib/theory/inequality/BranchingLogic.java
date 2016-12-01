package de.learnlib.ralib.theory.inequality;

import static de.learnlib.ralib.theory.DataRelation.EQ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class BranchingLogic<T> {
	private DataType<T> type;
	private TypedTheory<T> theory;

	public BranchingLogic(TypedTheory<T> theory) {
		this.theory = theory;
		this.type = theory.getType();
	}

	public BranchingContext<T> computeBranchingContext(int pid, List<DataValue<T>> potential, Word<PSymbolInstance> prefix,
			Constants constants, SuffixValuation suffixValues, GeneralizedSymbolicSuffix suffix) {
		EnumSet<DataRelation> suffixRel = getSuffixRelations(suffix, pid);
		EnumSet<DataRelation> prefixRel = suffix.getPrefixRelations(pid);
		Supplier<List<DataValue<T>>> eqDeqSuffVals = () -> getRelatedSuffixValues(suffix, pid, suffixValues,
				EnumSet.of(DataRelation.EQ, DataRelation.DEQ));
		Supplier<List<DataValue<T>>> prefVals = () -> Arrays.asList(DataWords.valsOf(prefix, this.type));
		Supplier<DataValue<T>> eqSuffVal = () -> {
			DataValue<T> eqSuffix = (DataValue<T>)suffixValues.get(findLeftMostEqualSuffix(suffix, pid));
			if (eqSuffix == null)
				eqSuffix = this.theory.getFreshValue(potential);
			return eqSuffix;
		};
		

		BranchingContext<T> action = new BranchingContext<>(BranchingStrategy.FULL, potential);
		if (prefixRel.contains(DataRelation.ALL) || suffixRel.contains(DataRelation.ALL))
			return action;

		// branching processing based on relations included
		if (prefixRel.isEmpty()) {
			if (suffixRel.isEmpty() || suffixRel.equals(EnumSet.of(DataRelation.DEQ)))
				action = new BranchingContext<T>(BranchingStrategy.TRUE_FRESH);
			else if (suffixRel.contains(EnumSet.of(DataRelation.EQ)))
				action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, eqSuffVal.get());
		} else {
			if (prefixRel.equals(EnumSet.of(DataRelation.DEQ))) {
				if (suffixRel.isEmpty() || suffixRel.equals(EnumSet.of(DataRelation.DEQ)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_FRESH);
				else if (suffixRel.contains(DataRelation.EQ))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, eqSuffVal.get());
			}

			else {
				if (EnumSet.of(DataRelation.EQ, DataRelation.DEQ).containsAll(prefixRel)) {
					if (suffixRel.contains(DataRelation.EQ))
						action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, eqSuffVal.get());
					else if (suffixRel.equals(DataRelation.DEQ)){ 
						action = new BranchingContext<T>(BranchingStrategy.IF_EQU_ELSE,
						 prefVals.get());
					} else if (suffixRel.isEmpty()) {
						action = new BranchingContext<T>(BranchingStrategy.IF_EQU_ELSE,
								 prefVals.get());
					}
				} 
			}
		}
		
		if (action.strategy == BranchingStrategy.TRUE_PREV && action.getBranchingValue() == null) {
			System.out.println("");
		} 

		//System.out.println(action.getStrategy());
		//return new BranchingContext<>(BranchingStrategy.FULL, potential);
		return action;
	}

	private List<DataValue<T>> getRelatedSuffixValues(GeneralizedSymbolicSuffix suffix, int pId,
			SuffixValuation suffixValues, EnumSet<DataRelation> relations) {
		List<DataValue<T>> relatedValues = new ArrayList<DataValue<T>>();
		DataType<?> t = suffix.getDataValue(pId).getType();
		for (int i = 1; i < pId; i++) {
			if (!t.equals(suffix.getDataValue(i).getType())) {
				continue;
			}
			EnumSet<DataRelation> suffRelations = suffix.getSuffixRelations(i, pId);
			if (relations.containsAll(suffRelations) && !suffRelations.isEmpty()) {
				DataValue<T> suffValue = (DataValue<T>) suffixValues.get(suffix.getDataValue(i));
				relatedValues.add(suffValue);
			}
		}
		return relatedValues;
	}

	private EnumSet<DataRelation> getSuffixRelations(GeneralizedSymbolicSuffix suffix, int idx) {
		// FIXME: support muliple types
		EnumSet<DataRelation> dset;
		if (idx == 1) {
			dset = EnumSet.noneOf(DataRelation.class);
		} else {
			dset = EnumSet.noneOf(DataRelation.class);
			for (int i = 1; i < idx; i++) {
				dset.addAll(suffix.getSuffixRelations(i, idx));
			}
		}

		return dset;
	}
	
	private int findLeftMostEqualSuffix(GeneralizedSymbolicSuffix suffix, int pId) {
		// System.out.println("findLeftMostEqual (" + pId + "): " + suffix);
		DataType t = suffix.getDataValue(pId).getType();
		for (int i = 1; i < pId; i++) {
			if (!t.equals(suffix.getDataValue(i).getType())) {
				continue;
			}
			if (suffix.getSuffixRelations(i, pId).contains(EQ))
				return i;
		}
		return -1;
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
		TRUE_FRESH, TRUE_PREV, IF_EQU_ELSE, IF_INTERVALS_ELSE, FULL,
	}
}
