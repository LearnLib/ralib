package de.learnlib.ralib.oracles.mto;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.DisjunctionRestriction;
import de.learnlib.ralib.theory.ElementRestriction;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.TrueRestriction;
import de.learnlib.ralib.theory.equality.EqualityRestriction;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.theory.equality.UnmappedEqualityRestriction;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class SLLambdaEqRestrictionBuilder extends SymbolicSuffixRestrictionBuilder {

	private boolean useImprovedRegClosedOpt = false;

	protected final ConstraintSolver solver;

	public SLLambdaEqRestrictionBuilder(SymbolicSuffixRestrictionBuilder restrBuilder, ConstraintSolver solver) {
		this(restrBuilder.consts, restrBuilder.teachers, solver);
	}

	public SLLambdaEqRestrictionBuilder(Constants consts, Map<DataType, Theory> teachers, ConstraintSolver solver) {
		super(consts, teachers);
		if (teachers == null) {
			throw new IllegalArgumentException("Non-null argument expected");
		}
		this.solver = solver;
	}

	public SLLambdaEqRestrictionBuilder(Constants consts, Map<DataType, Theory> teachers, ConstraintSolver solver, boolean useImprovedRegClosedOpt) {
		this(consts, teachers, solver);
		this.useImprovedRegClosedOpt = useImprovedRegClosedOpt;
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
	@SafeVarargs
	public static SymbolicSuffix concretize(SymbolicSuffix suffix, Mapping<? extends SymbolicDataValue, DataValue> ... valuations) {
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		for (Mapping<? extends SymbolicDataValue, DataValue> m : valuations) {
			mapping.putAll(m);
		}
		return concretize(suffix, mapping);
	}

	/**
	 * Concretize the restrictions of {@code suffix} according to {@code mapping}. A concretized
	 * restriction is constructed for a specific prefix, and will usually be expressed as
	 * guard relations between suffix values and data values.
	 *
	 * @param suffix restricted symbolic suffix
	 * @param mapping mapping of registers and prefix parameters
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

	/**
	 * Checks whether {@code av} has a restriction on an unmapped data value
	 *
	 * @param av
	 * @param mem
	 * @return {@code true} if and only if the restrictions of {@code av} contain any data values not in {@code mem}
	 */
	public boolean hasUnmappedRestrictionValue(SymbolicSuffix av, Set<DataValue> mem) {
		Set<DataValue> restrVals = getDataValueElements(av.getRestrictions());
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

    	if (!isEqualityTheory(DataWords.typesOf(suffixActions))) {
    		throw new IllegalArgumentException("Only supported for equality theory");
    	}

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
    			AbstractSuffixValueRestriction r = restrictSuffixValue(s, u1, u1Extended.lastSymbol(), u1.getRegisters(), consts);
    			actionRestrictions.put(s, r);
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	// relabel to the representative prefix of u1 (and u2)
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u1.getRpBijection().toVarMapping());

    	// restrictions for suffix
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictionFromSDTs(sdt1, sdt2,
    			u1Extended, u2Extended,
    			u1.getRpBijection(), u2.getRpBijection(),
    			false, consts, suffix, solver);
    	suffixRestrictions = AbstractSuffixValueRestriction.relabel(suffixRestrictions, u1.getRpBijection().toVarMapping());

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
    public SymbolicSuffix extendSuffix(Prefix u, Prefix uIf, Prefix uElse, SymbolicSuffix suffix, SDT sdtIf, SDT sdtElse, boolean sameLeaf) {
    	PSymbolInstance symbol = uIf.lastSymbol();
    	ParameterizedSymbol action = symbol.getBaseSymbol();
    	assert uElse.lastSymbol().getBaseSymbol().equals(action) : "Extensions do not match";
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();

    	if (!isEqualityTheory(DataWords.typesOf(suffixActions))) {
    		throw new IllegalArgumentException("Only supported for equality theory");
    	}

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
    			AbstractSuffixValueRestriction rIf = restrictSuffixValue(s, u, symbol, u.getRegisters(), consts);
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
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictionFromSDTs(sdtIf, sdtElse,
    			uIf, uElse,
    			u.getRpBijection(), u.getRpBijection(),
    			sameLeaf, consts, suffix, solver);
    	suffixRestrictions = AbstractSuffixValueRestriction.relabel(suffixRestrictions, u.getRpBijection().toVarMapping());

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
    	ParameterizedSymbol action = uExtended.lastSymbol().getBaseSymbol();
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));

    	if (!isEqualityTheory(DataWords.typesOf(suffixActions))) {
    		throw new IllegalArgumentException("Only supported for equality theory");
    	}

    	if (teachers == null) {
    		return unrestricted(action, suffix);
    	}

    	Set<DataValue> missingRegisters = new LinkedHashSet<>(sdt.getDataValues());
    	missingRegisters.removeAll(u.getRegisters());

    	SuffixValueGenerator sgen = new SuffixValueGenerator();

    	// compute restrictions for action
    	Map<SuffixValue, AbstractSuffixValueRestriction> actionRestrictions = new LinkedHashMap<>();
    	for (DataValue d : uExtended.lastSymbol().getParameterValues()) {
    		DataType type = d.getDataType();
    		SuffixValue s = sgen.next(type);
    		Theory theory = teachers.get(type);
    		if (theory instanceof EqualityTheory) {
    			AbstractSuffixValueRestriction restr = uVals.contains(d) ?
    					(u.getRegisters().contains(d) ? new EqualityRestriction(s, Set.of(d)) :
    						DisjunctionRestriction.create(s, new UnmappedEqualityRestriction(s), new FreshSuffixValue(s))) :
    							new FreshSuffixValue(s);
    			actionRestrictions.put(s, restr);
    		} else {
    			actionRestrictions.put(s, new TrueRestriction(s));
    		}
    	}
    	// relabel to representative prefix of u
    	actionRestrictions = AbstractSuffixValueRestriction.relabel(actionRestrictions, u.getRpBijection().toVarMapping());

    	// compute restrictions for the suffix part
    	Map<SuffixValue, AbstractSuffixValueRestriction> suffixRestrictions = restrictionFromSDT(sdt, u, uExtended, u.getRpBijection(), consts, suffix, solver, useImprovedRegClosedOpt);
    	suffixRestrictions = AbstractSuffixValueRestriction.relabel(suffixRestrictions, u.getRpBijection().toVarMapping());

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
    	DataType[] actionTypes = action.getPtypes();

    	SuffixValueGenerator sgen = new SuffixValueGenerator();
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();

    	for (int i = 0; i < actionTypes.length; i++) {
    		SuffixValue s = sgen.next(actionTypes[i]);
    		restrictions.put(s, new TrueRestriction(s));
    	}

    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : suffix.getRestrictions().entrySet()) {
    		SuffixValue s = sgen.next(e.getKey().getDataType());
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

    public void setUseImprovedRegClosedOpt(boolean useImprovedRegClosedOpt) {
    	this.useImprovedRegClosedOpt = useImprovedRegClosedOpt;
    }


    /**
     * Compute restriction on {@code suffixValue} by examining the relationship between its
     * corresponding data value in {@code action} and data values in {@code u}.
     *
     * @param suffixValue
     * @param u
     * @param action
     * @param memorable
     * @param consts
     * @return
     */
    private AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> u, PSymbolInstance action, Set<DataValue> memorable, Constants consts) {
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
    	DataValue[] actionVals = action.getParameterValues();
    	int index = suffixValue.getId() - 1;

    	if (consts.containsValue(actionVals[index])) {
    		// we never have accidental equality with constants, so restriction can be equality with constant
    		return SuffixValueRestriction.equalityRestriction(suffixValue, consts.getAllKeysForValue(actionVals[index]));
    	}

    	Set<SuffixValue> prior = new LinkedHashSet<>();
    	for (int i = 0; i < index; i++) {
    		if (actionVals[index].equals(actionVals[i])) {
    			SuffixValue s = new SuffixValue(actionVals[i].getDataType(), i + 1);
    			prior.add(s);
    		}
    	}

    	AbstractSuffixValueRestriction unmappedWithFreshRestr = DisjunctionRestriction.create(suffixValue, new UnmappedEqualityRestriction(suffixValue), new FreshSuffixValue(suffixValue));

    	// determine type of equality with data values in u
    	AbstractSuffixValueRestriction eq = uVals.contains(actionVals[index]) ?
    			((memorable.contains(actionVals[index])) ?
    					SuffixValueRestriction.equalityRestriction(suffixValue, actionVals[index]) :
    						unmappedWithFreshRestr) :
    							null;

    	if (prior.isEmpty()) {
    		return eq == null ? new FreshSuffixValue(suffixValue) : eq;
    	}

    	AbstractSuffixValueRestriction eqPrior = SuffixValueRestriction.equalityRestriction(suffixValue, prior);

    	if (eq == null) {
    		return eqPrior;
    	}

    	return DisjunctionRestriction.create(suffixValue, eq, eqPrior);
    }

    /**
     * Compute restrictions separating {@code sdt1} and {@code sdt2} from a common separating path.
     *
     * @param sdt1 SDT for prefix 1
     * @param sdt2 SDT for prefix 2
     * @param oldRestrictions
     * @param mappedInPrefix  memorable data values in the common prefix of prefix 1 and 2
     * @param action1Vals data values in the last symbol of prefix 1
     * @param action2Vals data values in the last symbol of prefix 2
     * @param solver
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionsFromPruning(SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrictions, Set<DataValue> mappedInPrefix, List<DataValue> action1Vals, List<DataValue> action2Vals, ConstraintSolver solver) {
    	Optional<List<Map.Entry<SDTGuard, SDTGuard>>> pathsOpt = prune(sdt1, sdt2, solver);
    	assert pathsOpt.isPresent();
    	List<Map.Entry<SDTGuard, SDTGuard>> paths = pathsOpt.get();
    	List<SDTGuard> path = pathConjunction(paths);
    	return pathToRestrictions(path, oldRestrictions, mappedInPrefix, action1Vals, action2Vals, false);
    }

    /**
     * Compute restrictions that reveal the unmapped data values of {@code sdt}.
     *
     * @param sdt SDT for a prefix
     * @param missingRegs unmapped data values of {@code sdt}
     * @param oldRestrictions
     * @param mappedInPrefix memorable data values in last symbol of prefix
     * @param actionVals all data values of last symbol of prefix
     * @param solver
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionsFromPruning(SDT sdt, Set<DataValue> missingRegs, Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrictions, Set<DataValue> mappedInPrefix, List<DataValue> actionVals, ConstraintSolver solver) {
    	List<Map.Entry<SDTGuard, SDTGuard>> paths = pruneRegClosed(sdt, missingRegs, solver);
    	List<SDTGuard> path = pathConjunction(paths);
    	return pathToRestrictions(path, oldRestrictions, mappedInPrefix, actionVals, Arrays.asList(), true);
    }

    /**
     * Forms the conjunction of each pair in {@code paths}.
     * This method assumes that valid conjunctions can be formed, i.e., that the conjunction
     * of the guard expressions of each pair is satisfiable.
     * If a pair consists of two equality guards, this method assumes that the guards are on
     * the same register.
     *
     * @param paths
     * @return a list containing the conjunctions of each pair of {@code paths}
     */
    private static List<SDTGuard> pathConjunction(List<Map.Entry<SDTGuard, SDTGuard>> paths) {
    	List<SDTGuard> path = new ArrayList<>();
    	for (Map.Entry<SDTGuard, SDTGuard> pair : paths) {
    		SDTGuard left = pair.getKey();
    		SDTGuard right = pair.getValue();
    		assert left.getParameter().equals(right.getParameter()) : "Non-matching guards";
    		SDTGuard.EqualityGuard eg = left instanceof SDTGuard.EqualityGuard ?
    				(SDTGuard.EqualityGuard) left : (
    						right instanceof SDTGuard.EqualityGuard ?
    								(SDTGuard.EqualityGuard) right :
    									null);
    		if (eg != null) {
    			path.add(eg);
    		} else {
    			path.add(new SDTGuard.SDTTrueGuard(left.getParameter()));
    		}
    	}
    	return path;
    }

    /**
     * Convert a path of SDT guards into a set of restrictions, with calls to {@link guardToRestriction}.
     *
     * @param path
     * @param oldRestrictions
     * @param mappedInPrefix
     * @param action1Vals
     * @param action2Vals
     * @param isRegClosed
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> pathToRestrictions(List<SDTGuard> path, Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrictions, Set<DataValue> mappedInPrefix, List<DataValue> action1Vals, List<DataValue> action2Vals, boolean isRegClosed) {
    	int arity = action1Vals.size();
    	Map<SuffixValue, AbstractSuffixValueRestriction> restr = new LinkedHashMap<>();
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> old : oldRestrictions.entrySet()) {
    		SuffixValue sv = old.getKey();
    		AbstractSuffixValueRestriction oldRestr = old.getValue();
    		SDTGuard guard = path.get(sv.getId() - arity - 1);
    		restr.put(sv, guardToRestriction(guard, oldRestr, mappedInPrefix, action1Vals, action2Vals, isRegClosed));
    	}
    	return restr;
    }

    /**
     * Convert {@code guard} into a restriction. This method assumes the existence of a prefix
     * and action (and a potential action for an potential additional prefix).
     * If encountering an equality with a data value, translates it to an equality restriction.
     * This equality will be on the same data value, if it is memorable in the prefix (i.e.,
     * present in {@code mappedInPrefix}. Otherwise it will be an equality with any suffix value
     * corresponding to the action which is of the same data type.
     * If the guard is not an equality guard, return but the old restriction is an equality
     * restriction, return that restriction (with data values not in {@code mappedInPrefix}
     * replaced with action suffix values of matching type, similarly to above).
     * Otherwise, return fresh restriction.
     *
     * @param guard
     * @param oldRestriction previous restriction for the parameter of {@code guard}
     * @param mappedInPrefix memorable data values in prefix
     * @param action1Vals values in the action of prefix 1
     * @param action2Vals values in the action of prefix 2
     * @param isRegClosed {@code true} if constructing restrictions for Register Closedness special case
     * @return
     */
    private static AbstractSuffixValueRestriction guardToRestriction(SDTGuard guard, AbstractSuffixValueRestriction oldRestriction, Set<DataValue> mappedInPrefix, List<DataValue> action1Vals, List<DataValue> action2Vals, boolean isRegClosed) {
    	SuffixValue suffixValue = guard.getParameter();
    	if (guard instanceof SDTGuard.EqualityGuard eg) {
    		SDTGuardElement element = eg.register();
    		if (element instanceof DataValue d) {
				if (mappedInPrefix.contains(d)) {
					// mapped data value in prefix, so can be used in restriction
					return new EqualityRestriction(suffixValue, Set.of(d));
				}
				// not a mapped data value, so is instead an equality with a parameter in the action
				// (or an unmapped data value, if restriction is for Register Closedness special case)
				Set<SDTGuardElement> potentiallyEqualSuffixValues = potentiallyEqualSuffixValues(d, action1Vals);
				if (isRegClosed) {
					if (action1Vals.contains(d) || action2Vals.contains(d)) {
						// can be an unmapped data value, a fresh data value or any action suffix value of matching type
						return DisjunctionRestriction.create(suffixValue,
								new UnmappedEqualityRestriction(suffixValue),
								new EqualityRestriction(suffixValue, potentiallyEqualSuffixValues),
								new FreshSuffixValue(suffixValue));
					}
					// not present in action so must be unmapped or fresh
					return DisjunctionRestriction.create(suffixValue,
							new UnmappedEqualityRestriction(suffixValue),
							new FreshSuffixValue(suffixValue));
				}
				return new EqualityRestriction(suffixValue, potentiallyEqualSuffixValues);
    		} else if (element instanceof SuffixValue sv) {
    			return new EqualityRestriction(suffixValue, Set.of(sv));
    		} else if (element instanceof Constant c) {
    			return new EqualityRestriction(suffixValue, Set.of(c));
    		} else {
    			throw new IllegalArgumentException("Invalid value in equality: " + eg.register());
    		}
    	}

    	// not equality guard, check old restriction
    	if (oldRestriction instanceof EqualityRestriction er) {
    		Set<SDTGuardElement> suffixVals = new LinkedHashSet<>();
    		for (SDTGuardElement elem : er.getGuardElements()) {
    			if (elem instanceof DataValue d) {
    				if (mappedInPrefix.contains(d)) {
    					// mapped data value, can use in restriction
    					return new EqualityRestriction(suffixValue, Set.of(d));
    				}
    				// not mapped, so must be referring to action parameter
    				Set<SDTGuardElement> potentiallyEqualSuffixValues = potentiallyEqualSuffixValues(d, action1Vals);
    				return new EqualityRestriction(suffixValue, potentiallyEqualSuffixValues);
    			} else if (elem instanceof Constant c) {
    				return new EqualityRestriction(suffixValue, Set.of(c));
    			} else if (elem instanceof SuffixValue) {
    				suffixVals.add(elem);
    			}
    		}
    		assert !suffixVals.isEmpty() : "Invalid equality restriction: " + er;
    		return new EqualityRestriction(suffixValue, suffixVals);
    	}

    	// if not equality restriction, must be fresh
    	assert oldRestriction.containsFresh() : "Restriction invalid at this point: " + oldRestriction;
    	return new FreshSuffixValue(suffixValue);
    }

    /**
     * Find a "common" path in {@code sdt1} and {@code sdt2} (i.e., a path in {@code sdt1} and
     * another path in {@code sdt2} such that the conjunction of these two paths is satisfiable)
     * with different outcomes.
     *
     * @param sdt1
     * @param sdt2
     * @param restrictions
     * @param solver
     * @return {@code Optional} containing a "common" path in {@code sdt1} and {@code sdt2}, if such a path exists
     */
    private static Optional<List<Map.Entry<SDTGuard, SDTGuard>>> prune(SDT sdt1, SDT sdt2, ConstraintSolver solver) {
    	Map<List<SDTGuard>, Boolean> paths1 = sdt1.getAllPaths(new ArrayList<>());
    	Map<List<SDTGuard>, Boolean> paths2 = sdt2.getAllPaths(new ArrayList<>());
    	for (Map.Entry<List<SDTGuard>, Boolean> e1 : paths1.entrySet()) {
    		for (Map.Entry<List<SDTGuard>, Boolean> e2 : paths2.entrySet()) {
    			if (!e1.getValue().equals(e2.getValue())) {
    				// paths have different outcomes
    				List<SDTGuard> path1 = e1.getKey();
    				List<SDTGuard> path2 = e2.getKey();
    				int n = path1.size();
    				assert path2.size() == n : "SDTs are not compatible";
    				Expression[] exprs = new Expression[n + n];
    				Iterator<SDTGuard> it1 = path1.iterator();
    				Iterator<SDTGuard> it2 = path2.iterator();
    				for (int i = 0; i < n; i++) {
    					exprs[i] = SDTGuard.toExpr(it1.next());
    					exprs[i+n] = SDTGuard.toExpr(it2.next());
    				}
    				Expression<Boolean> expr = ExpressionUtil.and(exprs);
    				if (solver.isSatisfiable(expr, new Mapping<>())) {
    					// common path
    					List<SDTGuard> sorted1 = new ArrayList<>(path1);
    					List<SDTGuard> sorted2 = new ArrayList<>(path2);
    					// sort paths in ascending suffix value order
    					sorted1.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
    					sorted2.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));

    					List<Map.Entry<SDTGuard, SDTGuard>> ret = new ArrayList<>();
    					Iterator<SDTGuard> pathIt1 = sorted1.iterator();
    					Iterator<SDTGuard> pathIt2 = sorted2.iterator();
    					while (pathIt1.hasNext()) {
    						assert pathIt2.hasNext();
    						ret.add(new SimpleEntry<>(pathIt1.next(), pathIt2.next()));
    					}

    					return Optional.of(ret);
    				}
    			}
    		}
    	}
    	return Optional.empty();
    }

    private static List<Map.Entry<SDTGuard, SDTGuard>> pruneRegClosed(SDT sdt, Set<DataValue> missingRegs, ConstraintSolver solver) {
    	return pruneRegClosed(new ArrayList<>(), sdt, missingRegs, solver);
    }

    /**
     * Find a pair of paths in {@code sdt} which reveal a missing register.
     *
     * @param path
     * @param sdt
     * @param missingRegs
     * @param solver
     * @return
     */
    private static List<Map.Entry<SDTGuard, SDTGuard>> pruneRegClosed(List<Map.Entry<SDTGuard, SDTGuard>> path, SDT sdt, Set<DataValue> missingRegs, ConstraintSolver solver) {
    	if (sdt.getChildren() == null) {
    		return new ArrayList<>();
    	}

    	Map<SDTGuard, SDT> children = sdt.getChildren();
    	for (Map.Entry<SDTGuard, SDT> child : children.entrySet()) {
    		SDTGuard guard = child.getKey();
    		if (guard instanceof SDTGuard.EqualityGuard ifGuard) {
    			SDTGuardElement element = ifGuard.register();
    			if (element instanceof DataValue d && missingRegs.contains(d)) {
    				SDTGuard elseGuard = findElseGuard(children.keySet());
    				SDT ifSdt = child.getValue();
    				SDT elseSdt = children.get(elseGuard);
    				Optional<List<Map.Entry<SDTGuard, SDTGuard>>> prunedPathsOpt = prune(ifSdt, elseSdt, solver);
    				assert prunedPathsOpt.isPresent();
    				List<Map.Entry<SDTGuard, SDTGuard>> prunedPaths = prunedPathsOpt.get();

    				path.add(Map.entry(ifGuard, elseGuard));
    				path.addAll(prunedPaths);
    				return path;
    			}
    		}

    		path.add(Map.entry(guard, guard));
    		List<Map.Entry<SDTGuard, SDTGuard>> potPath = pruneRegClosed(path, child.getValue(), missingRegs, solver);
    		if (!potPath.isEmpty()) {
    			return potPath;
    		}
    	}
    	return new ArrayList<>();
    }

    private static SDTGuard findElseGuard(Set<SDTGuard> guards) {
    	for (SDTGuard guard : guards) {
    		if (isElseGuard(guard)) {
    			return guard;
    		}
    	}
    	throw new IllegalStateException("No else guard to corresponding equality guard");
    }

    /**
     * @param g
     * @return {@code true} if and only if {@code g} is an else guard
     */
    private static boolean isElseGuard(SDTGuard g) {
    	if (g instanceof SDTGuard.SDTTrueGuard) {
    		return true;
    	}
    	if (g instanceof SDTGuard.DisequalityGuard) {
    		return true;
    	}
    	if (g instanceof SDTGuard.SDTAndGuard andGuard) {
    		for (SDTGuard conjunct : andGuard.conjuncts()) {
    			if (!isElseGuard(conjunct)) {
    				return false;
    			}
    		}
    		return true;
    	}
    	return false;
    }

    /**
     * Derive restrictions for an extended symbolic suffix, i.e., {@code suffix} prepended by
     * the last symbol of {@code uExt1}. The new restrictions are derived by examining the paths
     * of {@code sdt1} and {@code sdt2} to find a "common" path in {@code sdt1} and {@code sdt2}
     * with different outcomes. The restrictions will have data values mapped to {@code uExt1}.
     *
     * @param sdt1
     * @param sdt2
     * @param uExt1
     * @param uExt2
     * @param u1RpBijection
     * @param u2RpBijection
     * @param consts
     * @param suffix
     * @param solver
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDTs(SDT sdt1, SDT sdt2, Prefix uExt1, Prefix uExt2, Bijection<DataValue> u1RpBijection, Bijection<DataValue> u2RpBijection, boolean sameLeaf, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb1 = uExt1.lastSymbol();
    	PSymbolInstance symb2 = uExt2.lastSymbol();
    	if (!symb1.getBaseSymbol().equals(symb2.getBaseSymbol())) {
    		throw new IllegalArgumentException("One-symbol extensions do not match");
    	}
    	int arity = symb1.getBaseSymbol().getArity();

    	// shift parameters
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestr = suffix.getRestrictions();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrShifted = AbstractSuffixValueRestriction.shift(oldRestr, arity);
    	SDT sdt1Shifted = sdt1.shift(arity);
    	SDT sdt2Shifted = sdt2.shift(arity);

    	// remap old restrictions from the RP of the immediate ancestor node of u1 to match u1
    	// u1 will be used as the base prefix, so all data values must be mapped to u1
    	Bijection<DataValue> uExt1FromAncestorRenaming = uExt1.getBijection(uExt1.getPath().getPrior(suffix)).inverse();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrShiftedRenamed = AbstractSuffixValueRestriction.relabel(oldRestrShifted, uExt1FromAncestorRenaming.toVarMapping());

    	// data values in the action will become suffix values, so map values in the action to their corresponding suffix values
    	Mapping<DataValue, SuffixValue> actionRenaming1 = actionValueToSuffixValue(uExt1);
    	Mapping<DataValue, SuffixValue> actionRenaming2 = actionValueToSuffixValue(uExt2);
    	SDT sdt1ActionRenamed = sdt1Shifted.relabel(SDTRelabeling.fromMapping(actionRenaming1));
    	SDT sdt2ActionRenamed = sdt2Shifted.relabel(SDTRelabeling.fromMapping(actionRenaming2));
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrActionRenamed = AbstractSuffixValueRestriction.relabel(oldRestrShiftedRenamed, actionRenaming1);

    	// remap data values of uExt2 to uExt1 in such a way that there are no collisions for data values in uExt2 that have no correlation to uExt1
    	Bijection<DataValue> uExt2Renaming = collisionFreeRenaming(uExt1, uExt2, u1RpBijection, u2RpBijection, suffix, sameLeaf);
    	SDT sdt2Renamed = sdt2ActionRenamed.relabel(SDTRelabeling.fromBijection(uExt2Renaming));

    	// get memorable data values of u1 and data values in the actions of uExt1 and uExt2
    	Set<DataValue> mappedInPrefix = u1RpBijection.keySet();
    	List<DataValue> action1Vals = Arrays.asList(symb1.getParameterValues());
    	List<DataValue> action2Vals = new ArrayList<>();
    	// map uExt2 action data values to uExt1
    	renameCollection(action2Vals, Arrays.asList(symb2.getParameterValues()), uExt2Renaming);

    	// derive restrictions
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrPruned = restrictionsFromPruning(sdt1ActionRenamed, sdt2Renamed, oldRestrActionRenamed, mappedInPrefix, action1Vals, action2Vals, solver);

    	// replace restrictions on data values in the actions of uExt2 with their corresponding suffix values
    	Bijection<DataValue> uExt2FromAncestorRenaming = uExt2.getBijection(uExt2.getPath().getPrior(suffix)).inverse();
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrElseParams = addActionParameter(restrPruned, actionRenaming2, uExt1FromAncestorRenaming.inverse(), uExt2FromAncestorRenaming);

    	return restrElseParams;
    }

    private static void renameCollection(Collection<DataValue> dest, Collection<DataValue> col, Bijection<DataValue> renaming) {
    	for (DataValue d : col) {
    		if (renaming.containsKey(d)) {
    			dest.add(renaming.get(d));
    		}
    	}
    }

    /**
     * Derive restrictions for an extended symbolic suffix, i.e., {@code suffix} prepended by
     * the last symbol of {@code uExt}. The restrictions are derived by examining paths of
     * {@code sdt} to find and isolate paths that reveal unmapped data values. Each suffix value
     * with a guard on unmapped data value will have a {@link TrueRestriction}, while any other
     * will have a restriction given by the conjunction of existing restrictions in {@code suffix}
     * and restrictions derived from the paths in {@code sdt} which reveal the unmapped data
     * values.
     *
     * @param sdt
     * @param uExt
     * @param rp
     * @param consts
     * @param suffix
     * @param solver
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDT(SDT sdt, Prefix u, Prefix uExt, Bijection<DataValue> rp, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver, boolean useImprovedRegClosed) {
    	PSymbolInstance symb = uExt.lastSymbol();
    	int arity = symb.getBaseSymbol().getArity();
    	List<DataValue> actionVals = Arrays.asList(symb.getParameterValues());

    	Set<DataValue> missingRegs = new LinkedHashSet<>(sdt.getDataValues());
    	missingRegs.removeAll(rp.keySet());

    	if (!Collections.disjoint(actionVals, missingRegs) || !useImprovedRegClosed) {
    		return transferRestriction(sdt, u, uExt, rp, consts, suffix, solver);
    	}

    	Bijection<DataValue> ancestorRenaming = uExt.getBijection(uExt.getPath().getPrior(suffix)).inverse();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestr = suffix.getRestrictions();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrRenamed = AbstractSuffixValueRestriction.relabel(oldRestr, ancestorRenaming.toVarMapping());

    	SDT sdtShifted = sdt.shift(arity);
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrRenamedShifted = AbstractSuffixValueRestriction.shift(oldRestrRenamed, arity);

    	Set<DataValue> mappedInPrefix = rp.keySet();

    	return restrictionsFromPruning(sdtShifted, missingRegs, oldRestrRenamedShifted, mappedInPrefix, actionVals, solver);
    }

    /**
     * Shift restrictions one action-arity to the right.
     * If there are any equality restrictions on a data value that is not memorable in {@code u},
     * replace that with an equality restriction on any suffix value in the action that is
     * of a matching type to that data value.
     *
     * @param sdt
     * @param u
     * @param uExt
     * @param rp
     * @param consts
     * @param suffix
     * @param solver
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> transferRestriction(SDT sdt, Prefix u, Prefix uExt, Bijection<DataValue> rp, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb = uExt.lastSymbol();
    	ArrayList<DataValue> symbVals = new ArrayList<>(Arrays.asList(symb.getParameterValues()));

    	Set<DataValue> missingRegs = new LinkedHashSet<>(sdt.getDataValues());
    	missingRegs.removeAll(rp.keySet());

    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = AbstractSuffixValueRestriction.shift(suffix.getRestrictions(), symb.getBaseSymbol().getArity());
    	sdt = sdt.shift(symb.getBaseSymbol().getArity());

    	Bijection<DataValue> ancestorRenaming = uExt.getBijection(uExt.getPath().getPrior(suffix));

    	// find missing registers in restriction and replace those that are in the action with suffix params
    	Mapping<DataValue, SuffixValue> suffixValueRenaming = new Mapping<>();
    	for (DataValue r : missingRegs) {
    		if (symbVals.contains(r) && ancestorRenaming.containsKey(r)) {
    			SuffixValue s = new SuffixValue(r.getDataType(), symbVals.indexOf(r));
    			suffixValueRenaming.put(r, s);
    		}
    	}
    	ret = AbstractSuffixValueRestriction.relabel(ret, suffixValueRenaming);

    	// replace unmapped restriction depending on sdt guards
    	ret = replaceUnmappedRestriction(ret, u, uExt, sdt);

    	return AbstractSuffixValueRestriction.relabel(ret, ancestorRenaming.inverse().toVarMapping());
    }

    /**
     * Check old unmapped restrictions have discovered a new mapped value, or discovered that
     * a prior unmapped value is a value in the action and therefore now a suffix value.
     *
     * @param restr
     * @param u
     * @param uExt
     * @param sdt
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> replaceUnmappedRestriction(Map<SuffixValue, AbstractSuffixValueRestriction> restr, Prefix u, Prefix uExt, SDT sdt) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	List<DataValue> symbVals = Arrays.asList(uExt.lastSymbol().getParameterValues());
    	Set<DataValue> uMem = u.getRegisters();

    	Set<SuffixValue> unmappedSuffixVals = AbstractSuffixValueRestriction.unmappedSuffixValues(restr);
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : restr.entrySet()) {
    		SuffixValue sv = e.getKey();
    		if (!unmappedSuffixVals.contains(sv)) {
    			ret.put(sv, e.getValue());
    			continue;
    		}
    		List<AbstractSuffixValueRestriction> disjuncts = new ArrayList<>();
    		disjuncts.add(new FreshSuffixValue(sv));
    		Set<SDTGuardElement> eqElems = new LinkedHashSet<>();
    		for (SDTGuard g : sdt.getGuards(sv)) {
    			if (g instanceof SDTGuard.EqualityGuard eg) {
    				SDTGuardElement element = eg.register();
    				if (SDTGuardElement.isDataValue(element)) {
    					DataValue d = (DataValue) element;
    					if (uMem.contains(d)) {
    						eqElems.add(element);
    					} else {
    						disjuncts.add(new UnmappedEqualityRestriction(sv));
    					}
    					if (symbVals.contains(d)) {
							eqElems.addAll(potentiallyEqualSuffixValues(d, symbVals));
						}
    				} else if (SDTGuardElement.isSuffixValue(element)) {
    					eqElems.add(element);
    				}
    			}
    		}
    		if (!eqElems.isEmpty()) {
    			disjuncts.add(new EqualityRestriction(sv, eqElems));
    		}
    		ret.put(sv, DisjunctionRestriction.create(sv, disjuncts));
    	}
    	return ret;
    }

    /**
     * Find a mapping for the data values from {@code uExt2} to {@code uExt1}. This mapping
     * ensures that the mappings of data values from {@code uExt1} and {@code uExt2} to their
     * ancestor node is adhered to. Data values of {@code uExt2} that have no mapping to the
     * ancestor node are renamed to ensure there is no collision with data values in
     * {@code uExt1} that are also not mapped to the ancestor node.
     *
     * @param uExt1
     * @param uExt2
     * @param u1RpBijection
     * @param u2RpBijection
     * @param suffix
     * @param sameLeaf
     * @return
     */
    private static Bijection<DataValue> collisionFreeRenaming(Prefix uExt1, Prefix uExt2, Bijection<DataValue> u1RpBijection, Bijection<DataValue> u2RpBijection, SymbolicSuffix suffix, boolean sameLeaf) {
    	Set<DataValue> usedVals = DataWords.valSet(uExt1);
    	Bijection<DataValue> freshRenaming = new Bijection<>();
    	for (DataValue d : DataWords.valsOf(uExt2)) {
    		DataValue fresh = EqualityTheory.getFreshValue(usedVals, d.getDataType());
    		freshRenaming.put(d, fresh);
    		usedVals.add(fresh);
    	}

    	Bijection<DataValue> uExt1AncestorRenaming = uExt1.getBijection(uExt1.getPath().getPrior(suffix));
    	Bijection<DataValue> uExt2AncestorRenaming = uExt2.getBijection(uExt2.getPath().getPrior(suffix));
    	Bijection<DataValue> uExt1RpBijection = uExt1.getRpBijection();
    	Bijection<DataValue> uExt2RpBijection = uExt2.getRpBijection();

    	List<Bijection<DataValue>> bijections = new ArrayList<>();

    	bijections.add(uExt2AncestorRenaming.compose(uExt1AncestorRenaming.inverse()));
    	bijections.add(u2RpBijection.compose(u1RpBijection.inverse()));
    	if (sameLeaf) {
    		bijections.add(uExt2RpBijection.compose(uExt1RpBijection.inverse()));
    	}

    	Bijection<DataValue> renaming = new Bijection<>(freshRenaming);
    	for (Bijection<DataValue> b : bijections) {
    		renaming = updateRenaming(renaming, b);
    	}
    	return renaming;
    }

    /**
     * @param renaming
     * @param b
     * @return {@code b} added to {@code renaming}
     */
    private static Bijection<DataValue> updateRenaming(Bijection<DataValue> renaming, Bijection<DataValue> b) {
    	Bijection<DataValue> ret = new Bijection<>(renaming);
    	for (Map.Entry<DataValue, DataValue> e : b.entrySet()) {
    		ret.put(e.getKey(), e.getValue());
    	}
    	return ret;
    }

    /**
     * Map data values in the last symbol of {@code uExt} (the action) to their corresponding
     * suffix values when the action is made symbolic.
     *
     * @param uExt
     * @return
     */
    private static Mapping<DataValue, SuffixValue> actionValueToSuffixValue(Word<PSymbolInstance> uExt) {
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(uExt.prefix(uExt.length() - 1)));
    	DataValue[] actionVals = uExt.lastSymbol().getParameterValues();

    	Mapping<DataValue, SuffixValue> ret = new Mapping<>();
    	for (int i = 0; i < actionVals.length; i++) {
    		DataValue d = actionVals[i];
    		if (!uVals.contains(d)) {
    			SuffixValue sv = new SuffixValue(d.getDataType(), i + 1);
    			ret.put(d, sv);
    		}
    	}
    	return ret;
    }

    /**
     * Compute set of suffix values corresponding to each data value in {@code vals} with the
     * same type as {@code d}. The id for each suffix value is given by the position of its
     * corresponding value in {@code vals}.
     *
     * @param d
     * @param vals
     * @return set of suffix values corresponding to {@code vals} with the same type as {@code d}
     */
    private static Set<SDTGuardElement> potentiallyEqualSuffixValues(DataValue d, List<DataValue> vals) {
    	Set<SDTGuardElement> ret = new LinkedHashSet<>();
    	for (int i = 0; i < vals.size(); i++) {
    		if (vals.get(i).getDataType().equals(d.getDataType())) {
    			ret.add(new SuffixValue(d.getDataType(), i + 1));
    		}
    	}
    	return ret;
    }

    /**
     * @param restr
     * @return the set of {@code DataValue} elements of {@code restr}
     */
    private static Set<DataValue> getDataValueElements(Map<SuffixValue, AbstractSuffixValueRestriction> restr) {
    	return AbstractSuffixValueRestriction.getElements(restr)
    			.stream()
    			.filter(e -> e instanceof DataValue)
    			.map(d -> (DataValue) d)
    			.collect(Collectors.toSet());
    }

    /**
     * Given a prefix {@code ua}, where {@code a} (the action) is a one-symbol extension,
     * checks for any equality restriction with a data value in {@code a}. If such an equality
     * restriction is found, add to it an equality restriction with the value's corresponding
     * suffix value, as given by {@code actionRenaming}.
     *
     * @param restr
     * @param actionRenaming mapping from values in action to corresponding suffix value
     * @param toAncestorRenaming mapping of data values from {@code restr} to ancestor node
     * @param fromAncestorToExtRenaming mapping of data values from ancestor node to prefix
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> addActionParameter(Map<SuffixValue, AbstractSuffixValueRestriction> restr, Mapping<DataValue, SuffixValue> actionRenaming, Bijection<DataValue> toAncestorRenaming, Bijection<DataValue> fromAncestorToExtRenaming) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restr;
    	Set<DataValue> restrVals = getDataValueElements(restr);
    	for (DataValue d : restrVals) {
    		if (toAncestorRenaming.containsKey(d)) {
    			DataValue dAncestor = toAncestorRenaming.get(d);
    			DataValue dExt = fromAncestorToExtRenaming.get(dAncestor);
    			assert dExt != null : "Data value of ancestor node not present in bijection";
    			if (actionRenaming.containsKey(dExt)) {
    				for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restr, d)) {
    					SuffixValue sv = er.cast().getParameter();
    					assert er instanceof EqualityRestriction : "Unsupported restriction type";
    					EqualityRestriction replace = (EqualityRestriction) er;
    					Set<SDTGuardElement> elems = new LinkedHashSet<>(replace.getGuardElements());
    					elems.add(actionRenaming.get(dExt));
    					EqualityRestriction by = new EqualityRestriction(sv, elems);
    					ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
    				}
    			}
    		}
    	}
    	return ret;
    }
}
