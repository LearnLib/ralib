package de.learnlib.ralib.ceanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RARun;
import de.learnlib.ralib.ct.CTHypothesis;
import de.learnlib.ralib.ct.CTLeaf;
import de.learnlib.ralib.ct.ClassificationTree;
import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.ct.ShortPrefix;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaEqRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.ReplacingVarsVisitor;
import de.learnlib.ralib.smt.VarsValuationVisitor;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class PrefixFinderEq extends PrefixFinder {

	public PrefixFinderEq(TreeOracle sulOracle, CTHypothesis hyp, ClassificationTree ct, Map<DataType, Theory> teachers,
			SLLambdaEqRestrictionBuilder restrBuilder, ConstraintSolver solver, Constants consts) {
		super(sulOracle, hyp, ct, teachers, restrBuilder, solver, consts);
		if (!isEqTheory(teachers)) {
			throw new RuntimeException("PrefixFinderEq only supports theories of type EqualityTheory");
		}
	}

	@Override
	public Result analyzeCounterExample(Word<PSymbolInstance> ce) {
		RARun run = hyp.getRun(ce);
		for (int i = ce.length(); i >= 1; i--) {
			RALocation loc = run.getLocation(i - 1);
			CTLeaf leaf = hyp.getLeaf(loc);
			for (ShortPrefix u : leaf.getShortPrefixes()) {
				Optional<Result> result = checkTransition(u, run, i);
				if (result.isEmpty()) {
					result = checkLocation(u, run, i);
				}
				if (result.isPresent()) {
					return result.get();
				}
			}
		}
		throw new IllegalStateException("Found no counterexample in " + ce);
	}

	private SLLambdaEqRestrictionBuilder getRestrBuilder() {
		return (SLLambdaEqRestrictionBuilder) restrBuilder;
	}

	/**
	 * Check for a transition discrepancy. This is done by checking whether there exists no
	 * one-symbol extension of {@code u} in the leaf of the location of {@code run} at
	 * index {@code id} that, after the symbolic suffix derived from the concrete suffix
	 * of {@code run} after {@code id}, is equivalent to any one-symbol extension of {@code u}
	 * in the hypothesis.
	 *
	 * @param u short prefix from leaf of {@code loc}
	 * @param run counterexample run on the hypothesis
	 * @param id index of {@code run} being searched
	 * @return an {@code Optional} containing the result if there is a transition discrepancy, or an empty {@code Optional} otherwise
	 */
	private Optional<Result> checkTransition(ShortPrefix u, RARun run, int i) {
		int arity = run.getTransitionSymbol(i).getBaseSymbol().getArity();
		if (arity == 0) {
			return Optional.empty();
		}
		return checkTransition(new DataValue[arity], 0, u, run, i);
	}

	/**
	 * For each possible set of data values the action may take according to the concrete suffix,
	 * check whether the resulting (prefix+action) word is inequivalent to an existing prefix
	 * extension. The prefix and suffix are taken from {@code run} at index {@code i-1} (for prefix)
	 * and {@code i} (for suffix).
	 *
	 * @param dvals already generated values
	 * @param did index of next value not yet generated
	 * @param u short prefix matching {@code run.getPrefix(i-1)}
	 * @param run run of hypothesis over counterexample
	 * @param i index of run to check
	 * @return {@code Optional} enclosing new transition, if one is found, otherwise {@code Optional.empty()}
	 */
	private Optional<Result> checkTransition(DataValue[] dvals, int did, ShortPrefix u, RARun run, int i) {
		Word<PSymbolInstance> prefix = run.getPrefix(i - 1);
		Word<PSymbolInstance> prefixNext = run.getPrefix(i);
		Word<PSymbolInstance> suffixNext = run.getSuffix(i);
		RegisterValuation prefixValuation = run.getValuation(i - 1);
		RegisterValuation prefixExtValuation = run.getValuation(i);

		PSymbolInstance action = run.getTransitionSymbol(i);
		DataValue d = action.getParameterValues()[did];

		// find the indices of data values in u that parameter with index did may be equal to
		EqualityTheory et = (EqualityTheory) teachers.get(d.getDataType());
		RegisterValuation uValuation = hyp.getRun(u).getValuation(u.length());
		Map<Integer, DataValue> potmap = et.potmap(u, uValuation, prefix, prefixValuation, d.getDataType());
		Set<Integer> potmatch = et.potmatch(prefix, d, u, uValuation, potmap);

		if (potmatch.isEmpty()) {
			// not equal to a data value in the prefix, could equal a constant or a prior data value in the action
			Word<PSymbolInstance> suffix = run.getSuffix(i - 1);
			SymbolicSuffix v = getRestrBuilder().constructRestrictedSuffix(prefix, suffix, u, prefixValuation, uValuation);
			SymbolicSuffix vHyp = SLLambdaEqRestrictionBuilder.concretize(v, uValuation, ParameterValuation.fromPSymbolWord(u), consts);
			SDT sdt = sulOracle.treeQuery(u, vHyp);
			Branching branching = sulOracle.getInitialBranching(u, action.getBaseSymbol(), sdt);
			Set<Expression<Boolean>> guards = branching.guardSet();
			Set<Word<PSymbolInstance>> sulExtensions = instantiateGuards(guards, vHyp, u, hyp.getRun(u).getValuation(u.length()).keySet(), action.getBaseSymbol());
			Set<Word<PSymbolInstance>> hypExtensions = ct.getExtensions(u, action.getBaseSymbol());
			// check for any extension on the sul not already covered by the hyp
			for (Word<PSymbolInstance> uExt : sulExtensions) {
				if (!hypExtensions.contains(uExt)) {
					return Optional.of(new Result(uExt, ResultType.TRANSITION));
				}
			}

		}

		// for each data value in action allowed by the potmatch, check if (prefix+action) is equivalent to an existing extension
		DataValue[] uVals = DataWords.valsOf(u);
		POTMATCH: for (int l : potmatch) {
			DataValue dPot = uVals[l - 1];
			dvals[did] = dPot;
			if (did + 1 < action.getBaseSymbol().getArity()) {
				// not final index, check each potmatch for next index
				Optional<Result> res = checkTransition(dvals, did + 1, u, run, i);
				if (res.isPresent()) {
					return res;
				}
			} else {
				// final index, construct (prefix+action) symbol and check equivalence with prefixNext
				PSymbolInstance psi = new PSymbolInstance(action.getBaseSymbol(), dvals);
				Word<PSymbolInstance> uExtSul = u.append(psi);
				Set<Word<PSymbolInstance>> extensions = ct.getExtensions(u, action.getBaseSymbol());
				if (extensions.contains(uExtSul)) {
					continue;
				}
				for (Word<PSymbolInstance> uExtHyp : extensions) {
					RegisterValuation uExtSulValuation = hyp.getRun(uExtSul).getValuation(uExtSul.length());
					RegisterValuation uExtHypValuation = hyp.getRun(uExtHyp).getValuation(uExtHyp.length());
					SymbolicSuffix v = getRestrBuilder().constructRestrictedSuffix(prefixNext, suffixNext, uExtSul, prefixExtValuation, uExtSulValuation);
					SymbolicSuffix vSul = SLLambdaEqRestrictionBuilder.concretize(v, uExtSulValuation, ParameterValuation.fromPSymbolWord(uExtSul), consts);
					SymbolicSuffix vHyp = SLLambdaEqRestrictionBuilder.concretize(v, uExtHypValuation, ParameterValuation.fromPSymbolWord(uExtHyp), consts);

					SDT sdtSul = sulOracle.treeQuery(uExtSul, vSul).toRegisterSDT(uExtSul, consts);
					SDT sdtHyp = sulOracle.treeQuery(uExtHyp, vHyp).toRegisterSDT(uExtHyp, consts);
					if (SDT.equalUnderActionRemapping(sdtSul, sdtHyp, uExtSul, uExtHyp)) {
						continue POTMATCH;
					}
				}
				return Optional.of(new Result(uExtSul, ResultType.TRANSITION));
			}
		}
		return Optional.empty();
	}

	/**
	 * Check whether any extension of {@code u} is inequivalent to a short prefix in location
	 * {@code run.getLocation(i)}.
	 *
	 * @param u
	 * @param run
	 * @param i
	 * @return {@code Optional} enclosing an inequivalent transition, if one exists, otherwise {@code Optional.empty()}
	 */
	private Optional<Result> checkLocation(ShortPrefix u, RARun run, int i) {
		Word<PSymbolInstance> prefix = run.getPrefix(i);
		Word<PSymbolInstance> suffix = run.getSuffix(i);
		RegisterValuation prefixValuation = run.getValuation(i);
		CTLeaf leafNext = hyp.getLeaf(run.getLocation(i));
		PSymbolInstance action = prefix.lastSymbol();

		Iterator<Prefix> extensions = ct.getExtensions(u, action.getBaseSymbol())
				.stream()
				.filter(w -> leafNext.getPrefixes().contains(w))
				.map(w -> leafNext.getPrefix(w))
				.iterator();
		EXTENSIONS: while (extensions.hasNext()) {
			Prefix uExt = extensions.next();
			RegisterValuation uExtValuation = hyp.getRun(uExt).getValuation(uExt.length());
			Bijection<DataValue> uExtBijection = uExt.getRpBijection();
			for (ShortPrefix uNext : leafNext.getShortPrefixes()) {
				RegisterValuation uNextValuation = hyp.getRun(uNext).getValuation(uNext.length());
				SymbolicSuffix v = getRestrBuilder().constructRestrictedSuffix(prefix, suffix, uExt, uNext, prefixValuation, uExtValuation, uNextValuation);

				SymbolicSuffix vuExt = SLLambdaEqRestrictionBuilder.concretize(v, uExtValuation, ParameterValuation.fromPSymbolWord(uExt), consts);
				SymbolicSuffix vuNext = SLLambdaEqRestrictionBuilder.concretize(v, uNextValuation, ParameterValuation.fromPSymbolWord(uNext), consts);

				Bijection<DataValue> uNextBijection = uNext.getRpBijection();
				Bijection<DataValue> gamma = uNextBijection.compose(uExtBijection.inverse());
				SDT uExtSDT = sulOracle.treeQuery(uExt, vuExt);
				SDT uNextSDT = sulOracle.treeQuery(uNext, vuNext);
				if (SDT.equivalentUnderBijection(uNextSDT, uExtSDT, gamma) != null) {
					continue EXTENSIONS;
				}
			}
			return Optional.of(new Result(uExt, ResultType.LOCATION));
		}
		return Optional.empty();
	}

	/**
	 * For each guard in {@code guards}, instantiate an action whose values satisfy the guard
	 * and the restrictions of {@code suffix}, and return a set of words formed by appending
	 * the actions to {@code u}.
	 *
	 * @param guards
	 * @param suffix
	 * @param u
	 * @param regs
	 * @param action
	 * @return
	 */
	private Set<Word<PSymbolInstance>> instantiateGuards(Set<Expression<Boolean>> guards, SymbolicSuffix suffix, Word<PSymbolInstance> u, Set<Register> regs, ParameterizedSymbol action) {
		Set<Word<PSymbolInstance>> extensions = new LinkedHashSet<>();
		for (Expression<Boolean> guard : guards) {
			Expression<Boolean> con = conjunctionWithRestriction(guard, suffix, u, regs, consts);
			List<DataValue> vals = new ArrayList<>();
			DataValue[] valsArr = new DataValue[action.getArity()];
			for (int i = 0; i < action.getArity(); i++) {
				DataType t = action.getPtypes()[i];
				Theory theory = teachers.get(t);
				assert theory instanceof EqualityTheory;
				EqualityTheory et = (EqualityTheory) theory;
				Optional<DataValue> dOpt = et.instantiate(u, action, con, i + 1, vals, consts, solver);
				assert dOpt.isPresent();
				vals.add(dOpt.get());
				valsArr[i] = dOpt.get();
			}
			PSymbolInstance psi = new PSymbolInstance(action, valsArr);
			extensions.add(u.append(psi));
		}
		return extensions;
	}

	/**
	 * Compute conjunction of {@code guard} and the restrictions of {@code suffix}.
	 *
	 * @param guard
	 * @param suffix
	 * @param u
	 * @param regs
	 * @param consts
	 * @return
	 */
	private Expression<Boolean> conjunctionWithRestriction(Expression<Boolean> guard, SymbolicSuffix suffix, Word<PSymbolInstance> u, Set<Register> regs, Constants consts) {
		DataType[] types = null;
		for (ParameterizedSymbol ps : suffix.getActions()) {
			if (ps.getArity() > 0) {
				types = ps.getPtypes();
				break;
			}
		}
		if (types == null) {
			return guard;
		}
		SuffixValueGenerator sgen = new SuffixValueGenerator();

		Set<SymbolicDataValue> vals = new LinkedHashSet<>();
		DataValue[] uVals = DataWords.valsOf(u);
		ParameterGenerator pgen = new ParameterGenerator();
		ParameterValuation pmap = new ParameterValuation();
		for (int i = 0; i < uVals.length; i++) {
			Parameter p = pgen.next(uVals[i].getDataType());
			vals.add(p);
			pmap.put(p, uVals[i]);
		}
		vals.addAll(regs);
		vals.addAll(consts.keySet());

		List<Expression<Boolean>> restrictionExpressions = new ArrayList<>();
		VarMapping<SuffixValue, Parameter> paramMapping = new VarMapping<>();
		for (int i = 0; i < types.length; i++) {
			if (!teachers.containsKey(types[i]) || !teachers.get(types[i]).isUsingSuffixOptimization()) {
				continue;
			}

			SuffixValue s = sgen.next(types[i]);
			Parameter p = new Parameter(s.getDataType(), s.getId());
			AbstractSuffixValueRestriction r = suffix.getRestriction(s);
			Expression<Boolean> expr = r.toGuardExpression(vals);

			VarsValuationVisitor vvv = new VarsValuationVisitor();
			expr = vvv.apply(expr, pmap);

			ReplacingVarsVisitor rvv = new ReplacingVarsVisitor();
			paramMapping.put(s, p);
			Expression<Boolean> renamedExpr = rvv.apply(expr, paramMapping);
			restrictionExpressions.add(renamedExpr);
		}
		restrictionExpressions.add(guard);
		Expression<Boolean> con = ExpressionUtil.and(restrictionExpressions.toArray(new Expression[restrictionExpressions.size()]));
		return con;
	}

	/**
	 * @param teachers
	 * @return {@code true} if and only if all data types of {@code teachers} are associated with {@code EqualityTheory}
	 */
	private static boolean isEqTheory(Map<DataType, Theory> teachers) {
		for (Map.Entry<DataType, Theory> t : teachers.entrySet()) {
			if (t.getKey() == null || !(t.getValue() instanceof EqualityTheory)) {
				return false;
			}
		}
		return true;
	}
}
