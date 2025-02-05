package de.learnlib.ralib.ceanalysis;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.*;
import de.learnlib.ralib.learning.rastar.CEAnalysisResult;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class PrefixFinder {

    private final TreeOracle sulOracle;

    private TreeOracle hypOracle;

    private Hypothesis hypothesis;

	private final Map<DataType, Theory> teachers;

    private final SDTLogicOracle sdtOracle;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;
    //private Map<Word<PSymbolInstance>, LocationComponent> components;

    private final Constants consts;

    private SymbolicWord[] candidates;

    private final Map<SymbolicWord, TreeQueryResult> candidateCEs = new LinkedHashMap<SymbolicWord, TreeQueryResult>();
    private final Map<SymbolicWord, TreeQueryResult> storedQueries = new LinkedHashMap<SymbolicWord, TreeQueryResult>();

    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixFinder.class);

    public PrefixFinder(TreeOracle sulOracle, TreeOracle hypOracle,
            Hypothesis hypothesis, SDTLogicOracle sdtOracle,
            Map<DataType, Theory> teachers,
            Constants consts) {

        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.consts = consts;
        this.restrictionBuilder = sulOracle.getRestrictionBuilder();
        this.teachers = teachers;
    }

    public CEAnalysisResult analyzeCounterexample(Word<PSymbolInstance> ce) {
		int idx = findIndex(ce);
        SymbolicWord sw = new SymbolicWord(candidates[idx].getPrefix(), candidates[idx].getSuffix());
        TreeQueryResult tqr = null; //storedQueries.get(sw);
        if (tqr == null) {
        	// THIS CAN (possibly) BE DONE WITHOUT A NEW TREE QUERY
        	tqr = sulOracle.treeQuery(sw.getPrefix(), sw.getSuffix());
        }
        CEAnalysisResult result = new CEAnalysisResult(candidates[idx].getPrefix(),
        		                                       candidates[idx].getSuffix(),
        		                                       tqr);

        candidateCEs.put(candidates[idx], tqr);
        storeCandidateCEs(ce, idx);

        return result;
    }

	private int findIndex(Word<PSymbolInstance> ce) {
		candidates = new SymbolicWord[ce.length()];
		int max = ce.length() - 1;
		for (int idx=max; idx>=0; idx = idx-1) {

			Word<PSymbolInstance> prefix = ce.prefix(idx);
			Word<PSymbolInstance> nextPrefix = ce.prefix(idx+1);

			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} ce:     {}", idx, ce);
			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} prefix: {}", idx, prefix);
			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} next:   {}", idx, nextPrefix);

			// check for location counterexample ...
			//
			Word<PSymbolInstance> suffix = ce.suffix(ce.length() - nextPrefix.length());
			Word<PSymbolInstance> extendedSuffix = ce.suffix(ce.length() - prefix.length());
			SymbolicSuffix symSuffix = new SymbolicSuffix(nextPrefix, suffix, restrictionBuilder);
			SymbolicSuffix extendedSymSuffix = new SymbolicSuffix(prefix, extendedSuffix, restrictionBuilder);

			for (Word<PSymbolInstance> u : hypothesis.possibleAccessSequences(prefix)) {
				Word<PSymbolInstance> uAlpha = hypothesis.transformTransitionSequence(nextPrefix, u);
				TreeQueryResult uAlphaResult = sulOracle.treeQuery(uAlpha, symSuffix);
				storedQueries.put(new SymbolicWord(uAlpha, symSuffix), uAlphaResult);

				// check if the word is inequivalent to all access sequences
				//
				boolean inequivalent = true;
				for (Word<PSymbolInstance> uPrime : hypothesis.possibleAccessSequences(nextPrefix)) {
					TreeQueryResult uPrimeResult = sulOracle.treeQuery(uPrime, symSuffix);
					storedQueries.put(new SymbolicWord(uPrime, symSuffix), uPrimeResult);

					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} u:  {}", idx, u);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} ua: {}", idx, uAlpha);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} u': {}", idx, uPrime);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} v:  {}", idx, symSuffix);

					// different piv sizes
					//
					if (!uPrimeResult.getPiv().typedSize().equals(uAlphaResult.getPiv().typedSize())) {
						continue;
					}

					// remapping
					//
					PIVRemappingIterator iterator = new PIVRemappingIterator(
							uAlphaResult.getPiv(), uPrimeResult.getPiv());

					for (VarMapping m : iterator) {
						if (uAlphaResult.getSdt().isEquivalent(uPrimeResult.getSdt(), m)) {
							inequivalent = false;
							break;
						}
					}
				}

				if (inequivalent) {
					candidates[idx] = new SymbolicWord(uAlpha, symSuffix);
					LOGGER.trace(Category.COUNTEREXAMPLE, "Counterexample for location");
					return idx;
				}

				// check for transition counterexample
				//
				ParameterizedSymbol alphaSymbol = extendedSuffix.firstSymbol().getBaseSymbol();
				int alphaLen = alphaSymbol.getArity();
				if (alphaLen < 1) {
					continue;
				}

				// compute g'
				TreeQueryResult uResult = sulOracle.treeQuery(u, extendedSymSuffix);
				storedQueries.put(new SymbolicWord(u, extendedSymSuffix), uResult);
				SDTGuard[] guards = findGuard(uResult.getSdt(), uResult.getPiv(), u, extendedSymSuffix);
				if (guards == null) {
					continue;
				}

				GuardExpression[] guardExpressions = new GuardExpression[alphaLen];
				DataValue<?>[] dataValues = new DataValue<?>[alphaLen];
				Map<Integer, DataValue<?>> prior = new LinkedHashMap<>();
				for (int i = 0; i < alphaLen; i++) {
					guardExpressions[i] = guards[i].toExpr();
					DataValue<?> rdv = representativeDataValue(guards[i], u, alphaSymbol, uResult.getPiv(), prior);
					dataValues[i] = representativeDataValue(guards[i], u, alphaSymbol, uResult.getPiv(), prior);
					prior.put(i, dataValues[i]);
				}
				GuardExpression gPrime = alphaLen > 1 ? new Conjunction(guardExpressions) : guardExpressions[0];
				PSymbolInstance alpha = new PSymbolInstance(alphaSymbol, dataValues);
				Word<PSymbolInstance> uAlphaGPrime = u.append(alpha);

				ParValuation uPars = DataWords.computeParValuation(u);
				VarValuation uVars = DataWords.computeVarValuation(uPars, uResult.getPiv());

				for (Word<PSymbolInstance> ua : hypothesis.getAlphaTransitions(uAlpha)) {
					// check sdts not equivalent under identity mapping
					TreeQueryResult uaResult = sulOracle.treeQuery(ua, symSuffix);
					TreeQueryResult uAlphaGPrimeResult = sulOracle.treeQuery(uAlphaGPrime, symSuffix);
					storedQueries.put(new SymbolicWord(ua, symSuffix), uaResult);

					boolean pivEqual = uaResult.getPiv().typedSize().equals(uAlphaGPrimeResult.getPiv().typedSize());
					boolean equivSdts = pivEqual;
					if (pivEqual) {
						equivSdts = uAlphaGPrimeResult.getSdt().isEquivalentUnderId(uaResult.getSdt(), uAlphaGPrimeResult.getPiv(), uaResult.getPiv());
					}
					if (!equivSdts) {
						// check guard of ua does not satisfy g'
						Mapping<SymbolicDataValue, DataValue<?>> uaVars = new Mapping<>();
						uaVars.putAll(uVars);
						uaVars.putAll(consts);
						DataValue<?>[] aDvs = ua.lastSymbol().getParameterValues();
						for (int i = 0; i < alphaLen; i++) {
							int id = i + 1;
							SuffixValue sv = extendedSymSuffix.getSuffixValue(id);
							uaVars.put(sv, aDvs[i]);
						}

						if (!gPrime.isSatisfied(uaVars)) {
							// compute conjunction of g and g', and add prefix u.alpha(d_u^{g and g'})
							TreeQueryResult hypResult = hypOracle.treeQuery(u, extendedSymSuffix);
							Word<PSymbolInstance> candidate = uAlphaGAndGPrime(guards, uResult.getPiv(), hypResult, u, prefix, nextPrefix);
							candidates[idx] = new SymbolicWord(candidate, symSuffix);
							LOGGER.trace(Category.COUNTEREXAMPLE, "Counterexample for transition");
							return idx;
						}
					}
				}
			}
		}
		throw new RuntimeException("should not reach here");
	}

	private SDTGuard[] findGuard(SymbolicDecisionTree uSDT, PIV uPIV,Word<PSymbolInstance> u, SymbolicSuffix symSuffix) {
		ParValuation uPars = DataWords.computeParValuation(u);
		VarValuation uVars = DataWords.computeVarValuation(uPars, uPIV);

		TreeQueryResult uHypRes = hypOracle.treeQuery(u, symSuffix);

    	GuardExpression expr = sdtOracle.getCEGuard(u, uSDT, uPIV, uHypRes.getSdt(), uHypRes.getPiv());
    	if (expr == null) {
    		return null;
    	}

        Map<Word<PSymbolInstance>, Boolean> sulPaths = sulOracle.instantiate(u, symSuffix, uSDT, uPIV);
        for (Word<PSymbolInstance> path : sulPaths.keySet()) {
        	ParameterGenerator parGen = new ParameterGenerator();
        	for (PSymbolInstance psi : u) {
        		for (DataType dt : psi.getBaseSymbol().getPtypes())
        			parGen.next(dt);
        	}

        	VarMapping<SuffixValue, Parameter> renaming = new VarMapping<>();
        	SuffixValueGenerator svGen = new SuffixValueGenerator();
        	for (ParameterizedSymbol ps : symSuffix.getActions()) {
        		for (DataType dt : ps.getPtypes()) {
        			Parameter p = parGen.next(dt);
        			SuffixValue sv = svGen.next(dt);
        			renaming.put(sv, p);
        		}
        	}
        	GuardExpression exprR = expr.relabel(renaming);

        	ParValuation pars = new ParValuation(path);
        	Mapping<SymbolicDataValue, DataValue<?>> vals = new Mapping<>();
        	vals.putAll(DataWords.computeVarValuation(pars, uPIV));
        	vals.putAll(pars);
        	vals.putAll(consts);

        	if (exprR.isSatisfied(vals)) {
        		PSymbolInstance alpha = path.suffix(path.length() - u.length()).firstSymbol();
        		DataValue<?>[] dvs = alpha.getParameterValues();
        		SDTGuard[] guards = new SDTGuard[dvs.length];
        		SuffixValueGenerator svgen = new SuffixValueGenerator();
        		Mapping<SymbolicDataValue, DataValue<?>> valuation = new Mapping<>();
				valuation.putAll(uVars);
				valuation.putAll(consts);
        		for (int i = 0; i < dvs.length; i++) {
        			SuffixValue sv = svgen.next(dvs[i].getType());
    				valuation.put(sv, dvs[i]);
        			for (SDTGuard g : uSDT.getSDTGuards(sv)) {
        				GuardExpression gx = g.toExpr();
        				if (gx.isSatisfied(valuation)) {
        					guards[i] = g;
        					break;
        				}
        			}
        		}
        		return guards;
        	}
        }

		return null;
	}

	private DataValue<?> representativeDataValue(SDTGuard guard, Word<PSymbolInstance> u, ParameterizedSymbol alpha, PIV uPiv, Map<Integer, DataValue<?>> prior) {
		DataType type = guard.getParameter().getType();
		Parameter suffixValueParam = new Parameter(type, guard.getParameter().getId());
		Theory teach = teachers.get(type);
		ParValuation pars = new ParValuation();
		ParameterGenerator pgen = new ParameterGenerator();
		for (PSymbolInstance psi : u) {
			DataValue<?>[] vals = psi.getParameterValues();
			DataType[] types = psi.getBaseSymbol().getPtypes();
			for (int i = 0; i < vals.length; i++) {
				Parameter p = pgen.next(types[i]);
				pars.put(p, vals[i]);
			}
		}
		int svId = suffixValueParam.getId() - 1;
		for (int i = 0; i < svId; i++) {
			DataType t = alpha.getPtypes()[i];
			Parameter p = pgen.next(t);
			DataValue<?> dv = prior.get(i);
			pars.put(p, dv);
		}

		return teach.instantiate(u, alpha, uPiv, pars, consts, guard, suffixValueParam, new LinkedHashSet<>());
	}

	// TODO: come up with a better name for this function
	private Word<PSymbolInstance> uAlphaGAndGPrime(SDTGuard[] gPrimeGuards, PIV gPrimePiv, TreeQueryResult hypResult, Word<PSymbolInstance> u, Word<PSymbolInstance> prefix, Word<PSymbolInstance> nextPrefix) {
		VarMapping<Register, Register> renaming = new VarMapping<>();
		PIV unifiedPiv = new PIV();
		unifiedPiv.putAll(hypResult.getPiv());
		int freshId = unifiedPiv.size() + 1;
		for (Map.Entry<Parameter, Register> gPrimeEntry : gPrimePiv.entrySet()) {
			Parameter p = gPrimeEntry.getKey();
			Register r = gPrimeEntry.getValue();
			Register otherR = unifiedPiv.get(p);
			if (otherR == null) {
				otherR = new Register(p.getType(), freshId);
				freshId++;
				unifiedPiv.put(p, otherR);
			}
			renaming.put(r, otherR);
		}

		SuffixValueGenerator svgen = new SuffixValueGenerator();
		PSymbolInstance action = nextPrefix.lastSymbol();
		DataValue<?>[] actionValues = action.getParameterValues();
		ParValuation prefixPars = DataWords.computeParValuation(prefix);
		VarValuation vars = new VarValuation();
		for (Map.Entry<Parameter, Register> entry : hypResult.getPiv().entrySet()) {
			Parameter p = entry.getKey();
			Register r = entry.getValue();
			DataValue<?> dv = prefixPars.get(p);
			vars.put(r, dv);
		}
		Mapping<SymbolicDataValue, DataValue<?>> mapping = new Mapping<>();
		mapping.putAll(prefixPars);
		mapping.putAll(vars);
		mapping.putAll(consts);
		for (DataValue<?> av : actionValues) {
			SuffixValue sv = svgen.next(av.getType());
			mapping.put(sv, av);
		}

		ParameterizedSymbol alpha = action.getBaseSymbol();
		DataType[] types = alpha.getPtypes();
		DataValue<?>[] dataValues = new DataValue<?>[alpha.getArity()];
		svgen = new SuffixValueGenerator();
		Map<Integer, DataValue<?>> prior = new LinkedHashMap<>();
		for (int i = 0; i < alpha.getArity(); i++) {
			SuffixValue sv = svgen.next(types[i]);
			SDTGuard gAndGPrime = null;
			for (SDTGuard g : hypResult.getSdt().getSDTGuards(sv)) {
				if (g.toExpr().isSatisfied(mapping)) {
					gAndGPrime = new SDTAndGuard(sv, g, gPrimeGuards[i].relabel(renaming));
					break;
				}
			}
			Parameter p = new Parameter(sv.getType(), sv.getId());
			dataValues[i] = representativeDataValue(gAndGPrime, u, alpha, unifiedPiv, prior);
			prior.put(i, dataValues[i]);
		}

		PSymbolInstance psi = new PSymbolInstance(alpha, dataValues);
		return u.append(psi);
	}

	private void storeCandidateCEs(Word<PSymbolInstance> ce, int idx) {
    	if (idx+1 >= ce.length())
    		return;
    	Word<PSymbolInstance> prefix = ce.prefix(idx+1);

    	Word<PSymbolInstance> suffix = ce.suffix(ce.length() - (idx+1));
    	SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, restrictionBuilder);

    	Set<Word<PSymbolInstance>> locations = hypothesis.possibleAccessSequences(prefix);
    	for (Word<PSymbolInstance> location : locations) {
    		SymbolicWord symWord = new SymbolicWord(location, symSuffix);
    		TreeQueryResult tqr = storedQueries.get(symWord);

    		assert tqr != null;

    		candidateCEs.put(symWord, tqr);
    	}
    }

    public Set<DefaultQuery<PSymbolInstance, Boolean>> getCounterExamples() {
    	Set<DefaultQuery<PSymbolInstance, Boolean>> ces = new LinkedHashSet<DefaultQuery<PSymbolInstance, Boolean>>();
    	for (Map.Entry<SymbolicWord, TreeQueryResult> e : candidateCEs.entrySet()) {
    		SymbolicWord sw = e.getKey();
    		TreeQueryResult tqr = e.getValue();
    		Map<Word<PSymbolInstance>, Boolean> cemaps = sulOracle.instantiate(sw.getPrefix(), sw.getSuffix(), tqr.getSdt(), tqr.getPiv());
    		for (Map.Entry<Word<PSymbolInstance>, Boolean> c : cemaps.entrySet()) {
    			ces.add(new DefaultQuery<PSymbolInstance, Boolean>(c.getKey(), c.getValue()));
    		}
    	}

    	return ces;
    }

    public void setHypothesisTreeOracle(TreeOracle hypOracle) {
        this.hypOracle = hypOracle;
    }

    public void setHypothesis(Hypothesis hyp) {
    	this.hypothesis = hyp;
    }
}
