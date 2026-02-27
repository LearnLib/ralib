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

	protected final ConstraintSolver solver;

	public SLLambdaRestrictionBuilder(SymbolicSuffixRestrictionBuilder restrBuilder, ConstraintSolver solver) {
		this(restrBuilder.consts, restrBuilder.teachers, solver);
	}

	public SLLambdaRestrictionBuilder(Constants consts, Map<DataType, Theory> teachers, ConstraintSolver solver) {
		super(consts, teachers);
		if (teachers == null) {
			throw new IllegalArgumentException("Non-null argument expected");
		}
		this.solver = solver;
	}

	/**
	 * Restrict suffix value by examining relation between corresponding data values in {@code suffix}
     * and values in {@code prefix} and {@code u} during counterexample analysis.
     * <br>
     * Note that restrictions computed by this method are specific to the counterexample and should
     * not be used for suffixes added to the classification tree.
     * <p>
     * This method is currently only implemented for the {@link EqualityTheory}
     *
     * @param prefix prefix of counterexample
     * @param suffix suffix of counterexample
     * @param u short prefix in classification tree corresponding to {@code prefix}
     * @param prefixValuation valuation after a run of the hypothesis over {@code prefix}
     * @param uValuation valuation after a run of the hypothesis over {@code u}
     * @return
	 */
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

	/**
	 * Construct a restricted symbolic suffix with restrictions derived by examining relations
	 * between data values in {@code suffix} and data values in {@code prefix} and {@code u}
	 * during counterexample analysis.
     * Note that restrictions computed by this method are specific to the counterexample and should
     * not be used for suffixes added to the classification tree.
     * <p>
     * This method is currently only implemented for the {@link EqualityTheory}.
     *
	 * @param prefix prefix of counterexample
	 * @param suffix suffix of counterexample
	 * @param u short prefix in classification tree corresponding to {@code prefix}
	 * @param prefixValuation valuation after a run of the hypothesis over {@code prefix}
	 * @param uValuation valuation after a run of the hypothesis over {@code u}
	 * @return symbolic suffix with restrictions respecting the relations between data values in counterexample
	 */
	public SymbolicSuffix constructRestrictedSuffix(Word<PSymbolInstance> prefix,
			Word<PSymbolInstance> suffix,
			Word<PSymbolInstance> u,
			RegisterValuation prefixValuation,
			RegisterValuation uValuation) {
		return new SymbolicSuffix(DataWords.actsOf(suffix),
				restrictSuffix(prefix, suffix, u, prefixValuation, uValuation));
	}

	/**
	 * Construct a restricted symbolic suffix with restrictions derived by examining relations
	 * between data values in {@code suffix} and data values in {@code prefix} and {@code u}
	 * during counterexample analysis.
     * Note that restrictions computed by this method are specific to the counterexample and should
     * not be used for suffixes added to the classification tree.
     * <p>
     * This method is currently only implemented for the {@link EqualityTheory}.
     *
	 * @param prefix prefix of counterexample
	 * @param suffix suffix of counterexample
	 * @param u1 short prefix in classification tree corresponding to {@code prefix}
	 * @param u2 other short prefix in same leaf as {@code u1}
	 * @param prefixValuation valuation after a run of the hypothesis over {@code prefix}
	 * @param u1Valuation valuation after a run of the hypothesis over {@code u1}
	 * @param u2Valuation valuation after a run of the hypothesis over {@code u2}
	 * @return symbolic suffix with restrictions respecting the relations between data values in counterexample
	 */
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

	/**
	 * Concretize the restrictions of {@code suffix} according to {@code valuations}. A concretized
	 * restriction is constructed for a specific prefix, and will usually be expressed as
	 * guard relations between suffix values and data values.
	 *
	 * @param suffix restricted symbolic suffix
	 * @param valuations valuations of registers and prefix parameters
	 * @return {@code suffix} with conretized restrictions
	 */
	public static SymbolicSuffix concretize(SymbolicSuffix suffix, Mapping<? extends SymbolicDataValue, DataValue> ... valuations) {
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		for (Mapping<? extends SymbolicDataValue, DataValue> m : valuations) {
			mapping.putAll(m);
		}
		return concretize(suffix, mapping);
	}

	/**
	 * Concretize the restrictions of {@code suffix} according to {@code valuations}. A concretized
	 * restriction is constructed for a specific prefix, and will usually be expressed as
	 * guard relations between suffix values and data values.
	 *
	 * @param suffix restricted symbolic suffix
	 * @param valuations valuations of registers and prefix parameters
	 * @return {@code suffix} with conretized restrictions
	 */
	public static SymbolicSuffix concretize(SymbolicSuffix suffix, Mapping<? extends SymbolicDataValue, DataValue> mapping) {
		Map<SuffixValue, AbstractSuffixValueRestriction> newRestrs = new LinkedHashMap<>();
		for (SuffixValue s : suffix.getValues()) {
			AbstractSuffixValueRestriction restr = suffix.getRestriction(s);
			AbstractSuffixValueRestriction concrRestr = restr.concretize(mapping);
			newRestrs.put(s, concrRestr);
		}
		return new SymbolicSuffix(suffix.getActions(), newRestrs);
	}

	public boolean hasUnmappedRestrictionValue(SymbolicSuffix av, Set<DataValue> mem) {
		Set<DataValue> restrVals = new LinkedHashSet<>();
		AbstractSuffixValueRestriction.getElements(av.getRestrictions()).stream().filter(e -> e instanceof DataValue).forEach(e -> restrVals.add((DataValue) e));
		for (DataValue d : restrVals) {
			if (teachers != null && teachers.get(d.getDataType()) instanceof EqualityTheory && !mem.contains(d)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Extend {@code suffix} by prepending it with the last symbol of {@code u1Extended} (hereafter
	 * known as the action). Note that the last symbol of {@code u2Extended} must have the same
	 * base symbol. The extended symbolic suffix will be restricted in such a way that the
	 * restrictions of the action respect all possible relations between its data values and
	 * data values in the prefix. Restrictions for the {@code suffix} part of the extended suffix
	 * will be restricted such that the extended suffix will be able to separate {@code u1} and
	 * {@code u2}. Any data values in the restrictions will be mapped to the representative
	 * prefix of the leaf containing {@code u1} and {@code u2}.
	 * <p>
	 * This method assumes the following:
	 * <ul>
	 *     <li>{@code u1Extended} and {@code u2Extended} are one-symbol extensions of {@code u1}
	 *     and {@code u2}, respectively, with the same base symbol</li>
	 *     <li>{@code sdt1} and {@code sdt2} were constructed from a tree query with
	 *     {@code u1Extended} and {@code u2Extended}, respectively, and {@code suffix}</li>
	 *     <li>{@code sdt1} and {@code sdt2} are not equivalent under any bijection (thereby
	 *     separating {@code u1Extended} and {@code u2Extended})</li>
	 *     <li>{@code u1} and {@code u2} are in the same leaf</li>
	 * </ul>
	 * <p>
	 * Note that restrictions are currently only implemented for the {@link EqualityTheory}.
	 *
	 * @param u1 a short prefix
	 * @param u1Extended one-symbol extension of {@code u1}
	 * @param u2 a different short prefix in the same leaf as {@code u1}
	 * @param u2Extended one-symbol extension of {@code u2}
	 * @param suffix restricted symbolic suffix separating {@code u1Extended} and {@code u2Extended}
	 * @param sdt1 SDT from a tree query with {@code u1Extended} and {@code suffix}
	 * @param sdt2 SDT from a tree query with {@code u2Extended} and {@code suffix}
	 * @return restricted symbolic suffix separating {@code u1} and {@code u2}
	 */
    public SymbolicSuffix extendSuffix(Prefix u1, Prefix u1Extended, Prefix u2, Prefix u2Extended, SymbolicSuffix suffix, SDT sdt1, SDT sdt2) {
    	ParameterizedSymbol action = u1Extended.lastSymbol().getBaseSymbol();
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	// compute restrictions for the suffix values of the action
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
    	// relabel to the representative prefix of u1 (and u2)
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u1.getRpBijection().toVarMapping());

    	// restrictions for suffix
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

    /**
     * Extend {@code suffix} by prepending it with the last symbol of {@code uIf} (hereafter
	 * known as the action). Note that the last symbol of {@code uElse} must have the same
	 * base symbol. The extended symbolic suffix will be restricted in such a way that the
	 * restrictions of the action respect all possible relations between its data values and
	 * data values in the prefix. Restrictions for the {@code suffix} part of the extended suffix
	 * will be restricted such that the extended suffix will be able to separate {@code u1} and
	 * {@code u2}. Any data values in the restrictions will be mapped to the representative
	 * prefix of the leaf containing {@code u}.
	 * <p>
	 * This method assumes the following:
	 * <ul>
	 *
	 *     <li>{@code uIf} and {@code uElse} are one-symbol extensions of {@code u}, specifically
	 *         <ul>
	 *             <li>{@code uIf} is the one-symbol extension of the "if-guard", i.e., an equality
	 *                 guard on data values in {@code u}</li>
	 *             <li>{@code uElse} is the one-symbol extension of the "else-guard", i.e., the guard
	 *                 corresponding to a fresh data value</li>
	 *         </ul>
	 *     </li>
	 *     <li>{@code sdtIf} and {@code sdtElse} were constructed from a tree query with
	 *         {@code uIf} and {@code uElse}, respectively, and {@code suffix}</li>
	 *     <li>{@code sdtIf} and {@code sdtElse} are not equivalent</li>
	 * </ul>
	 * <p>
	 * Note that restrictions are currently only implemented for the {@link EqualityTheory}.
	 *
     * @param u a short prefix
     * @param uIf one-symbol extension of {@code u} corresponding to an if-guard
     * @param uElse one-symbol extension of {@code u} corresponding to an else-guard
     * @param suffix restricted symbolic suffix separating {@code uIf} and {@code uElse}
     * @param sdtIf SDT from a tree query with {@code uIf} and {@code suffix}
     * @param sdtElse SDT from a tree query with {@code uElse} and {@code suffix}
     * @return restricted symbolic suffix, extended from {@code suffix}, which reveals the if-guard of {@code uIf}
     */
    public SymbolicSuffix extendSuffix(Prefix u, Prefix uIf, Prefix uElse, SymbolicSuffix suffix, SDT sdtIf, SDT sdtElse) {
    	PSymbolInstance symbol = uIf.lastSymbol();
    	ParameterizedSymbol action = symbol.getBaseSymbol();
    	assert uElse.lastSymbol().getBaseSymbol().equals(action) : "Extensions do not match";
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	// compute restrictions for action
    	Map<SuffixValue, AbstractSuffixValueRestriction> actionRestrictions = new LinkedHashMap<>();
    	for (DataType type : action.getPtypes()) {
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			EqualityTheory et = (EqualityTheory) theory;
    			AbstractSuffixValueRestriction rIf = et.restrictSuffixValue(s, u, symbol, u.getRegisters(), consts);
    			// must include fresh in order to allow extended suffix to reveal guard
    			AbstractSuffixValueRestriction r = DisjunctionRestriction.create(s, rIf, new FreshSuffixValue(s));
    			actionRestrictions.put(s, r);
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	// relabel to representative prefix of u
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u.getRpBijection().toVarMapping());

    	// compute restrictions for suffix part
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

    /**
     * Extend {@code suffix} by prepending it with the last symbol of {@code uExtended} (hereafter
	 * known as the action). The extended symbolic suffix will be restricted in such a way that the
	 * restrictions of the action respect all possible relations between its data values and
	 * data values in the prefix. Restrictions for the {@code suffix} part of the extended suffix
	 * will be restricted such that the extended suffix reveals all data values in {@code sdt}
	 * which are not memorable in {@code u}.
	 *
     * @param u a short prefix
     * @param uExtended a one-symbol extension of {@code u}
     * @param suffix a restricted symbolic suffix revealing data values in {@code uExtended} that are not memorable in {@code u}
     * @param sdt SDT from a tree query with {@code uExtended} and {@code suffix}
     * @return a restricted suffix, extended from {@code suffix}, which reveals unmapped data values in {@code u}
     */
    public SymbolicSuffix extendSuffix(Prefix u, Prefix uExtended, SymbolicSuffix suffix, SDT sdt) {
    	// TODO: REMOVE TEMPORARY HACK FOR REGISTER CLOSEDNESS AND FIX THE PROBLEMS INSTEAD
//    	return unrestricted(uExtended.lastSymbol().getBaseSymbol(), suffix);
    	ParameterizedSymbol action = uExtended.lastSymbol().getBaseSymbol();
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	Set<DataValue> missingRegisters = new LinkedHashSet<>(sdt.getDataValues());
    	missingRegisters.removeAll(u.getRegisters());

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	// compute restrictions for action
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
    	// relabel to representative prefix of u
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u.getRpBijection().toVarMapping());

    	// compute restrictions for the suffix part
    	DataType[] suffixTypes = DataWords.typesOf(suffixActions);
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = isEqualityTheory(suffixTypes) ?
    			EqualityTheory.restrictionFromSDT(sdt, uExtended, u.getRpBijection(), consts, suffix, solver) :
    				genericRestrictions(suffix, u, uExtended, u, uExtended);

    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	restrictions.putAll(actionRestrictions);
    	restrictions.putAll(suffixRestrictions);

    	Word<ParameterizedSymbol> actions = DataWords.concatenate(Word.fromSymbols(action), suffixActions);
    	return new SymbolicSuffix(actions, restrictions);
    }

    /**
     * @param action
     * @param suffix
     * @return unrestricted symbolic suffix constructed by prepending {@code suffix} with {@code action}
     */
    private SymbolicSuffix unrestricted(ParameterizedSymbol action, SymbolicSuffix suffix) {
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();
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
//    		restrictions.put(s, e.getValue().shift(shift));
    		restrictions.put(s, new TrueRestriction(s));
    	}

    	Word<ParameterizedSymbol> actions = DataWords.concatenate(Word.fromSymbols(action), suffix.getActions());
    	return new SymbolicSuffix(actions, restrictions);
    }

    /**
     * @param types
     * @return {@code true} if and only if all data types of {@code types} are associated with the {@link EqualityTheory}
     */
    private boolean isEqualityTheory(DataType[] types) {
    	for (DataType type : types) {
    		Theory theory = teachers.get(type);
    		if (theory == null || !(theory instanceof EqualityTheory)) {
    			return false;
    		}
    	}
    	return true;
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
