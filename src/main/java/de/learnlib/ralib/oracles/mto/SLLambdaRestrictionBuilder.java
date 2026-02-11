package de.learnlib.ralib.oracles.mto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.data.Bijection;
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
import de.learnlib.ralib.theory.ElementRestriction;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TrueRestriction;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.theory.equality.UnmappedEqualityRestriction;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
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
//    			restrictions.put(s, r.relabel(reprPrefix.getRpBijection().toVarMapping()));
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	actionRestrictions = this.relabelToRP(actionRestrictions, u1);

    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = isEqualityTheory(suffixTypes) ?
    			EqualityTheory.restrictionFromSDTs(sdt1, sdt2, u1Extended, u2Extended, u1.getRpBijection(), u2.getRpBijection(), consts, suffix, solver) :
    				genericRestrictions(suffix, u1, u1Extended, u2, u2Extended);

//    	Set<DataValue> usedVals = this.collectUsedDataValues(u1, u1Extended.lastSymbol(), u2Extended.lastSymbol(), sdt1, sdt2, suffix.getRestrictions());
//    	Bijection<DataValue> unmappedRenaming1 = unmappedRelabeling(sdt1, u1.getRegisters(), suffix.getRestrictions(), usedVals);
//    	Bijection<DataValue> unmappedRenaming2 = unmappedRelabeling(sdt2, u2.getRegisters(), suffix.getRestrictions(), usedVals);
//    	SDT sdt1Relabeled = relabelSdt(sdt1, u1.getRpBijection(), unmappedRenaming1);
//    	SDT sdt2Relabeled = relabelSdt(sdt2, u2.getRpBijection(), unmappedRenaming2);
//    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictionsSeparatingSdts(sdt1Relabeled, sdt2Relabeled, suffix);
//    	suffixRestrictions = AbstractSuffixValueRestriction.shift(suffixRestrictions, action.getArity());
//    	Set<DataValue> handledActionValues = new LinkedHashSet<>();
//    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, u1Extended.lastSymbol(), u1.getRpBijection(), unmappedRenaming1, handledActionValues);
//    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, u2Extended.lastSymbol(), u2.getRpBijection(), unmappedRenaming2, handledActionValues);
//    	suffixRestrictions = replaceUnmapped(suffixRestrictions, unmappedRenaming1);
//    	suffixRestrictions = replaceUnmapped(suffixRestrictions, unmappedRenaming2);
//
////    	SDT sdt1Relabeled = relabelFromExtension(sdt1, u1Extended, suffix);
////    	SDT sdt2Relabeled = relabelFromExtension(sdt2, u2Extended, suffix);
////    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictionsSeparatingSdts(sdt1Relabeled, sdt2Relabeled, suffix);
////    	suffixRestrictions = relabelToExtension(suffixRestrictions, u1Extended, suffix);
////    	suffixRestrictions = shift(suffixRestrictions, action);
////    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, u1Extended.lastSymbol(), new LinkedHashSet<>());
////    	suffixRestrictions = replaceUnmapped(suffixRestrictions, u1.getRegisters(), u1Extended.lastSymbol());
//
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	restrictions.putAll(actionRestrictions);
    	restrictions.putAll(suffixRestrictions);
//    	restrictions = relabelToRP(restrictions, u1);

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
//    			restrictions.put(s, r.relabel(uRepr.getRpBijection().toVarMapping()));
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	actionRestrictions = this.relabelToRP(actionRestrictions, u);

    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = isEqualityTheory(suffixTypes) ?
    			EqualityTheory.restrictionFromSDTs(sdtIf, sdtElse, uIf, uElse, u.getRpBijection(), u.getRpBijection(), consts, suffix, solver) :
    				genericRestrictions(suffix, u, uIf, u, uElse);

