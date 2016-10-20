package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public abstract class TheoryBase<T> implements Theory<T> {
	private static final LearnLogger log = LearnLogger.getLogger(TheoryBase.class);

	public SDT treeQuery(
            Word<PSymbolInstance> prefix,             
            SymbolicSuffix suffix,
            WordValuation values, 
            PIV piv,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle) {
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
		
		List<SDTGuard> guards = generateGuardsFromPotential(potential, prefixValues, currentParam, values, piv, constants);
		for (SDTGuard guard : guards) {	
			Set<SymbolicDataValue> sdvs = guard.getAllSDVsFormingGuard();
			Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		}
		return null;
	}
 
	
	
	protected abstract List<SDTGuard> generateGuardsFromPotential(List<DataValue<T>> pot, List<DataValue> prefixValues, SuffixValue currentParam,
			WordValuation wordValues, PIV pir, Constants constants) ;
	
	// given a set of registers and a set of guards, keep only the registers
	// that are mentioned in any guard
	private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
		if (guardMap.keySet().stream().anyMatch(guard -> guard instanceof SDTTrueGuard))
			throw new IllegalStateException("wrong kind of guard");

		PIV ret = new PIV();
		for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
			SDTGuard mg = e.getKey();
			if (mg instanceof SDTTrueGuard)
				throw new IllegalStateException("wrong kind of guard");
			Set<SymbolicDataValue> regs = mg.getAllSDVsFormingGuard().stream().filter(r -> r instanceof Register)
					.collect(Collectors.toSet());
			regs.forEach(r -> ret.put(new Parameter(r.getType(), r.getId()), (Register) r));
		}
		return ret;
	}
	

	// retrieve the symbolic data expression associated with a concrete data value
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
}
