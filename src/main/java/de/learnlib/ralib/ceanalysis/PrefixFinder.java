package de.learnlib.ralib.ceanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.PermutationIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.ReplacingValuesVisitor;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class PrefixFinder {

	public enum ResultType {
		TRANSITION,
		LOCATION
	}

	public record Result(Word<PSymbolInstance> prefix, ResultType result) {};

	private final CTHypothesis hyp;
	private final ClassificationTree ct;

	private final TreeOracle sulOracle;
	private final Map<DataType, Theory> teachers;

	private final SymbolicSuffixRestrictionBuilder restrBuilder;

	private final ConstraintSolver solver;

	private final Constants consts;

	public PrefixFinder(TreeOracle sulOracle, CTHypothesis hyp, ClassificationTree ct,
			Map<DataType, Theory> teachers, SymbolicSuffixRestrictionBuilder restrBuilder,
			ConstraintSolver solver, Constants consts) {
		this.hyp = hyp;
		this.ct = ct;
		this.sulOracle = sulOracle;
		this.teachers = teachers;
		this.restrBuilder = restrBuilder;
		this.solver = solver;
		this.consts = consts;
	}

	public Result analyzeCounterExample(Word<PSymbolInstance> ce) {
		RARun run = hyp.getRun(ce);
		for (int i = ce.length(); i >= 1; i--) {
			RALocation loc = run.getLocation(i-1);
			RALocation locNext = run.getLocation(i);
			PSymbolInstance symbol = run.getTransition(i);
			RegisterValuation mu = run.getValuation(i-1);
			ParameterizedSymbol action = symbol.getBaseSymbol();

			SymbolicSuffix vNext = new SymbolicSuffix(ce.prefix(i), ce.suffix(ce.length() - i), restrBuilder);
			SymbolicSuffix v = new SymbolicSuffix(ce.prefix(i-1), ce.suffix(ce.length() - i + 1), restrBuilder);

			Optional<Expression<Boolean>> gOpt = getHypGuard(run, i);
			assert gOpt.isPresent() : "No guard satisfying valuation at index " + i;
			Expression<Boolean> gHyp = gOpt.get();

			for (ShortPrefix u : hyp.getLeaf(loc).getShortPrefixes()) {
				SDT sdt = sulOracle.treeQuery(u, v);
				Set<DataValue> uVals = hyp.getLeaf(loc).getPrefix(u).getRegisters();
				Mapping<DataValue, DataValue> muRenaming = valuationRenaming(u, mu);
				Set<Mapping<DataValue, DataValue>> renamings = extendedValuationRenamings(sdt, uVals, run, i);
				Branching branching = sulOracle.getInitialBranching(u, action, sdt);
				for (Expression<Boolean> gSul : branching.guardSet()) {
					for (Mapping<DataValue, DataValue> renaming : renamings) {
						renaming.putAll(muRenaming);
						if (isGuardSatisfied(gSul, renaming, symbol)) {
							Optional<Result> res = checkTransition(locNext, u, action, vNext, gHyp, gSul);
							if (res.isEmpty()) {
								res = checkLocation(locNext, u, action, vNext);
							}
							if (res.isPresent()) {
								return res.get();
							}
						}
					}
				}
			}
		}

		throw new IllegalStateException("Found no counterexample in " + ce);
	}

	/*
	 * Generate a mapping from the register valuation of u on the hypothesis to val
	 */
	private Mapping<DataValue, DataValue> valuationRenaming(Word<PSymbolInstance> u, RegisterValuation val) {
		RARun uRun = hyp.getRun(u);
		RegisterValuation uVal = uRun.getValuation(u.size());
		Mapping<DataValue, DataValue> ret = new Mapping<>();
		for (Map.Entry<Register, DataValue> e : uVal.entrySet()) {
			DataValue replace = e.getValue();
			DataValue by = val.get(e.getKey());
			if (by == null) {
				by = replace;
			}
			ret.put(replace, by);
		}
		return ret;
	}

	private Set<Mapping<DataValue, DataValue>> extendedValuationRenamings(SDT uSDT, Set<DataValue> uValuation, RARun run, int id) {
		Set<Mapping<DataValue, DataValue>> identity = new LinkedHashSet<>();
		identity.add(new Mapping<>());
		if (id <= 1) {
			return identity;
		}
		Set<DataValue> sdtVals = new LinkedHashSet<>(uSDT.getDataValues());
		for (DataValue d : uValuation) {
			sdtVals.remove(d);
		}
		if (sdtVals.isEmpty()) {
			return identity;
		}
		DataValue[] sdtValsArr = sdtVals.toArray(new DataValue[sdtVals.size()]);

		ArrayList<DataValue> runVals = new ArrayList<>();
		for (int i = 1; i <= id-1; i++) {
			for (DataValue d : run.getTransition(i).getParameterValues()) {
				runVals.add(d);
			}
		}
		for (DataValue d : run.getValuation(id-1).values()) {
			runVals = removeFirst(runVals, d);
		}

		Set<Mapping<DataValue, DataValue>> renamings = new LinkedHashSet<>();
		PermutationIterator permit = new PermutationIterator(runVals.size());
		for (int[] order : permit) {
			Mapping<DataValue, DataValue> remapping = new Mapping<>();
			boolean valid = true;
			for (int i = 0; i < sdtValsArr.length; i++) {
				DataValue sdtVal = sdtValsArr[i];
				DataValue runVal = runVals.get(order[i]);
				if (!sdtVal.getDataType().equals(runVal.getDataType())) {
					valid = false;
					break;
				}
				remapping.put(sdtVal, runVal);
			}
			if (valid) {
				renamings.add(remapping);
			}
		}

		return renamings;
	}

	private ArrayList<DataValue> removeFirst(ArrayList<DataValue> list, DataValue d) {
		ArrayList<DataValue> ret = new ArrayList<>();
		ret.addAll(list);
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(d)) {
				ret.remove(i);
				break;
			}
		}
		return ret;
	}

	private Optional<Result> checkTransition(RALocation loc_i,
			ShortPrefix u,
			ParameterizedSymbol action,
			SymbolicSuffix vi,
			Expression<Boolean> gi,
			Expression<Boolean> giPrime) {
        ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
        Expression<Boolean> giRenamed = rvv.apply(gi, u.getRpBijection().inverse().toVarMapping());
        Expression<Boolean> con = ExpressionUtil.and(giRenamed, giPrime);

        DataType[] types = action.getPtypes();
        DataValue[] reprDataVals = new DataValue[types.length];
        Set<DataValue> prior = new LinkedHashSet<>();
        for (int i = 0; i < types.length; i++) {
        	reprDataVals[i] = teachers.get(types[i]).instantiate(u, action, prior, consts, con, i+1, solver);
        	prior.add(reprDataVals[i]);
        }
        PSymbolInstance psi = new PSymbolInstance(action, reprDataVals);
        Word<PSymbolInstance> uExtSul = u.append(psi);

		CTLeaf leaf_i = hyp.getLeaf(loc_i);
		Iterator<Word<PSymbolInstance>> extensions = ct.getExtensions(u, action)
				.stream()
				.filter(w -> leaf_i.getPrefixes().contains(w))
				.iterator();
		while (extensions.hasNext()) {
			Word<PSymbolInstance> uExtHyp = extensions.next();    // u_{i-1}\alpha_i(d_i')
			SDT uExtHypSDT = sulOracle.treeQuery(uExtHyp, vi).toRegisterSDT(uExtHyp, consts);
			SDT uExtSulSDT = sulOracle.treeQuery(uExtSul, vi).toRegisterSDT(uExtSul, consts);

			if (SDT.equivalentUnderId(uExtHypSDT, uExtSulSDT)) {
				return Optional.empty();
			}
		}

		Result res = new Result(uExtSul, ResultType.TRANSITION);
		return Optional.of(res);
	}

	private Optional<Result> checkLocation(RALocation loc_i,
			Word<PSymbolInstance> u,
			ParameterizedSymbol action,
			SymbolicSuffix vi) {
		CTLeaf leaf_i = hyp.getLeaf(loc_i);
		Iterator<Prefix> extensions = ct.getExtensions(u, action)
				.stream()
				.filter(w -> leaf_i.getPrefixes().contains(w))
				.map(w -> leaf_i.getPrefix(w))
				.iterator();
		while (extensions.hasNext()) {
			Prefix uExt = extensions.next();
			Bijection<DataValue> uExtBi = uExt.getRpBijection();
			boolean noEquivUi = true;
			for (Prefix ui : leaf_i.getShortPrefixes()) {
				Bijection<DataValue> uiBi = ui.getRpBijection();
				Bijection<DataValue> gamma = uiBi.compose(uExtBi.inverse());
				SDT uExtSDT = sulOracle.treeQuery(uExt, vi);
				SDT uiSDT = sulOracle.treeQuery(ui, vi);
				if (SDT.equivalentUnderBijection(uiSDT, uExtSDT, gamma) != null) {
					noEquivUi = false;
					break;
				}
			}
			if (noEquivUi) {
				Result res = new Result(uExt, ResultType.LOCATION);
				return Optional.of(res);
			}
		}

		return Optional.empty();
	}

	private Optional<Expression<Boolean>> getHypGuard(RARun run, int idx) {
		RALocation locNext = run.getLocation(idx);
		RALocation loc = run.getLocation(idx - 1);
		CTLeaf leafNext = hyp.getLeaf(locNext);
		RegisterValuation mu = run.getValuation(idx-1);
		PSymbolInstance a = run.getTransition(idx);
		ShortPrefix sp = hyp.getLeaf(loc).getShortPrefixes().iterator().next();
		Mapping<DataValue, DataValue> renaming = valuationRenaming(sp, mu);
		for (Word<PSymbolInstance> ua : ct.getExtensions(sp, a.getBaseSymbol())) {
			if (leafNext.getPrefixes().contains(ua)) {
				for (Expression<Boolean> g : sp.getBranching(a.getBaseSymbol()).getBranches().values()) {
					if (isGuardSatisfied(g, renaming, a)) {
						return Optional.of(g);
					}
				}
			}
		}
		return Optional.empty();
	}

	private boolean isGuardSatisfied(Expression<Boolean> guard, Mapping<DataValue, DataValue> renaming, PSymbolInstance symbol) {
		Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
		DataValue[] vals = symbol.getParameterValues();
		ParameterGenerator pgen = new ParameterGenerator();

        ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
        Expression<Boolean> guardRenamed = rvv.apply(guard, renaming);

		for (int i = 0; i < vals.length; i++) {
			Parameter p = pgen.next(vals[i].getDataType());
			mapping.put(p, vals[i]);
		}
		mapping.putAll(consts);

		return solver.isSatisfiable(guardRenamed, mapping);
	}
}