//    	Set<DataValue> usedVals = this.collectUsedDataValues(u, uIf.lastSymbol(), uElse.lastSymbol(), sdtIf, sdtElse, suffix.getRestrictions());
//    	Bijection<DataValue> unmappedRenamingIf = unmappedRelabeling(sdtIf, u.getRegisters(), suffix.getRestrictions(), usedVals);
//    	Bijection<DataValue> unmappedRenamingElse = unmappedRelabeling(sdtElse, u.getRegisters(), suffix.getRestrictions(), usedVals);
//    	SDT sdtIfRelabeled = relabelSdt(sdtIf, u.getRpBijection(), unmappedRenamingIf);
//    	SDT sdtElseRelabeled = relabelSdt(sdtElse, u.getRpBijection(), unmappedRenamingElse);
//    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictionsSeparatingSdts(sdtIfRelabeled, sdtElseRelabeled, suffix);
//    	suffixRestrictions = AbstractSuffixValueRestriction.shift(suffixRestrictions, action.getArity());
//    	Set<DataValue> handledActionValues = new LinkedHashSet<>();
//    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, uIf.lastSymbol(), u.getRpBijection(), unmappedRenamingIf, handledActionValues);
//    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, uElse.lastSymbol(), u.getRpBijection(), unmappedRenamingElse, handledActionValues);
//    	suffixRestrictions = replaceUnmapped(suffixRestrictions, unmappedRenamingIf);
//    	suffixRestrictions = replaceUnmapped(suffixRestrictions, unmappedRenamingElse);
//
////    	SDT sdtIfRelabeled = relabelFromExtension(sdtIf, uIf, suffix);
////    	SDT sdtElseRelabeled = relabelFromExtension(sdtElse, uElse, suffix);
////    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictionsSeparatingSdts(sdtIfRelabeled, sdtElseRelabeled, suffix);
////    	suffixRestrictions = relabelToExtension(suffixRestrictions, uIf, suffix);
////    	suffixRestrictions = shift(suffixRestrictions, action);
////    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, uIf.lastSymbol(), new LinkedHashSet<>());
////    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, uElse.lastSymbol(), Set.of(uIf.lastSymbol().getParameterValues()));
////    	suffixRestrictions = replaceUnmapped(suffixRestrictions, u.getRegisters(), uIf.lastSymbol());
////    	suffixRestrictions = replaceUnmapped(suffixRestrictions, u.getRegisters(), uElse.lastSymbol());
//
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	restrictions.putAll(actionRestrictions);
    	restrictions.putAll(suffixRestrictions);
//    	restrictions = relabelToRP(restrictions, u);

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

//    	int lastId = 0;
    	Map<SuffixValue, AbstractSuffixValueRestriction> actionRestrictions = new LinkedHashMap<>();
    	for (DataType type : action.getPtypes()) {
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			EqualityTheory et = (EqualityTheory) theory;
    			AbstractSuffixValueRestriction restr = et.restrictSuffixValue(s, u, uExtended.lastSymbol(), u.getRegisters(), missingRegisters, consts);
    			actionRestrictions.put(s, restr);
//    			restrictions.put(s, restr.relabel(u.getRpBijection().toVarMapping()));
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
//    		lastId = s.getId();
    	}
    	actionRestrictions = relabelToRP(actionRestrictions, u);

    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = isEqualityTheory(suffixTypes) ?
    			EqualityTheory.restrictionFromSDT(sdt, uExtended, u.getRpBijection(), consts, missingRegisters, suffix, solver) :
    				genericRestrictions(suffix, u, uExtended, u, uExtended);

//    	Set<DataValue> usedVals = this.collectUsedDataValues(u, uExtended.lastSymbol(), uExtended.lastSymbol(), sdt, sdt, suffix.getRestrictions());
//    	Bijection<DataValue> unmappedRenaming = this.unmappedRelabeling(sdt, u.getRegisters(), suffix.getRestrictions(), usedVals);
//    	SDT sdtRelabeled = this.relabelSdt(sdt, u.getRpBijection(), unmappedRenaming);
//    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictSuffixValues(sdtRelabeled, missingRegisters, suffix);
//    	suffixRestrictions = AbstractSuffixValueRestriction.shift(suffixRestrictions, action.getArity());
//    	suffixRestrictions = this.addActionSuffixValue(suffixRestrictions, uExtended.lastSymbol(), u.getRpBijection(), unmappedRenaming, new LinkedHashSet<>());
//    	suffixRestrictions = this.replaceUnmapped(suffixRestrictions, unmappedRenaming);
//
////    	SDT sdtRelabeled = relabelFromExtension(sdt, uExtended, suffix);
////    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictSuffixValues(sdtRelabeled, missingRegisters, suffix);
////    	suffixRestrictions = relabelToExtension(suffixRestrictions, uExtended, suffix);
////    	suffixRestrictions = shift(suffixRestrictions, action);
////    	suffixRestrictions = addActionSuffixValue(suffixRestrictions, uExtended.lastSymbol(), new LinkedHashSet<>());
////    	suffixRestrictions = replaceUnmapped(suffixRestrictions, u.getRegisters(), uExtended.lastSymbol());
//
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	restrictions.putAll(actionRestrictions);
    	restrictions.putAll(suffixRestrictions);
