package de.learnlib.ralib.oracles.mto;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.DisjunctionRestriction;
import de.learnlib.ralib.theory.ElementRestriction;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TrueRestriction;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class SLLambdaRestrictionBuilder extends SymbolicSuffixRestrictionBuilder {

	protected ConstraintSolver solver = new ConstraintSolver();  // should be in constructor

	public SLLambdaRestrictionBuilder(SymbolicSuffixRestrictionBuilder restrBuilder) {
		this(restrBuilder.consts, restrBuilder.teachers);
	}

	public SLLambdaRestrictionBuilder(Constants consts, Map<DataType, Theory> teachers) {
		super(consts, teachers);
		if (teachers == null) {
			throw new IllegalArgumentException("Non-null argument expected");
		}
	}

	public Map<SuffixValue, AbstractSuffixValueRestriction> restrictSuffix(Word<PSymbolInstance> prefix,
			Word<PSymbolInstance> suffix,
			Word<PSymbolInstance> u,
			RegisterValuation prefixValuation,
			RegisterValuation uValuation) {
		Map<SuffixValue, AbstractSuffixValueRestriction> restrs = new LinkedHashMap<>();
		DataValue[] suffixVals = DataWords.valsOf(suffix);
		for (int i = 0; i < suffixVals.length; i++) {
			SuffixValue sv = new SuffixValue(suffixVals[i].getDataType(), i+1);
			assert teachers != null;
			Theory theory = teachers.get(suffixVals[i].getDataType());
			restrs.put(sv, theory.restrictSuffixValue(sv, prefix, suffix, u, prefixValuation, uValuation, consts));
		}
		return restrs;
	}

	public SymbolicSuffix constructRestrictedSuffix(Word<PSymbolInstance> prefix,
			Word<PSymbolInstance> suffix,
			Word<PSymbolInstance> u,
			RegisterValuation prefixValuation,
			RegisterValuation uValuation) {
		return new SymbolicSuffix(DataWords.actsOf(suffix),
				restrictSuffix(prefix, suffix, u, prefixValuation, uValuation));
	}

	public SymbolicSuffix constructRestrictedSuffix(Word<PSymbolInstance> prefix,
			Word<PSymbolInstance> suffix,
			Word<PSymbolInstance> u1,
			Word<PSymbolInstance> u2,
			RegisterValuation prefixValuation,
			RegisterValuation u1Valuation,
			RegisterValuation u2Valuation) {
		Map<SuffixValue, AbstractSuffixValueRestriction> restr1 = restrictSuffix(prefix, suffix, u1, prefixValuation, u1Valuation);
		Map<SuffixValue, AbstractSuffixValueRestriction> restr2 = restrictSuffix(prefix, suffix, u2, prefixValuation, u2Valuation);
		Map<SuffixValue, AbstractSuffixValueRestriction> restr = new LinkedHashMap<>();
		for (SuffixValue s : restr1.keySet()) {
			AbstractSuffixValueRestriction r1 = restr1.get(s);
			AbstractSuffixValueRestriction r2 = restr2.get(s);
			if (!r1.equals(r2)) {
				restr.put(s, DisjunctionRestriction.create(s, r1, r2));
			} else {
				restr.put(s, r1);
			}
		}
		return new SymbolicSuffix(DataWords.actsOf(suffix), restr);
	}

	public static SymbolicSuffix concretize(SymbolicSuffix suffix, Mapping<? extends SymbolicDataValue, DataValue> ... valuations) {
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		for (Mapping<? extends SymbolicDataValue, DataValue> m : valuations) {
			mapping.putAll(m);
		}
		return concretize(suffix, mapping);
	}

	public static SymbolicSuffix concretize(SymbolicSuffix suffix, Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		Map<SuffixValue, AbstractSuffixValueRestriction> newRestrs = new LinkedHashMap<>();
		for (SuffixValue s : suffix.getValues()) {
			AbstractSuffixValueRestriction restr = suffix.getRestriction(s);
			AbstractSuffixValueRestriction concrRestr = restr.concretize(mapping);
			newRestrs.put(s, concrRestr);
		}
		return new SymbolicSuffix(suffix.getActions(), newRestrs);
	}

    public SymbolicSuffix extendSuffix(Prefix u1, Prefix u1Extended, Prefix u2, Prefix u2Extended, Prefix reprPrefix, SymbolicSuffix suffix, SDT sdt1, SDT sdt2) {
    	ParameterizedSymbol action = u1Extended.lastSymbol().getBaseSymbol();
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	Map<SuffixValue, AbstractSuffixValueRestriction> actionRestrictions = new LinkedHashMap<>();
    	for (DataType type : action.getPtypes()) {
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			EqualityTheory et = (EqualityTheory) theory;
    			AbstractSuffixValueRestriction r = et.restrictSuffixValue(s, u1, u1Extended.lastSymbol(), u1.getRegisters(), consts);
    			actionRestrictions.put(s, r);
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u1.getRpBijection().toVarMapping());

    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = isEqualityTheory(suffixTypes) ?
    			EqualityTheory.restrictionFromSDTs(sdt1, sdt2, u1Extended, u2Extended, u1.getRpBijection(), u2.getRpBijection(), consts, suffix, solver) :
    				genericRestrictions(suffix, u1, u1Extended, u2, u2Extended);

    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	restrictions.putAll(actionRestrictions);
    	restrictions.putAll(suffixRestrictions);

    	Word<ParameterizedSymbol> actions = DataWords.concatenate(Word.fromSymbols(action), suffixActions);
    	return new SymbolicSuffix(actions, restrictions);
    }

    public SymbolicSuffix extendSuffix(Prefix u, Prefix uIf, Prefix uElse, Prefix uRepr, SymbolicSuffix suffix, SDT sdtIf, SDT sdtElse) {
    	PSymbolInstance symbol = uIf.lastSymbol();
    	ParameterizedSymbol action = symbol.getBaseSymbol();
    	assert uElse.lastSymbol().getBaseSymbol().equals(action) : "Extensions do not match";
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	Map<SuffixValue, AbstractSuffixValueRestriction> actionRestrictions = new LinkedHashMap<>();
    	for (DataType type : action.getPtypes()) {
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			EqualityTheory et = (EqualityTheory) theory;
    			AbstractSuffixValueRestriction rIf = et.restrictSuffixValue(s, u, symbol, u.getRegisters(), consts);
    			AbstractSuffixValueRestriction r = DisjunctionRestriction.create(s, rIf, new FreshSuffixValue(s));
    			actionRestrictions.put(s, r);
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u.getRpBijection().toVarMapping());

    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = isEqualityTheory(suffixTypes) ?
    			EqualityTheory.restrictionFromSDTs(sdtIf, sdtElse, uIf, uElse, u.getRpBijection(), u.getRpBijection(), consts, suffix, solver) :
    				genericRestrictions(suffix, u, uIf, u, uElse);

    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	restrictions.putAll(actionRestrictions);
    	restrictions.putAll(suffixRestrictions);

    	Word<ParameterizedSymbol> actions = DataWords.concatenate(Word.fromSymbols(action), suffixActions);
    	return new SymbolicSuffix(actions, restrictions);
    }

    public SymbolicSuffix extendSuffix(Prefix u, Prefix uExtended, Prefix reprPrefix, SymbolicSuffix suffix, SDT sdt, Set<DataValue> missingRegisters) {
    	ParameterizedSymbol action = uExtended.lastSymbol().getBaseSymbol();
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	Map<SuffixValue, AbstractSuffixValueRestriction> actionRestrictions = new LinkedHashMap<>();
    	for (DataType type : action.getPtypes()) {
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			EqualityTheory et = (EqualityTheory) theory;
    			AbstractSuffixValueRestriction restr = et.restrictSuffixValue(s, u, uExtended.lastSymbol(), u.getRegisters(), missingRegisters, consts);
    			actionRestrictions.put(s, restr);
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u.getRpBijection().toVarMapping());

    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = isEqualityTheory(suffixTypes) ?
    			EqualityTheory.restrictionFromSDT(sdt, uExtended, u.getRpBijection(), consts, missingRegisters, suffix, solver) :
    				genericRestrictions(suffix, u, uExtended, u, uExtended);

    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	restrictions.putAll(actionRestrictions);
    	restrictions.putAll(suffixRestrictions);

    	Word<ParameterizedSymbol> actions = DataWords.concatenate(Word.fromSymbols(action), suffixActions);
    	return new SymbolicSuffix(actions, restrictions);
    }

    private SymbolicSuffix unrestricted(ParameterizedSymbol action, SymbolicSuffix suffix) {
    	Word<ParameterizedSymbol> actions = suffix.getActions();
    	DataType[] actionTypes = action.getPtypes();

    	SuffixValueGenerator sgen = new SuffixValueGenerator();
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();

    	for (int i = 0; i < actionTypes.length; i++) {
    		SuffixValue s = sgen.next(actionTypes[i]);
    		restrictions.put(s, new TrueRestriction(s));
    	}

    	int shift = action.getArity();
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : suffix.getRestrictions().entrySet()) {
    		SuffixValue s = sgen.next(e.getKey().getDataType());
    		restrictions.put(s, e.getValue().shift(shift));
    	}

    	return new SymbolicSuffix(actions, restrictions);
    }

    private boolean isEqualityTheory(DataType[] types) {
    	boolean isEquality = true;
    	for (DataType type : types) {
    		Theory theory = teachers.get(type);
    		if (theory == null || !(theory instanceof EqualityTheory)) {
    			isEquality = false;
    			break;
    		}
    	}
    	return isEquality;
    }

    private Map<SuffixValue, AbstractSuffixValueRestriction> genericRestrictions(SymbolicSuffix suffix, Prefix u1, Prefix u1Ext, Prefix u2, Prefix u2Ext) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = suffix.getRestrictions();
    	ret = AbstractSuffixValueRestriction.shift(ret, u1Ext.lastSymbol().getBaseSymbol().getArity());

    	Bijection<DataValue> u1Renaming = u1Ext.getBijection(u1Ext.getPath().getPrior(suffix)).inverse();
    	Bijection<DataValue> u2Renaming = u1Ext.getBijection(u1Ext.getPath().getPrior(suffix)).inverse();

    	Set<DataValue> vals = new LinkedHashSet<>();
    	AbstractSuffixValueRestriction.getElements(ret).stream().filter(e -> e instanceof DataValue).forEach(e -> vals.add((DataValue) e));
    	for (DataValue d : vals) {
    		DataValue d1 = u1Renaming.get(d);
    		DataValue d2 = u2Renaming.get(d);
    		if (d1 == null || !u1.getRegisters().contains(d1) || d2 == null || !u2.getRegisters().contains(d2)) {
    			for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(ret, d)) {
    				SuffixValue param = er.cast().getParameter();
    				ret.put(param, new TrueRestriction(param));
    			}
    		}
    	}

    	return ret;
    }
}
