package de.learnlib.ralib.ceanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RARun;
import de.learnlib.ralib.automata.xml.RegisterAutomaton.Constants.Constant;
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
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PermutationIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.ReplacingValuesVisitor;
import de.learnlib.ralib.smt.ReplacingVarsVisitor;
import de.learnlib.ralib.smt.VarsValuationVisitor;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 * Analyzes counterexamples according to the SLλ algorithm.
 *
 * @author fredrik
 */
public class PrefixFinder {

	public enum ResultType {
		TRANSITION,
		LOCATION
	}

	/**
	 * Container for the result of a counterexample analysis
	 */
	public record Result(Word<PSymbolInstance> prefix, ResultType result) {};

	private final CTHypothesis hyp;
	private final ClassificationTree ct;

	private final TreeOracle sulOracle;
	private final Map<DataType, Theory> teachers;

	private final SLLambdaRestrictionBuilder restrBuilder;

	private final ConstraintSolver solver;

	private final Constants consts;

	public PrefixFinder(TreeOracle sulOracle, CTHypothesis hyp, ClassificationTree ct,
			Map<DataType, Theory> teachers, SLLambdaRestrictionBuilder restrBuilder,
			ConstraintSolver solver, Constants consts) {
		this.hyp = hyp;
		this.ct = ct;
		this.sulOracle = sulOracle;
		this.teachers = teachers;
		this.restrBuilder = restrBuilder;
		this.solver = solver;
		this.consts = consts;
	}