//    	restrictions = relabelToRP(restrictions, u);

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

    private Set<DataValue> collectUsedDataValues(Prefix u, PSymbolInstance psi1, PSymbolInstance psi2, SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
    	Set<DataValue> ret = new LinkedHashSet<>();
    	ret.addAll(Set.of(psi1.getParameterValues()));
    	ret.addAll(Set.of(psi2.getParameterValues()));
    	ret.addAll(u.getRpBijection().values());
    	ret.addAll(sdt1.getDataValues());
    	ret.addAll(sdt2.getDataValues());
    	AbstractSuffixValueRestriction.getElements(restrictions).stream().filter(e -> e instanceof DataValue).forEach(e -> ret.add((DataValue) e));
    	return ret;
    }

    private Bijection<DataValue> unmappedRelabeling(SDT uSdt, Set<DataValue> memorable, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Set<DataValue> usedVals) {
    	Bijection<DataValue> ret = new Bijection<>();
    	for (DataValue d : uSdt.getDataValues()) {
    		if (!memorable.contains(d)) {
    			List<DataValue> valList = new ArrayList<>(usedVals);
    			valList.removeIf(v -> !v.getDataType().equals(d.getDataType()));
    			Theory theory = teachers.get(d.getDataType());
    			assert theory != null;
    			DataValue fresh = theory.getFreshValue(valList);
    			ret.put(d, fresh);
    			usedVals.add(fresh);
    		}
    	}
    	for (Expression<BigDecimal> e : AbstractSuffixValueRestriction.getElements(restrictions)) {
    		if (e instanceof DataValue d && !ret.containsKey(d)) {
    			List<DataValue> valList = new ArrayList<>(usedVals);
    			valList.removeIf(v -> !v.getDataType().equals(d.getDataType()));
    			Theory theory = teachers.get(d.getDataType());
    			assert theory != null;
    			DataValue fresh = theory.getFreshValue(valList);
    			ret.put(d, fresh);
    			usedVals.add(fresh);
    		}
    	}
    	return ret;
    }

    private SDT relabelSdt(SDT sdt, Bijection<DataValue> rpRenaming, Bijection<DataValue> unmappedRenaming) {
    	SDT unmapped = sdt.relabel(SDTRelabeling.fromBijection(unmappedRenaming));
    	return unmapped.relabel(SDTRelabeling.fromBijection(rpRenaming));
    }

