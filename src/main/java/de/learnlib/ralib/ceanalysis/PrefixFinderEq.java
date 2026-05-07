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
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

public class PrefixFinderEq extends PrefixFinder {

	public PrefixFinderEq(TreeOracle sulOracle, CTHypothesis hyp, ClassificationTree ct, Map<DataType, Theory> teachers,
			SLLambdaRestrictionBuilder restrBuilder, ConstraintSolver solver, Constants consts) {
		super(sulOracle, hyp, ct, teachers, restrBuilder, solver, consts);
		if (!isEqTheory(teachers)) {
			throw new RuntimeException("PrefixFinderEq onlu supports theories of type EqualityTheory");
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

	private Optional<Result> checkTransition(ShortPrefix u, RARun run, int i) {
		int arity = run.getTransitionSymbol(i).getBaseSymbol().getArity();
		if (arity == 0) {
			return Optional.empty();
		}
		return checkTransition(new DataValue[arity], 0, u, run, i);
	}

	private Optional<Result> checkTransition(DataValue[] dvals, int did, ShortPrefix u, RARun run, int i) {
		Word<PSymbolInstance> prefix = run.getPrefix(i - 1);
		Word<PSymbolInstance> prefixNext = run.getPrefix(i);
		Word<PSymbolInstance> suffixNext = run.getSuffix(i);
		RegisterValuation prefixValuation = run.getValuation(i - 1);
//		RegisterValuation prefixExtValuation = extendValuation(prefixValuation, run.getTransitionSymbol(i), DataWords.paramValLength(u));
		RegisterValuation prefixExtValuation = run.getValuation(i);

		PSymbolInstance action = run.getTransitionSymbol(i);
		DataValue d = action.getParameterValues()[did];
//		dvals[did] = d;
//		did = did + 1;

		EqualityTheory et = (EqualityTheory) teachers.get(d.getDataType());
		RegisterValuation uValuation = hyp.getRun(u).getValuation(u.length());
		Map<Integer, DataValue> potmap = et.potmap(u, uValuation, prefix, prefixValuation, d.getDataType());
		Set<Integer> potmatch = et.potmatch(prefix, d, u, uValuation, potmap);

		if (potmatch.isEmpty()) {
			Word<PSymbolInstance> suffix = run.getSuffix(i - 1);
			SymbolicSuffix v = restrBuilder.constructRestrictedSuffix(prefix, suffix, u, prefixValuation, uValuation);
			SymbolicSuffix vHyp = restrBuilder.concretize(v, uValuation, ParameterValuation.fromPSymbolWord(u), consts);
			SDT sdt = sulOracle.treeQuery(u, vHyp);
			Branching branching = sulOracle.getInitialBranching(u, action.getBaseSymbol(), sdt);
			Set<Expression<Boolean>> guards = branching.guardSet();
			Set<Word<PSymbolInstance>> sulExtensions = instantiateGuards(guards, vHyp, u, hyp.getRun(u).getValuation(u.length()).keySet(), action.getBaseSymbol());
			Set<Word<PSymbolInstance>> hypExtensions = ct.getExtensions(u, action.getBaseSymbol());
			for (Word<PSymbolInstance> uExt : sulExtensions) {
				if (!hypExtensions.contains(uExt)) {
					return Optional.of(new Result(uExt, ResultType.TRANSITION));
				}
			}

		}

		DataValue[] uVals = DataWords.valsOf(u);
		POTMATCH: for (int l : potmatch) {
			DataValue dPot = uVals[l - 1];
			dvals[did] = dPot;
			if (did + 1 < action.getBaseSymbol().getArity()) {
				Optional<Result> res = checkTransition(dvals, did + 1, u, run, i);
				if (res.isPresent()) {
					return res;
				}
			} else {
				PSymbolInstance psi = new PSymbolInstance(action.getBaseSymbol(), dvals);
				Word<PSymbolInstance> uExtSul = u.append(psi);
				Set<Word<PSymbolInstance>> extensions = ct.getExtensions(u, action.getBaseSymbol());
				if (extensions.contains(uExtSul)) {
					continue;
				}
				for (Word<PSymbolInstance> uExtHyp : extensions) {
//					if (uExtHyp.equals(uExtSul)) {
//						continue POTMATCH;
//					}
					RegisterValuation uExtSulValuation = hyp.getRun(uExtSul).getValuation(uExtSul.length());
					RegisterValuation uExtHypValuation = hyp.getRun(uExtHyp).getValuation(uExtHyp.length());
					SymbolicSuffix v = restrBuilder.constructRestrictedSuffix(prefixNext, suffixNext, uExtSul, prefixExtValuation, uExtSulValuation);
					SymbolicSuffix vSul = restrBuilder.concretize(v, uExtSulValuation, ParameterValuation.fromPSymbolWord(uExtSul), consts);
					SymbolicSuffix vHyp = restrBuilder.concretize(v, uExtHypValuation, ParameterValuation.fromPSymbolWord(uExtHyp), consts);

					SDT sdtSul = sulOracle.treeQuery(uExtSul, vSul).toRegisterSDT(uExtSul, consts);
					SDT sdtHyp = sulOracle.treeQuery(uExtHyp, vHyp).toRegisterSDT(uExtHyp, consts);
//					if (SDT.equivalentUnderId(sdtSul, sdtHyp)) {
					if (SDT.equalUnderActionRemapping(sdtSul, sdtHyp, uExtSul, uExtHyp)) {
						continue POTMATCH;
					}
				}
				return Optional.of(new Result(uExtSul, ResultType.TRANSITION));
			}
		}
		return Optional.empty();
	}

	private Optional<Result> checkLocation(ShortPrefix u, RARun run, int i) {
		Word<PSymbolInstance> prefix = run.getPrefix(i);
		Word<PSymbolInstance> suffix = run.getSuffix(i);
		RegisterValuation prefixValuation = run.getValuation(i);
		CTLeaf leaf = hyp.getLeaf(run.getLocation(i - 1));
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
				SymbolicSuffix v = restrBuilder.constructRestrictedSuffix(prefix, suffix, uExt, uNext, prefixValuation, uExtValuation, uNextValuation);

				SymbolicSuffix vuExt = restrBuilder.concretize(v, uExtValuation, ParameterValuation.fromPSymbolWord(uExt), consts);
				SymbolicSuffix vuNext = restrBuilder.concretize(v, uNextValuation, ParameterValuation.fromPSymbolWord(uNext), consts);

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

	private RegisterValuation extendValuation(RegisterValuation valuation, PSymbolInstance psi, int offset) {
		RegisterValuation ret = new RegisterValuation();
		ret.putAll(valuation);
		int id = offset + 1;
		for (DataValue d : psi.getParameterValues()) {
			Register r = new Register(d.getDataType(), id);
			ret.put(r, d);
			id = id + 1;
		}
		return ret;
	}

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

	private static boolean isEqTheory(Map<DataType, Theory> teachers) {
		for (Map.Entry<DataType, Theory> t : teachers.entrySet()) {
			if (t.getKey() == null || !(t.getValue() instanceof EqualityTheory)) {
				return false;
			}
		}
		return true;
	}
}
