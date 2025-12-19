package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

public class EquivalenceClassFilter {

	private final List<DataValue> equivClasses;
	private final boolean useOptimization;

	public EquivalenceClassFilter(List<DataValue> equivClasses, boolean useOptimization) {
		this.equivClasses = equivClasses;
		this.useOptimization = useOptimization;
	}

	public List<DataValue> toList(AbstractSuffixValueRestriction restr,
			Word<PSymbolInstance> prefix, Word<ParameterizedSymbol> suffix, WordValuation valuation) {

		if (!useOptimization) {
			return equivClasses;
		}

		List<DataValue> filtered = new ArrayList<>();

		ParameterGenerator pgen = new ParameterGenerator();
		SuffixValueGenerator svgen = new SuffixValueGenerator();
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		for (PSymbolInstance psi : prefix) {
			DataType[] dts = psi.getBaseSymbol().getPtypes();
			DataValue[] dvs = psi.getParameterValues();
			for (int i = 0; i < dvs.length; i++) {
				Parameter p = pgen.next(dts[i]);
				if (restr.parameter.getDataType().equals(dts[i])) {
					mapping.put(p, dvs[i]);
				}
			}
		}
		for (ParameterizedSymbol ps : suffix) {
			DataType[] dts = ps.getPtypes();
			for (int i = 0; i < dts.length; i++) {
				SuffixValue sv = svgen.next(dts[i]);
				DataValue val = valuation.get(sv.getId());
				if (val != null && val.getDataType().equals(restr.parameter.getDataType())) {
					mapping.put(sv, val);
				}
			}
		}

		Expression<Boolean> expr = restr.toGuardExpression(mapping.keySet());
		for (DataValue ec : equivClasses) {
			Mapping<SymbolicDataValue, DataValue> ecMapping = new Mapping<>();
			ecMapping.putAll(mapping);
			ecMapping.put(restr.getParameter(), ec);
			//System.out.println(" -- " + expr + "  - " + ecMapping);
			if (expr.evaluateSMT(SMTUtil.compose(ecMapping))) {
				filtered.add(ec);
			}
		}
		return filtered;
	}
}
