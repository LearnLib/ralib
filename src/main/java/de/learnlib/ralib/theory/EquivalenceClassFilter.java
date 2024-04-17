package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class EquivalenceClassFilter<T> {

	private final List<DataValue<T>> equivClasses;
	private boolean useOptimization;

	public EquivalenceClassFilter(List<DataValue<T>> equivClasses, boolean useOptimization) {
		this.equivClasses = equivClasses;
		this.useOptimization = useOptimization;
	}

	public List<DataValue<T>> toList(SuffixValueRestriction restr,
			Word<PSymbolInstance> prefix, Word<ParameterizedSymbol> suffix, WordValuation valuation) {

		if (!useOptimization)
			return equivClasses;

		List<DataValue<T>> filtered = new ArrayList<>();

		ParameterGenerator pgen = new ParameterGenerator();
		SuffixValueGenerator svgen = new SuffixValueGenerator();
		Mapping<SymbolicDataValue, DataValue<?>> mapping = new Mapping<>();
		for (PSymbolInstance psi : prefix) {
			DataType[] dts = psi.getBaseSymbol().getPtypes();
			DataValue<?>[] dvs = psi.getParameterValues();
			for (int i = 0; i < dvs.length; i++) {
				Parameter p = pgen.next(dts[i]);
				mapping.put(p, dvs[i]);
			}
		}
		for (ParameterizedSymbol ps : suffix) {
			DataType[] dts = ps.getPtypes();
			for (int i = 0; i < dts.length; i++) {
				SuffixValue sv = svgen.next(dts[i]);
				DataValue<?> val = valuation.get(sv.getId());
				if (val != null)
					mapping.put(sv, val);
			}
		}

		GuardExpression expr = restr.toGuardExpression(mapping.keySet());
		for (DataValue<T> ec : equivClasses) {
			Mapping<SymbolicDataValue, DataValue<?>> ecMapping = new Mapping<SymbolicDataValue, DataValue<?>>();
			ecMapping.putAll(mapping);
			ecMapping.put(restr.getParameter(), ec);
			if (expr.isSatisfied(ecMapping)) {
				filtered.add(ec);
			}
		}
		return filtered;
	}
}
