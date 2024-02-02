package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class OptimizedSymbolicSuffixBuilder {

    private final Constants consts;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private static LearnLogger log = LearnLogger.getLogger(OptimizedSymbolicSuffixBuilder.class);

    public OptimizedSymbolicSuffixBuilder(Constants consts) {
    	this.consts = consts;
    	this.restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts);
    }

    public OptimizedSymbolicSuffixBuilder(Constants consts, SymbolicSuffixRestrictionBuilder restrictionBuilder) {
    	this.consts = consts;
    	this.restrictionBuilder = restrictionBuilder;
    }

    /**
     * Extend suffix by prepending it with the last symbol of prefix. Any suffix value in the
     * new suffix which is not compared with a constant, a parameter in the prefix (excluding
     * the last symbol), a previous free suffix value or more than one symbolic data value
     * will be set to non-free. Any non-free parameter that is equal to a single non-free
     * suffix value will be optimized for equality with that suffix value.
     *
     * @param prefix (last symbol will be prepended to suffix)
     * @param sdt
     * @param piv
     * @param suffix
     * @param registers - a list of registers that must be revealed by the suffix
     * @return a new suffix formed by prepending suffix with the last symbol of prefix
     */
    public SymbolicSuffix extendSuffix(Word<PSymbolInstance> prefix, SDT sdt, PIV piv, SymbolicSuffix suffix, Register... registers) {
    	Word<ParameterizedSymbol> suffixActions = suffix.getActions();
    	if (registers.length > 0)
    		return extendSuffixRevealingRegisters(prefix, sdt, piv, suffixActions, registers);

    	Set<List<SDTGuard>> paths = sdt.getAllPaths(new ArrayList<>()).keySet();
    	SymbolicSuffix coalesced = null;
    	for (List<SDTGuard> path : paths) {
    		SymbolicSuffix extended = extendSuffix(prefix, path, piv, suffixActions);
    		if (coalesced == null) {
    			coalesced = extended;
    		} else {
    			coalesced = coalesceSuffixes(coalesced, extended);
    		}
    	}
    	return coalesced;
    }

    public SymbolicSuffix extendSuffixRevealingRegisters(Word<PSymbolInstance> prefix, SDT sdt, PIV piv, Word<ParameterizedSymbol> suffixActions, Register[] registers) {
    	Set<List<SDTGuard>> paths = sdt.getAllPaths(new ArrayList<>()).keySet();
    	SymbolicSuffix best = null;
    	for (List<SDTGuard> path : paths) {
    		SymbolicSuffix extendedSuffix = extendSuffix(prefix, path, piv, suffixActions);
    		SymbolicSuffix s = registers.length > 0 ? null : extendedSuffix;
    		for (Register r : registers) {
    			if (extendedSuffix.getValues()
    					.stream()
    					.filter(sv -> extendedSuffix.getRestriction(sv).revealsRegister(r))
    					.findAny()
    					.isPresent()) {
    				s = extendedSuffix;
    				break;
    			}
    		}
    		best = pickBest(best, s);
    	}
    	return best;
    }

    public SymbolicSuffix extendSuffix(Word<PSymbolInstance> prefix, List<SDTGuard> sdtPath, PIV piv, Word<ParameterizedSymbol> suffixActions) {
    	Word<PSymbolInstance> sub = prefix.prefix(prefix.length()-1);
    	PSymbolInstance action = prefix.lastSymbol();
    	ParameterizedSymbol actionSymbol = action.getBaseSymbol();
    	SymbolicSuffix actionSuffix = new SymbolicSuffix(sub, prefix.suffix(1), restrictionBuilder);
    	int actionArity = actionSymbol.getArity();
    	int subArity = DataWords.paramValLength(sub);

    	Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	for (SuffixValue sv : actionSuffix.getDataValues()) {
    		restrictions.put(sv, actionSuffix.getRestriction(sv));
    	}

    	VarMapping<SymbolicDataValue, SuffixValue> renaming = new VarMapping<>();
    	for (Map.Entry<Parameter, Register> e : piv.entrySet()) {
    		Parameter p = e.getKey();
    		Register r = e.getValue();
    		if (p.getId() > subArity) {
    			SuffixValue sv = new SuffixValue(p.getType(), p.getId()-subArity);
    			renaming.put(r, sv);
    		}
    	}
    	for (SDTGuard guard : sdtPath) {
    		SuffixValue oldSV = guard.getParameter();
    		SuffixValue newSV = new SuffixValue(oldSV.getType(), oldSV.getId()+actionArity);
    		renaming.put(oldSV, newSV);
    		// for some reason, true guards ignore relabelling
    		SDTGuard renamedGuard = guard instanceof SDTTrueGuard ? new SDTTrueGuard(newSV) : guard.relabel(renaming);
    		SuffixValueRestriction restr = restrictionBuilder.restrictSuffixValue(renamedGuard, restrictions);
    		restrictions.put(newSV, restr);
    	}

    	Word<ParameterizedSymbol> actions = suffixActions.prepend(actionSymbol);
    	return new SymbolicSuffix(actions, restrictions);
    }

    private Set<SDTGuard> getGuards(List<SDTGuard> path, SuffixValue sv) {
    	Set<SDTGuard> guards = new LinkedHashSet<>();
    	for (SDTGuard g : path) {
    		if (g.getParameter().equals(sv))
    			guards.add(g);
    	}
    	return guards;
    }

    private DataType[] dataTypes(Word<ParameterizedSymbol> actions) {
    	Collection<DataType> dataTypes = new ArrayList<>();
    	for (ParameterizedSymbol ps : actions) {
    		for (DataType dt : ps.getPtypes()) {
    			dataTypes.add(dt);
    		}
    	}
    	DataType[] dts = new DataType[dataTypes.size()];
    	dts = dataTypes.toArray(dts);
    	return dts;
    }

    private Map<Register, SuffixValue> buildParameterMap(Word<PSymbolInstance> prefix, PSymbolInstance action, PIV piv) {
    	int arity = DataWords.paramValLength(prefix);
    	Map<Register, SuffixValue> actionParameters = new LinkedHashMap<>();
    	for (Map.Entry<Parameter, Register> e : piv.entrySet()) {
    		Parameter p = e.getKey();
    		if (p.getId().intValue() > arity) {
    			int actionParameterIndex = p.getId() - arity;
    			actionParameters.put(e.getValue(), new SuffixValue(p.getType(), actionParameterIndex));
    		}
    	}
    	return actionParameters;
    }

    private Parameter getParameter(Register r, PIV piv) {
    	for (Map.Entry<Parameter, Register> e : piv) {
    		if (e.getValue().equals(r))
    			return e.getKey();
    	}
    	return null;
    }

    /**
     * Provides a one-symbol extension of an (optimized) suffix for two non-empty prefixes leading to inequivalent locations,
     * based on the SDTs and associated PIVs that revealed the source of the inequivalence.
     */
    public SymbolicSuffix extendDistinguishingSuffix(Word<PSymbolInstance> prefix1, SDT sdt1, PIV piv1,
            Word<PSymbolInstance> prefix2,  SDT sdt2, PIV piv2,  SymbolicSuffix suffix) {
        assert !prefix1.isEmpty() && !prefix2.isEmpty() && prefix1.lastSymbol().getBaseSymbol().equals(prefix2.lastSymbol().getBaseSymbol());
        // prefix1 = subprefix1 + sym(d1); prefix2 = subprefix2 + sym(d2)
        // our new_suffix will be sym(s1) + suffix
        // we first determine if s1 is free (extended to all parameters in sym, if there are more)
        SymbolicSuffix suffix1 = extendSuffix(prefix1, sdt1, piv1, suffix);
        SymbolicSuffix suffix2 = extendSuffix(prefix2, sdt2, piv2, suffix);

        return coalesceSuffixes(suffix1, suffix2);
    }

    private Set<Register> actionRegisters(Word<PSymbolInstance> prefix, PSymbolInstance action, PIV piv) {
    	Set<Parameter> params = new LinkedHashSet<>();
        ParameterGenerator pGen = new ParameterGenerator();
        for (PSymbolInstance psi : prefix) {
        	for (DataType dt : psi.getBaseSymbol().getPtypes()) {
        		pGen.next(dt);
        	}
        }
        for (DataType dt : action.getBaseSymbol().getPtypes()) {
        	params.add(pGen.next(dt));
        }

        Set<Register> registers = new LinkedHashSet<>();
        piv.entrySet().stream().filter((x) -> (params.contains(x.getKey()))).forEach((x) -> { registers.add(x.getValue()); });
        return registers;
    }

    /**
     * Provides an optimized suffix to distinguish two inequivalent locations specified by prefixes,
     * based on the SDTs and associated PIVs that revealed the source of the inequivalence.
     */
    public SymbolicSuffix distinguishingSuffixFromSDTs(Word<PSymbolInstance> prefix1, SDT sdt1, PIV piv1,
            Word<PSymbolInstance> prefix2,  SDT sdt2, PIV piv2,  Word<ParameterizedSymbol> suffixActions, ConstraintSolver solver) {

        // we relabel SDTs and PIV such that they use different registers
        SymbolicDataValueGenerator.RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
        VarMapping<Register, Register> relabellingSdt1 = new VarMapping<>();
        for (Register r : piv1.values()) {
            relabellingSdt1.put(r, rgen.next(r.getType()));
        }
        SDT relSdt1 = (SDT) sdt1.relabel(relabellingSdt1);
        PIV relPiv1 = piv1.relabel(relabellingSdt1);

        VarMapping<Register, Register> relabellingSdt2 = new VarMapping<>();
        for (Register r : piv2.values()) {
            relabellingSdt2.put(r, rgen.next(r.getType()));
        }
        SDT relSdt2 = (SDT) sdt2.relabel(relabellingSdt2);
        PIV relPiv2 = piv2.relabel(relabellingSdt2);

        // we build valuations which we use to determine satisfiable paths
        Mapping<SymbolicDataValue, DataValue<?>> valuationSdt1 = buildValuation(prefix1, relPiv1, consts);
        Mapping<SymbolicDataValue, DataValue<?>> valuationSdt2 = buildValuation(prefix2, relPiv2, consts);
        Mapping<SymbolicDataValue, DataValue<?>> combined = new Mapping<>();
        combined.putAll(valuationSdt1);
        combined.putAll(valuationSdt2);
        SymbolicSuffix suffix = distinguishingSuffixFromSDTs(prefix1, relSdt1, relPiv1, prefix2, relSdt2, relPiv2, combined, suffixActions, solver);
        return suffix;
    }

    private SymbolicSuffix distinguishingSuffixFromSDTs(Word<PSymbolInstance> prefix1, SDT sdt1, PIV piv1,
    		Word<PSymbolInstance> prefix2, SDT sdt2, PIV piv2,
    		Mapping<SymbolicDataValue, DataValue<?>> valuation, Word<ParameterizedSymbol> suffixActions, ConstraintSolver solver) {
    	SymbolicSuffix best = null;
        for (boolean b : new boolean [] {true, false}) {
            // we check for paths
            List<List<SDTGuard>> pathsSdt1 = sdt1.getPaths(b);
            List<List<SDTGuard>> pathsSdt2 = sdt2.getPaths(!b);
            for (List<SDTGuard> pathSdt1 : pathsSdt1) {
                GuardExpression expr1 = toGuardExpression(pathSdt1);
                for (List<SDTGuard> pathSdt2 : pathsSdt2) {
                    GuardExpression expr2 = toGuardExpression(pathSdt2);
                    if (solver.isSatisfiable(new Conjunction(expr1, expr2), valuation)) {
                        SymbolicSuffix suffix = buildOptimizedSuffix(prefix1, pathSdt1, piv1, prefix2, pathSdt2, piv2, suffixActions);
                        best = pickBest(best, suffix);
                    }
                }
            }
        }

        return best;
    }

    private SymbolicSuffix buildOptimizedSuffix(Word<PSymbolInstance> prefix1, List<SDTGuard> pathSdt1, PIV piv1,
    		Word<PSymbolInstance> prefix2, List<SDTGuard> pathSdt2, PIV piv2,
            Word<ParameterizedSymbol> suffixActions) {
    	SymbolicSuffix suffix1 = extendSuffix(prefix1, pathSdt1, piv1, suffixActions);
    	SymbolicSuffix suffix2 = extendSuffix(prefix2, pathSdt2, piv2, suffixActions);

        return coalesceSuffixes(suffix1, suffix2);
    }

    SymbolicSuffix coalesceSuffixes(SymbolicSuffix suffix1, SymbolicSuffix suffix2) {
    	assert suffix1.getActions().equals(suffix2.getActions());

    	Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();

    	SymbolicDataValueGenerator.SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
    	for (int i=0; i<DataWords.paramLength(suffix1.getActions()); i++) {
    		DataType type = suffix1.getDataValue(i+1).getType();
    		SuffixValue sv = sgen.next(type);
    		SuffixValueRestriction restr1 = suffix1.getRestriction(sv);
    		SuffixValueRestriction restr2 = suffix2.getRestriction(sv);
    		SuffixValueRestriction restr = restr1.merge(restr2, restrictions);
    		restrictions.put(sv, restr);
    	}

    	return new SymbolicSuffix(suffix1.getActions(), restrictions);
    }

    private Set<Integer> freeSuffixIndices(SymbolicSuffix suffix) {
    	Set<Integer> freeIndices = new LinkedHashSet<>();
    	Set<SuffixValue> freeVals = suffix.getFreeValues();
    	for (int i = 0; i < DataWords.paramLength(suffix.getActions()); i++) {
    		SuffixValue sv = suffix.getDataValue(i+1);
    		if (freeVals.contains(sv))
    			freeIndices.add(i+1);
    	}
    	return freeIndices;
    }

    private SymbolicSuffix pickBest(SymbolicSuffix current, SymbolicSuffix next) {
        if (current == null) {
            return next;
        }
        if (score(next) < score(current)) {
            return next;
        }
        return current;
    }

    private int score(SymbolicSuffix suffix) {
        final int freeCost = 100000;
        final int distinctValueCost = 100;
        return suffix.getFreeValues().size() * freeCost + suffix.getValues().size() * distinctValueCost;
    }


    private GuardExpression toGuardExpression(List<SDTGuard> guards) {
        List<GuardExpression> expr = new ArrayList<>();
        for (SDTGuard g : guards) {
            expr.add(g.toExpr());
        }
        return new Conjunction(expr.toArray(GuardExpression []::new));
    }

    private Mapping<SymbolicDataValue, DataValue<?>> buildValuation(Word<PSymbolInstance> prefix, PIV piv, Constants constants) {
        Mapping<SymbolicDataValue, DataValue<?>> valuation = new Mapping<SymbolicDataValue, DataValue<?>>();
        DataValue<?>[] values = DataWords.valsOf(prefix);
        piv.forEach((param, reg) -> valuation.put(reg, values[param.getId() - 1]));
        constants.forEach((c, dv) -> valuation.put(c, dv));
        return valuation;
    }
}