	/**
	 * Analyze counterexample {@code ce} from right to leaf to find a transition or
	 * location discrepancy. If a discrepancy is found, returns the prefix which reveals
	 * the discrepancy, along with a {@code ResultType} indicating the type of discrepancy.
	 *
	 * @param ce
	 * @return
	 */
	public Result analyzeCounterExample(Word<PSymbolInstance> ce) {
		RARun run = hyp.getRun(ce);
		for (int i = ce.length(); i >= 1; i--) {
			RALocation loc = run.getLocation(i-1);
			RALocation locNext = run.getLocation(i);
			PSymbolInstance symbol = run.getTransitionSymbol(i);
			RegisterValuation runValuation = run.getValuation(i-1);
			ParameterizedSymbol action = symbol.getBaseSymbol();

//			SymbolicSuffix vNext = new SymbolicSuffix(ce.prefix(i), ce.suffix(ce.length() - i), restrBuilder);
//			SymbolicSuffix v = new SymbolicSuffix(ce.prefix(i-1), ce.suffix(ce.length() - i + 1), restrBuilder);
//			SymbolicSuffix vNext = restrBuilder.constructRestrictedSuffix(run, i);
//			SymbolicSuffix v = restrBuilder.constructRestrictedSuffix(run, i-1);

			Expression<Boolean> gHyp = run.getGuard(i, consts);

			for (ShortPrefix u : hyp.getLeaf(loc).getShortPrefixes()) {
				RegisterValuation uValuation = hyp.getRun(u).getValuation(u.size());
				SymbolicSuffix v = restrBuilder.constructRestrictedSuffix(run.getPrefix(i-1), run.getSuffix(i-1), u, runValuation, uValuation);
				SymbolicSuffix uv = restrBuilder.concretize(v,
						hyp.getRun(u).getValuation(u.size()),
						ParameterValuation.fromPSymbolWord(u),
						consts);
				SDT sdt = sulOracle.treeQuery(u, uv);

				Set<DataValue> uVals = hyp.getLeaf(loc).getPrefix(u).getRegisters();
				Mapping<DataValue, DataValue> uToRunRenaming = valuationRenaming(u, runValuation);
				Set<Mapping<DataValue, DataValue>> uToRunExtendedRenamings = extendedValuationRenamings(sdt, uVals, run, i);

				Branching branching = sulOracle.getInitialBranching(u, action, sdt);
				for (Expression<Boolean> gSul : branching.guardSet()) {
					for (Mapping<DataValue, DataValue> renaming : uToRunExtendedRenamings) {
						renaming.putAll(uToRunRenaming);
						gSul = conjunctionWithRestriction(gSul, uv, u, hyp.getRun(u).getValuation(u.size()).keySet(), consts);
						if (isGuardSatisfied(gSul, renaming, symbol)) {
							Optional<Result> res = checkTransition(run, i, u, action, gHyp, gSul);
							if (res.isEmpty()) {
								res = checkLocation(run, i, u, action);
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
	
	private Expression<Boolean> conjunctionWithRestriction(Expression<Boolean> guard, SymbolicSuffix suffix, Word<PSymbolInstance> u, Set<Register> regs, Constants consts) {
		DataType[] types = suffix.getActions().firstSymbol().getPtypes();
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
		
		Expression[] restrictionExpressions = new Expression[types.length + 1];
		for (int i = 0; i < types.length; i++) {
			SuffixValue s = sgen.next(types[i]);
			Parameter p = new Parameter(s.getDataType(), s.getId());
			AbstractSuffixValueRestriction r = suffix.getRestriction(s);
			Expression<Boolean> expr = r.toGuardExpression(vals);
			
			VarsValuationVisitor vvv = new VarsValuationVisitor();
			expr = vvv.apply(expr, pmap);
			
			ReplacingVarsVisitor rvv = new ReplacingVarsVisitor();
			VarMapping<SuffixValue, Parameter> mapping = new VarMapping<>();
			mapping.put(s, p);
			Expression<Boolean> renamedExpr = rvv.apply(expr, mapping);
			restrictionExpressions[i] = renamedExpr;
		}
		restrictionExpressions[restrictionExpressions.length - 1] = guard;
		Expression<Boolean> con = ExpressionUtil.and(restrictionExpressions);
		return con;
	}

	/**
	 * @param u
	 * @param val
	 * @return a mapping from the register valuation of {@code u} on the hypothesis to the {@code val}
	 */
	private Mapping<DataValue, DataValue> valuationRenaming(Word<PSymbolInstance> u, RegisterValuation val) {
		// get register mapping for u when run over the hypothesis
		RARun uRun = hyp.getRun(u);
		RegisterValuation uVal = uRun.getValuation(u.size());
		// create mapping from u's valuation to val
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

	/**
	 * Create extensions of the valuation from the hypothesis at index {@code id}, and map
	 * data values from {@code uSDT} to these extended valuations.
	 * The returned remappings do not include parameters present in the valuation at {@code id}
	 * (which should be mapped to parameters in {@code uVals}, except for duplicate parameters.
	 * For example, if the parameters of {@code run} at index {@code id} contain the data values
	 * 5,5,7 and the valuation contains 5, then the 7 and a single 5 will be considered for the
	 * extension of the valuation. Similarly, if {@code uSDT} has data values 0,1,2 with 0 in
	 * {@code uValuation}, then 1,2 will be considered. In this example, this method would
	 * return the mappings {1->5, 2->7} and {1->7, 2->5}.
	 *
	 * @param uSDT sdt for prefix {@code u}
	 * @param uVals memorable data values of {@code u}
	 * @param run
	 * @param id
	 * @return
	 */
	private Set<Mapping<DataValue, DataValue>> extendedValuationRenamings(SDT uSDT, Set<DataValue> uVals, RARun run, int id) {
		Set<Mapping<DataValue, DataValue>> empty = new LinkedHashSet<>();
		empty.add(new Mapping<>());
		if (id < 1) {
			return empty;
		}

		// gather data values from uSDT, and remove values from uValuation
		Set<DataValue> sdtVals = new LinkedHashSet<>(uSDT.getDataValues());
		for (DataValue d : uVals) {
			sdtVals.remove(d);
		}
		if (sdtVals.isEmpty()) {
			return empty;
		}
		DataValue[] sdtValsArr = sdtVals.toArray(new DataValue[sdtVals.size()]);

		// gather data values from prefix of run at index id
		ArrayList<DataValue> runVals = new ArrayList<>();
		for (int i = 1; i <= id-1; i++) {
			for (DataValue d : run.getTransitionSymbol(i).getParameterValues()) {
				runVals.add(d);
			}
		}

		/* remove data values from valuation.
		 * may have multiple copies of same data value, which may be mapped to different
		 * data values in uSDT, so only remove one instance of data values in valuation
		 */
		for (DataValue d : run.getValuation(id-1).values()) {
			runVals = removeFirst(runVals, d);
		}

		// compute all possible permutations of mappings between extended uSDT values and run values
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

	/**
	 * @param list
	 * @param d
	 * @return array containing data values of {@code list}, with one occurrence of {@code d} removed
	 */
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

	/**
	 * Check for a transition discrepancy. This is done by checking whether there exists no
	 * {@code action}-extension of {@code u} in the leaf of {@code loc} that is equivalent
	 * to the {@code (hypGuard && sulGuard)} extension of {@code u} after {@code v}.
	 *
	 * @param loc the source location
	 * @param u short prefix from leaf of {@code loc}
	 * @param action the symbol of the next transition
	 * @param v the suffix after {@code u} and {@code action}
	 * @param hypGuard guard of {@code action} after {@code u} on the hypothesis
	 * @param sulGuard guard of {@code action} after {@code u} on the SUL
	 * @return an {@code Optional} containing the result if there is a transition discrepancy, or an empty {@code Optional} otherwise
	 */
//	private Optional<Result> checkTransition(RALocation loc,
//			ShortPrefix u,
//			ParameterizedSymbol action,
//			SymbolicSuffix v,
//			Expression<Boolean> hypGuard,
//			Expression<Boolean> sulGuard) {
	private Optional<Result> checkTransition(RARun run,
			int id,
			ShortPrefix u,
			ParameterizedSymbol action,
			Expression<Boolean> hypGuard,
			Expression<Boolean> sulGuard) {
		RALocation loc = run.getLocation(id);
		// rename hyp guard to match RP
        ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
        Expression<Boolean> hypGuardRenamed = rvv.apply(hypGuard, u.getRpBijection().inverse().toVarMapping());
        Expression<Boolean> conjunction = ExpressionUtil.and(hypGuardRenamed, sulGuard);

        // instantiate a representative data value for the conjunction
        DataType[] types = action.getPtypes();
        DataValue[] reprDataVals = new DataValue[types.length];
        for (int i = 0; i < types.length; i++) {
        	Optional<DataValue> reprDataVal = teachers.get(types[i]).instantiate(u, action, conjunction, i+1, consts, solver);
        	if (reprDataVal.isEmpty()) {
        		// guard unsat
        		return Optional.empty();
        	}
        	reprDataVals[i] = reprDataVal.get();
        }
        PSymbolInstance psi = new PSymbolInstance(action, reprDataVals);
        Word<PSymbolInstance> uExtSul = u.append(psi);
//        SymbolicSuffix vSul = restrBuilder.concretize(v,
//        		hyp.getRun(u).getValuation(u.size()),
//        		ParameterValuation.fromPSymbolWord(uExtSul),
//        		consts);

        // check whether leaf of loc contains an extension of u that is equivalent to uExtSul after v
		CTLeaf leaf = hyp.getLeaf(loc);
		Iterator<Word<PSymbolInstance>> extensions = ct.getExtensions(u, action)
				.stream()
				.filter(w -> leaf.getPrefixes().contains(w))
				.iterator();
		while (extensions.hasNext()) {
			Word<PSymbolInstance> uExtHyp = extensions.next();
	        SymbolicSuffix v = restrBuilder.constructRestrictedSuffix(run.getPrefix(id),
	        		run.getSuffix(id),
	        		uExtSul,
	        		uExtHyp,
	        		run.getValuation(id),
	        		hyp.getRun(uExtSul).getValuation(uExtSul.size()),
	        		hyp.getRun(uExtHyp).getValuation(uExtHyp.size()));
//			SymbolicSuffix v = restrBuilder.constructRestrictedSuffix(run.getPrefix(id),
//					run.getSuffix(id),
//					uExtHyp,
//					run.getValuation(id),
//					hyp.getRun(uExtHyp).getValuation(uExtHyp.size()));
//			SymbolicSuffix vHyp = restrBuilder.constructRestrictedSuffix(run.getPrefix(id),
//					run.getSuffix(id),
//					uExtHyp,
//					run.getValuation(id),
//					hyp.getRun(uExtHyp).getValuation(uExtHyp.size()));
	        SymbolicSuffix vSul = restrBuilder.concretize(v,
	        		hyp.getRun(uExtSul).getValuation(uExtSul.size()),
	        		ParameterValuation.fromPSymbolWord(uExtSul),
	        		consts);
	        SymbolicSuffix vHyp = restrBuilder.concretize(v,
	        		hyp.getRun(uExtHyp).getValuation(uExtHyp.size()),
	        		ParameterValuation.fromPSymbolWord(uExtHyp),
	        		consts);
			SDT uExtHypSDT = sulOracle.treeQuery(uExtHyp, vSul).toRegisterSDT(uExtHyp, consts);
			SDT uExtSulSDT = sulOracle.treeQuery(uExtSul, vHyp).toRegisterSDT(uExtSul, consts);

			if (SDT.equivalentUnderId(uExtHypSDT, uExtSulSDT)) {
				return Optional.empty();  // there is an equivalent extension, so no discrepancy
			}
		}

		// no equivalent extension exists
		Result res = new Result(uExtSul, ResultType.TRANSITION);
		return Optional.of(res);
	}

	/**
	 * Check for a location discrepancy. This is done by checking whether there is some
	 * {@code action}-extension of {@code u} in the leaf of {@code locNext} such that there
	 * does not exist some short prefix in the leaf of {@code locNext} that is equivalent
	 * to the {@code action}-extension of {@code u} after {@code v}.
	 *
	 * @param locNext the destination location
	 * @param u short prefix in leaf prior to {@code locNext} in the run
	 * @param action the symbol of the next transition
	 * @param v the suffix after {@code u} and {@code action}
	 * @return an {@code Optional} containing the result if there is a location discrepancy, or an empty {@code Optional} otherwise
	 */
	private Optional<Result> checkLocation(RARun run,
			int id,
			Word<PSymbolInstance> u,
			ParameterizedSymbol action) {
		RALocation locNext = run.getLocation(id);
		CTLeaf leafNext = hyp.getLeaf(locNext);
		Iterator<Prefix> extensions = ct.getExtensions(u, action)
				.stream()
				.filter(w -> leafNext.getPrefixes().contains(w))
				.map(w -> leafNext.getPrefix(w))
				.iterator();
		while (extensions.hasNext()) {
			Prefix uExtended = extensions.next();
			Bijection<DataValue> uExtBijection = uExtended.getRpBijection();
//			SymbolicSuffix vuExt = restrBuilder.constructRestrictedSuffix(run.getPrefix(id),
//					run.getSuffix(id),
//					uExtended,
//					run.getValuation(id),hyp.getRun(uExtended).getValuation(uExtended.size()));
			boolean noEquivU = true;
			for (Prefix uNext : leafNext.getShortPrefixes()) {
				Bijection<DataValue> uNextBijection = uNext.getRpBijection();
				Bijection<DataValue> gamma = uNextBijection.compose(uExtBijection.inverse());
				SymbolicSuffix vuNext = restrBuilder.constructRestrictedSuffix(run.getPrefix(id),
						run.getSuffix(id),
						uNext,
						uExtended,
						run.getValuation(id),
						hyp.getRun(uNext).getValuation(uNext.size()),
						hyp.getRun(uExtended).getValuation(uExtended.size()));
				SymbolicSuffix vuExt = restrBuilder.concretize(vuNext,
						hyp.getRun(uExtended).getValuation(uExtended.size()),
						consts,
						ParameterValuation.fromPSymbolWord(uExtended));
				vuNext = restrBuilder.concretize(vuNext,
						hyp.getRun(uNext).getValuation(uNext.size()),
						consts,
						ParameterValuation.fromPSymbolWord(uNext));
				SDT uExtSDT = sulOracle.treeQuery(uExtended, vuExt);
				SDT uNextSDT = sulOracle.treeQuery(uNext, vuNext);
				if (SDT.equivalentUnderBijection(uNextSDT, uExtSDT, gamma) != null) {
					noEquivU = false;
					break;
				}
			}
			if (noEquivU) {
				Result res = new Result(uExtended, ResultType.LOCATION);
				return Optional.of(res);
			}
		}

		return Optional.empty();
	}

	/**
	 * Check whether {@code guard} is satisfied by the parameters of {@code symbol}, after renaming
	 * the parameters of {@code guard} according to {@code renaming}.
	 *
	 * @param guard
	 * @param renaming
	 * @param symbol
	 * @return {@code true} if {@code symbol} satisfies {@code guard}, renamed according to {@code renaming}
	 */
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
