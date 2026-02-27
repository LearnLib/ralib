package de.learnlib.ralib.example.sdts;

import java.util.Collection;

import de.learnlib.query.Query;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class SDTOracle implements DataWordOracle {

	private SDT sdt = null;
	private Mapping<SymbolicDataValue, DataValue> registerMapping = null;
	private Constants consts = new Constants();

	public SDTOracle() {
	}

	public SDTOracle(SDT sdt, Mapping<SymbolicDataValue, DataValue> registerMapping, Constants consts) {
		this.sdt = sdt;
		this.registerMapping = registerMapping;
		this.consts = consts;
	}

	public SDTOracle(SDT sdt, Mapping<SymbolicDataValue, DataValue> registerMapping) {
		this(sdt, registerMapping, new Constants());
	}

	public void changeSDT(SDT sdt, Mapping<SymbolicDataValue, DataValue> registerMapping, Constants consts) {
		this.sdt = sdt;
		this.registerMapping = registerMapping;
		this.consts = consts;
	}

	public void changeSDT(SDT sdt, Mapping<SymbolicDataValue, DataValue> registerMapping) {
		changeSDT(sdt, registerMapping, new Constants());
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> query : queries) {
			if (sdt == null) {
				query.answer(false);
			}
			else {
				Word<PSymbolInstance> suffix = computeSuffix(query.getInput());
				Mapping<SymbolicDataValue, DataValue> vals = computeMapping(suffix);
				query.answer(sdt.isAccepting(vals, consts));
			}
		}
	}

	private Mapping<SymbolicDataValue, DataValue> computeMapping(Word<PSymbolInstance> suffix) {
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<SymbolicDataValue, DataValue>();
		int index = 1;
		for (PSymbolInstance psi : suffix) {
			DataType[] dts = psi.getBaseSymbol().getPtypes();
			DataValue[] dvs = psi.getParameterValues();
			for (int i = 0; i < dts.length; i++) {
				SuffixValue sv = new SuffixValue(dts[i], index);
				mapping.put(sv, dvs[i]);
				index++;
			}
		}
		mapping.putAll(registerMapping);
		return mapping;
	}

	private Word<PSymbolInstance> computeSuffix(Word<PSymbolInstance> word) {
		int variables = sdt.getSuffixValues().size();
		int params = DataWords.paramValLength(word);
		if (params < variables)
			throw new java.lang.IllegalArgumentException("Invalid parameter length");
		int n = word.length();
		while (params > variables) {
			n--;
			params = params - word.getSymbol(n).getBaseSymbol().getArity();
		}
		Word<PSymbolInstance> suffix = word.suffix(n);
		if (DataWords.paramValLength(suffix) != variables)
			throw new java.lang.IllegalArgumentException("Invalid parameter length");
		return suffix;
	}
}