//    private SDT relabelFromExtension(SDT sdt, Prefix uExt, SymbolicSuffix suffix) {
//    	Bijection<DataValue> renaming = uExt.getBijection(uExt.getPath().getPrior(suffix));
//    	return sdt.relabel(SDTRelabeling.fromBijection(renaming));
//    }

    private Map<SuffixValue, AbstractSuffixValueRestriction> restrictionsSeparatingSdts(SDT sdt1, SDT sdt2, SymbolicSuffix suffix) {
    	if (isEqualityTheory(DataWords.typesOf(suffix.getActions()))) {
    		return EqualityTheory.restrictSDTs(sdt1, sdt2, suffix.getRestrictions(), solver);
    	}
    	return suffix.getRestrictions();
    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> relabelToExtension(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Prefix uExt, SymbolicSuffix suffix) {
//    	Bijection<DataValue> renaming = uExt.getBijection(uExt.getPath().getPrior(suffix)).inverse();
//    	return AbstractSuffixValueRestriction.relabel(restrictions, renaming.toVarMapping());
//    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> shift(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ParameterizedSymbol action) {
//    	return AbstractSuffixValueRestriction.shift(restrictions, action.getArity());
//    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> relabelActionParameters(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Prefix u, PSymbolInstance action) {
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;
//    	Set<ElementRestriction> alreadyHandled = new LinkedHashSet<>();
//    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
//    	DataValue[] actionVals = action.getParameterValues();
//
//    	for (int i = 0; i < actionVals.length; i++) {
//    		DataValue d = actionVals[i];
//    		SuffixValue dParam = new SuffixValue(d.getDataType(), i + 1);
//    		for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, d)) {
//    			if (alreadyHandled.contains(er)) {
//    				continue;
//    			}
//    			AbstractSuffixValueRestriction replace = er.cast();
//    			AbstractSuffixValueRestriction byParam = er.replaceElement(d, dParam);
//    			if (!uVals.contains(d)) {
//    				ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, byParam);
//    			} else {
//    				AbstractSuffixValueRestriction byReg = u.getRegisters().contains(d) ?
//    						replace :
//    							new UnmappedEqualityRestriction(replace.getParameter());
//    				AbstractSuffixValueRestriction by = DisjunctionRestriction.create(replace.getParameter(), byParam, byReg);
//    				ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
//    			}
//    			alreadyHandled.add(er);
//    		}
//    	}
//
//    	return ret;
//    }

    private Map<SuffixValue, AbstractSuffixValueRestriction> addActionSuffixValue(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, PSymbolInstance action, Bijection<DataValue> rpRenaming, Bijection<DataValue> unmappedRenaming, Set<DataValue> ignore) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;
    	DataValue[] actionVals = action.getParameterValues();

    	for (int i = 0; i < actionVals.length; i++) {
    		DataValue d = actionVals[i];
    		DataValue dMapped = rpRenaming.get(d);
    		DataValue dUnmapped = unmappedRenaming.get(d);
    		SuffixValue dParam = new SuffixValue(d.getDataType(), i + 1);

    		if (dMapped != null &&
    				!ignore.contains(dMapped) &&
    				AbstractSuffixValueRestriction.containsElement(restrictions, dMapped)) {
    			ret = addActionSuffixValue(ret, dMapped, dParam);
    			ignore.add(dMapped);
    		}
    		if (dUnmapped != null &&
    				!ignore.contains(dUnmapped) &&
    				AbstractSuffixValueRestriction.containsElement(restrictions, dUnmapped)) {
    			ret = addActionSuffixValue(ret, dUnmapped, dParam);
    			ignore.add(dUnmapped);
    		}
    	}

    	return ret;
    }

    private Map<SuffixValue, AbstractSuffixValueRestriction> replaceUnmapped(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Bijection<DataValue> unmappedRenaming) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;
    	Set<ElementRestriction> alreadyHandled = new LinkedHashSet<>();
    	Set<DataValue> restrVals = new LinkedHashSet<>();
    	AbstractSuffixValueRestriction.getElements(restrictions).stream().filter(e -> e instanceof DataValue).forEach(e -> restrVals.add((DataValue) e));

//    	for (DataValue d : action.getParameterValues()) {
    	for (DataValue d : restrVals) {
    		DataValue dUnmapped = unmappedRenaming.get(d);
    		if (dUnmapped != null && AbstractSuffixValueRestriction.containsElement(restrictions, dUnmapped)) {
    			for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, dUnmapped)) {
    				if (alreadyHandled.contains(er)) {
    					continue;
    				}
    				AbstractSuffixValueRestriction replace = er.cast();
    				AbstractSuffixValueRestriction by = new UnmappedEqualityRestriction(replace.getParameter());
    				ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
    				alreadyHandled.add(er);
    			}
    		}
    	}

    	return ret;
    }

    private Map<SuffixValue, AbstractSuffixValueRestriction> addActionSuffixValue(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, DataValue d, SuffixValue s) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;
    	Set<ElementRestriction> alreadyHandled = new LinkedHashSet<>();
		for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, d)) {
			if (alreadyHandled.contains(er)) {
				continue;
			}
			AbstractSuffixValueRestriction replace = er.cast();
			AbstractSuffixValueRestriction byParam = er.replaceElement(d, s);
			AbstractSuffixValueRestriction by = DisjunctionRestriction.create(replace.getParameter(), replace, byParam);
			ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
		}
    	return ret;
    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> addActionSuffixValue(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, PSymbolInstance action, Set<DataValue> done) {
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;
//    	Set<ElementRestriction> alreadyHandled = new LinkedHashSet<>();
//    	DataValue[] actionVals = action.getParameterValues();
//
//    	for (int i = 0; i < actionVals.length; i++) {
//    		if (done.contains(actionVals[i])) {
//    			continue;
//    		}
//    		DataValue d = actionVals[i];
//    		SuffixValue dParam = new SuffixValue(d.getDataType(), i + 1);
//    		for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, d)) {
//    			if (alreadyHandled.contains(er)) {
//    				continue;
//    			}
//    			AbstractSuffixValueRestriction replace = er.cast();
//    			AbstractSuffixValueRestriction byParam = er.replaceElement(d, dParam);
//    			AbstractSuffixValueRestriction by = DisjunctionRestriction.create(replace.getParameter(), replace, byParam);
//    			ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
//    			alreadyHandled.add(er);
//    		}
//    	}
//
//    	return ret;
//    }
//
//    private Map<SuffixValue, AbstractSuffixValueRestriction> replaceUnmapped(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Set<DataValue> memorable, PSymbolInstance action) {
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;
//    	Set<ElementRestriction> alreadyHandled = new LinkedHashSet<>();
//
//    	for (DataValue d : action.getParameterValues()) {
//    		if (!memorable.contains(d)) {
//    			for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, d)) {
//    				if (alreadyHandled.contains(er)) {
//    					continue;
//    				}
//    				AbstractSuffixValueRestriction replace = er.cast();
//    				AbstractSuffixValueRestriction by = new UnmappedEqualityRestriction(replace.getParameter());
//    				ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
//    				alreadyHandled.add(er);
//    			}
//    		}
//    	}
//
//    	return ret;
//    }

    private Map<SuffixValue, AbstractSuffixValueRestriction> relabelToRP(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Prefix u) {
    	Bijection<DataValue> renaming = u.getRpBijection();
    	return AbstractSuffixValueRestriction.relabel(restrictions, renaming.toVarMapping());
    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> restrictSDTs(SDT sdt1, SDT sdt2, SymbolicSuffix suffix, SuffixValueGenerator sgen) {
//    	if (isEqualityTheory(DataWords.typesOf(suffix.getActions()))) {
//    		Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = EqualityTheory.restrictSDTs(sdt1, sdt2, suffix.getRestrictions(), solver);
//    		Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    		for (SuffixValue s : suffix.getDataValues()) {
//    			SuffixValue suffixValue = sgen.next(s.getDataType());
//    			int shift = suffixValue.getId() - s.getId();
//    			ret.put(suffixValue, suffixRestrictions.get(s).shift(shift));
//    		}
//    		return ret;
//    	}
//
//    	return suffix.getRestrictions();
//    }
//
//    private Map<SuffixValue, AbstractSuffixValueRestriction> restrictSuffixValues(SymbolicSuffix suffix, SDT sdt, SuffixValueGenerator sgen, Set<DataValue> missingRegs) {
//    	if (isEqualityTheory(DataWords.typesOf(suffix.getActions()))) {
//    		Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = EqualityTheory.restrictSuffixValues(sdt, suffix.getRestrictions(), solver);
//    		Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    	}
//    }
//
    private Map<SuffixValue, AbstractSuffixValueRestriction> restrictSuffixValues(SDT sdt, Set<DataValue> missingRegisters, SymbolicSuffix suffix) {
    	if (!isEqualityTheory(DataWords.typesOf(suffix.getActions()))) {
    		return suffix.getRestrictions();
    	}
    	return EqualityTheory.restrictSDT(sdt, missingRegisters, suffix.getRestrictions(), solver);
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = EqualityTheory.restrictSDT(sdt, missingRegisters, restrictions, solver);
//    	ret = AbstractSuffixValueRestriction.shift(ret, nextId - 1);
//    	return ret;
    }

//    private SDT relabelActionParameters(SDT sdt, PSymbolInstance action, Word<PSymbolInstance> u, int offset) {
//    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
//    	DataValue[] actionVals = action.getParameterValues();
//
//    	SDTRelabeling relabeling = new SDTRelabeling();
//    	int suffixIndex = 1 + offset;
//    	for (DataValue d : actionVals) {
//    		if (!uVals.contains(d)) {
//    			SuffixValue s = new SuffixValue(d.getDataType(), suffixIndex);
//    			relabeling.put(d, s);
//    			suffixIndex++;
//    		}
//    	}
//    	return sdt.relabel(relabeling);
//    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> relabelActionParameters(Prefix u1, PSymbolInstance symbol1, Prefix u2, PSymbolInstance symbol2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
//    	Map<SuffixValue, AbstractSuffixValueRestriction> renamed = AbstractSuffixValueRestriction.relabel(restrictions, u1.getRpBijection().inverse().toVarMapping());
//    	Set<ElementRestriction> alreadyHandled = new LinkedHashSet<>();
//    	Map<SuffixValue, AbstractSuffixValueRestriction> replaced = relabelActionParameters(u1, symbol1, renamed, alreadyHandled);
//
//    	Mapping<DataValue, DataValue> u1Tou2Renaming = u1.getRpBijection().compose(u2.getRpBijection().inverse()).toVarMapping();
//    	replaced = AbstractSuffixValueRestriction.relabel(replaced, u1Tou2Renaming);
//    	Set<ElementRestriction> alreadyHandledRenamed = new LinkedHashSet<>();
//    	for (ElementRestriction er : alreadyHandled) {
//    		ElementRestriction erRenamed = er;
//    		for (Map.Entry<DataValue, DataValue> mapping : u1Tou2Renaming.entrySet()) {
//    			AbstractSuffixValueRestriction erRenamedUncast = erRenamed.replaceElement(mapping.getKey(), mapping.getValue());
//    			assert erRenamedUncast instanceof ElementRestriction;
//    			erRenamed = (ElementRestriction) erRenamedUncast;
//    		}
//    		alreadyHandledRenamed.add(erRenamed);
//    	}
//
//    	replaced = relabelActionParameters(u2, symbol2, replaced, alreadyHandledRenamed);
//
//    	return AbstractSuffixValueRestriction.relabel(replaced, u2.getRpBijection().toVarMapping());
//    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> relabelActionParameters(Prefix u, PSymbolInstance symbol, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Set<ElementRestriction> alreadyHandled) {
//    	Map<SuffixValue, AbstractSuffixValueRestriction> replacedRestrictions = restrictions;
//
//    	DataValue[] symbolVals = symbol.getParameterValues();
//    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
//
//    	for (int i = 0; i < symbolVals.length; i++) {
//    		SuffixValue suffixValue = new SuffixValue(symbolVals[i].getDataType(), i + 1);
//    		List<ElementRestriction> erList = AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, symbolVals[i]);
//    		for (ElementRestriction replace : erList) {
//    			SuffixValue s = replace.cast().getParameter();
//    			if (alreadyHandled.contains(replace)) {
//    				continue;
//    			}
//    			AbstractSuffixValueRestriction bySuffixValue = replace.replaceElement(symbolVals[i], suffixValue);
//    			AbstractSuffixValueRestriction by = null;
//    			if (uVals.contains(symbolVals[i])) {
//    				// exists also in prefix
//    				AbstractSuffixValueRestriction byReg = u.getRegisters().contains(symbolVals[i]) ?
//    						replace.cast() :
//    							new UnmappedEqualityRestriction(s);
//    				by = DisjunctionRestriction.create(s, bySuffixValue, byReg);
//    			} else {
//    				by = bySuffixValue;
//    			}
//    			replacedRestrictions = AbstractSuffixValueRestriction.replaceRestriction(replacedRestrictions, replace.cast(), by);
//    			alreadyHandled.add(replace);
//    		}
//    	}
//
//    	return replacedRestrictions;
//    }

//    private Map<SuffixValue, AbstractSuffixValueRestriction> relabelActionParameters(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, PSymbolInstance action, Word<PSymbolInstance> u) {
//    	Map<SuffixValue, AbstractSuffixValueRestriction> renamed = new LinkedHashMap<>();
//    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
//    	DataValue[] actionVals = action.getParameterValues();
//    	Mapping<DataValue, SuffixValue> renaming = new Mapping<>();
//    	for (int i = 0; i < actionVals.length; i++) {
//    		if (!uVals.contains(actionVals[i])) {
//    			SuffixValue s = new SuffixValue(actionVals[i].getDataType(), i - actionVals.length + 1);
//    			renaming.put(actionVals[i], s);
//    		}
//    	}
//    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : restrictions.entrySet()) {
//    		AbstractSuffixValueRestriction r = e.getValue();
//    		renamed.put(e.getKey(), r.relabel(renaming));
//    	}
//    	return renamed;
//    }

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
