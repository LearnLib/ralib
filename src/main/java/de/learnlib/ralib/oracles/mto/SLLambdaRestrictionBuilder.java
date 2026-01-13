package de.learnlib.ralib.oracles.mto;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.DisjunctionRestriction;
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
//			if (!(restr instanceof SuffixValueRestriction)) {
//				throw new IllegalArgumentException("Incompatible restriction type");
//			}
//			SuffixValueRestriction svr = (SuffixValueRestriction) restr;
//			newRestrs.put(s, svr.concretize(valuations));
		}
		return new SymbolicSuffix(suffix.getActions(), newRestrs);
	}

    public SymbolicSuffix extendSuffix(Prefix u1, Prefix u1Extended, Prefix u2, Prefix u2Extended, Prefix reprPrefix, SymbolicSuffix suffix, SDT sdt1, SDT sdt2, boolean isLocationConsistency) {
    	ParameterizedSymbol action = u1Extended.lastSymbol().getBaseSymbol();
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	for (DataType type : action.getPtypes()) {
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			EqualityTheory et = (EqualityTheory) theory;
    			AbstractSuffixValueRestriction r = et.restrictSuffixValue(s, u1, u1Extended.lastSymbol(), u1.getRegisters());
    			if (!isLocationConsistency) {
    				r = DisjunctionRestriction.create(s, r, new FreshSuffixValue(s));
    			}
    			restrictions.put(s, r.relabel(reprPrefix.getRpBijection()));
    		} else {
    			restrictions.put(s, new TrueRestriction(s));
    		}
    	}

//    	boolean isEquality = true;
//    	for (DataType type : suffixTypes) {
//    		Theory theory = teachers.get(type);
//    		if (theory == null || !(theory instanceof EqualityTheory)) {
//    			isEquality = false;
//    			break;
//    		}
//    	}
//
//    	if (isEquality) {
//    		Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = EqualityTheory.restrictSDTs(
//    				sdt1.relabel(SDTRelabeling.fromBijection(u1.getRpBijection())),
//    				sdt2.relabel(SDTRelabeling.fromBijection(u2.getRpBijection())),
//    				suffix.getRestrictions(),
//    				solver);
//    		for (SuffixValue s : suffix.getDataValues()) {
//    			SuffixValue suffixValue = sgen.next(s.getDataType());
//    			int shift = suffixValue.getId() - s.getId();
//    			restrictions.put(suffixValue, suffixRestrictions.get(s).shift(shift));
//    		}
//    	}
    	int arity1 = u1Extended.lastSymbol().getBaseSymbol().getArity();
    	int arity2 = u2Extended.lastSymbol().getBaseSymbol().getArity();
    	SDT sdt1Relabeled = relabelActionParameters(sdt1, u1Extended.lastSymbol(), u1, -arity1);
    	SDT sdt2Relabeled = relabelActionParameters(sdt2, u2Extended.lastSymbol(), u2, -arity2);
    	restrictions.putAll(restrictSDTs(
    			sdt1Relabeled.relabel(SDTRelabeling.fromBijection(u1Extended.getRpBijection())),
    			sdt2Relabeled.relabel(SDTRelabeling.fromBijection(u2Extended.getRpBijection())),
    			suffix,
    			sgen));

    	Word<ParameterizedSymbol> actions = DataWords.concatenate(suffixActions, Word.fromSymbols(action));

    	return new SymbolicSuffix(actions, restrictions);

//    	Expression<Boolean> path = pruneSDT(sdt1.relabel(SDTRelabeling.fromBijection(u1.getRpBijection())),
//    			sdt2.relabel(SDTRelabeling.fromBijection(u2.getRpBijection())));
//
//    	Map<SuffixValue, AbstractSuffixValueRestriction> pathRestrictions = pathRestrictions(path);
//    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : pathRestrictions.entrySet()) {
//    		SuffixValue s = sgen.next(e.getKey().getDataType());
//    		int shift = e.getKey().getId() - s.getId();
//    		AbstractSuffixValueRestriction r = e.getValue().shift(shift);
//    		restrictions.put(s, r);
//    	}
//
//    	Word<ParameterizedSymbol> actions = suffix.getActions().append(action);
//    	return new SymbolicSuffix(actions, restrictions);
    }

    public SymbolicSuffix extendSuffix(Prefix u, Prefix uExtended, Prefix reprPrefix, SymbolicSuffix suffix, SDT sdt, Set<DataValue> missingRegisters) {
    	ParameterizedSymbol action = uExtended.lastSymbol().getBaseSymbol();
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	int lastId = 0;
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	for (DataType type : action.getPtypes()) {
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			EqualityTheory et = (EqualityTheory) theory;
    			AbstractSuffixValueRestriction restr = et.restrictSuffixValue(s, u, uExtended.lastSymbol(), u.getRegisters(), missingRegisters);
    			restrictions.put(s, restr);
    		} else {
    			restrictions.put(s, new TrueRestriction(s));
    		}
    		lastId = s.getId();
    	}

    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	if (!isEqualityTheory(suffixTypes)) {
    		restrictions.putAll(AbstractSuffixValueRestriction.shift(suffix.getRestrictions(), lastId));
    	} else {
    		restrictions.putAll(restrictSuffixValues(
    			sdt.relabel(SDTRelabeling.fromBijection(uExtended.getRpBijection())),
    			lastId + 1,
    			missingRegisters,
    			suffix.getRestrictions()));
    	}

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

    private Map<SuffixValue, AbstractSuffixValueRestriction> restrictSDTs(SDT sdt1, SDT sdt2, SymbolicSuffix suffix, SuffixValueGenerator sgen) {
    	if (isEqualityTheory(DataWords.typesOf(suffix.getActions()))) {
    		Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = EqualityTheory.restrictSDTs(sdt1, sdt2, suffix.getRestrictions(), solver);
    		Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    		for (SuffixValue s : suffix.getDataValues()) {
    			SuffixValue suffixValue = sgen.next(s.getDataType());
    			int shift = suffixValue.getId() - s.getId();
    			ret.put(suffixValue, suffixRestrictions.get(s).shift(shift));
    		}
    		return ret;
    	}

    	return suffix.getRestrictions();
    }
//
//    private Map<SuffixValue, AbstractSuffixValueRestriction> restrictSuffixValues(SymbolicSuffix suffix, SDT sdt, SuffixValueGenerator sgen, Set<DataValue> missingRegs) {
//    	if (isEqualityTheory(DataWords.typesOf(suffix.getActions()))) {
//    		Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = EqualityTheory.restrictSuffixValues(sdt, suffix.getRestrictions(), solver);
//    		Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    	}
//    }
//
    private Map<SuffixValue, AbstractSuffixValueRestriction> restrictSuffixValues(SDT sdt, int nextId, Set<DataValue> missingRegisters, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
    	// assume equality theory if we have reached this point
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = EqualityTheory.restrictSDT(sdt, missingRegisters, restrictions, solver);
    	ret = AbstractSuffixValueRestriction.shift(ret, nextId - 1);
    	return ret;
    }

    private SDT relabelActionParameters(SDT sdt, PSymbolInstance action, Word<PSymbolInstance> u, int offset) {
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
    	DataValue[] actionVals = action.getParameterValues();

    	SDTRelabeling relabeling = new SDTRelabeling();
    	int suffixIndex = 1 + offset;
    	for (DataValue d : actionVals) {
    		if (!uVals.contains(d)) {
    			SuffixValue s = new SuffixValue(d.getDataType(), suffixIndex);
    			relabeling.put(d, s);
    			suffixIndex++;
    		}
    	}
    	return sdt.relabel(relabeling);
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
}
