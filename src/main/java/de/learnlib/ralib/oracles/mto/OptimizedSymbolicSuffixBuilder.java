package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
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
     * @return a new suffix formed by prepending suffix with the last symbol of prefix
     */
    public SymbolicSuffix extendSuffix(Word<PSymbolInstance> prefix, SDT sdt, PIV piv, SymbolicSuffix suffix) {
    	assert !prefix.isEmpty();

    	Word<PSymbolInstance> sub = prefix.prefix(prefix.length()-1);
    	PSymbolInstance action = prefix.lastSymbol();
    	SymbolicSuffix actionSuffix = new SymbolicSuffix(sub, Word.fromSymbols(action), restrictionBuilder);
    	Set<Register> actionRegisters = actionRegisters(sub, action, piv);
    	Map<SuffixValue, SymbolicDataValue> sdvMap = new LinkedHashMap<>();

    	int arity = action.getBaseSymbol().getArity();
		int suffixParameters = DataWords.paramLength(suffix.getActions());

    	// find which values are free
    	Set<Integer> newFreeValues = new LinkedHashSet<>();
    	for (int i = 0; i < suffixParameters; i++) {
    		int pos = i + 1;
    		SuffixValue sv = suffix.getDataValue(pos);
    		Set<SymbolicDataValue> comparands = sdt.getComparands(new SuffixValue(sv.getType(), pos));
    		Set<Register> registers = new LinkedHashSet<>();
    		comparands.stream().filter((x) -> (x.isRegister())).forEach((x) -> { registers.add((Register)x); });

    		// determine whether a suffix value is free
    		if (!actionRegisters.containsAll(registers) ||
    				comparands.size() > 1 ||
    				comparands.stream().anyMatch((x) -> (x.isConstant())) ||
    				(comparands.size() == 1 && newFreeValues.contains(comparands.iterator().next().getId()-arity))) {
    			newFreeValues.add(pos + arity);
    		} else if (comparands.size() == 1) {
    			// if non-free and equal to a single symbolic data value, remember that value
    			SymbolicDataValue sdv = comparands.iterator().next();
    			for (SDTGuard g : sdt.getSDTGuards(sv)) {
    				if (g instanceof EqualityGuard) {
    					sdvMap.put(new SuffixValue(sv.getType(), pos), sdv);
    				}
    			}
    		}
    	}
    	// free suffix values in the action
    	for (int i = 0; i < action.getBaseSymbol().getArity(); i++) {
    		if (actionSuffix.getFreeValues().contains(actionSuffix.getDataValue(i+1)))
    			newFreeValues.add(i+1);
    	}

    	Map<Integer, SuffixValue> dataValues = new LinkedHashMap<>();
    	Map<Parameter, SuffixValue> actionParamaterMap = new LinkedHashMap<>();
    	int startingIndex = 1 + DataWords.paramValLength(sub);

    	// fill in suffix values from action
    	int position = 1;
    	ParameterizedSymbol actionSymbol = action.getBaseSymbol();
    	SuffixValueGenerator svGen = new SuffixValueGenerator();
    	for (int i = 0; i < actionSymbol.getArity(); i++ ) {
    		DataType dt = actionSymbol.getPtypes()[i];
    		SuffixValue sv = actionSuffix.getDataValue(i+1);
    		if (!dataValues.values().contains(sv))
    			svGen.next(dt);
    		dataValues.put(position, sv);
    		position++;
    		actionParamaterMap.put(new Parameter(dt, startingIndex+i), sv);
    	}

    	// find relations
    	Map<Integer, SuffixValue> suffixDataValues = new LinkedHashMap<>();
    	Map<Integer, Integer> suffixRelations = new LinkedHashMap<>();
    	for (int i = 1; i < suffixParameters + 1; i++) {
    		SuffixValue sv = suffix.getDataValue(i);
    		SymbolicDataValue sdv = sdvMap.get(new SuffixValue(sv.getType(), i));
    		if (suffixDataValues.values().contains(sv)) {
    			int key = suffixDataValues.entrySet()
    			                          .stream()
    			                          .filter((a) -> (a.getValue().equals(sv)))
    			                          .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
    			                          .findFirst()
    			                          .get().getKey();
    			suffixRelations.put(i+arity, key+arity);
    		} else if (sdv != null && sdv.isSuffixValue()) {
    			if (newFreeValues.contains(sdv.getId()+arity)) {
    				newFreeValues.add(i+arity);
    			} else {
    				suffixRelations.put(sv.getId()+arity, sdv.getId()+arity);
    			}
    		} else if (sdv != null && sdv.isRegister()) {
    			Parameter p = getParameter((Register)sdv, piv);
    			SuffixValue actionSV = actionParamaterMap.get(p);
    			if (actionSuffix.getFreeValues().contains(actionSV)) {
    				newFreeValues.add(i+arity);
    			} else {
    				suffixRelations.put(sv.getId()+arity, actionSV.getId());
    			}
    		}
    		suffixDataValues.put(i, sv);
    	}

    	if (suffixRelations.size() > 0) {
    		assert Collections.min(suffixRelations.keySet()) >= arity + 1;
    	}

		// construct suffix
    	Word<ParameterizedSymbol> actions = suffix.getActions().prepend(action.getBaseSymbol());
    	DataType[] dts = dataTypes(actions);
    	for (int i = 0; i < suffixParameters; i++) {
    		int pos = i + arity + 1;
    		Integer eq = suffixRelations.get(pos);
    		if (eq == null) {
    			dataValues.put(pos, svGen.next(dts[i]));
    		} else {
    			dataValues.put(pos, dataValues.get(eq));
    		}
    	}

    	Set<SuffixValue> freeValues = new LinkedHashSet<>();
    	newFreeValues.stream().forEach((x) -> { freeValues.add(dataValues.get(x)); });

    	for (SuffixValue fv : freeValues) {
    		assert fv != null;
    	}

    	return new SymbolicSuffix(actions, dataValues, freeValues);
    }

    public SymbolicSuffix extendSuffix(Word<PSymbolInstance> prefix, List<SDTGuard> sdtPath, PIV piv, Word<ParameterizedSymbol> suffixActions) {
    	Word<PSymbolInstance> sub = prefix.prefix(prefix.length()-1);
    	PSymbolInstance action = prefix.lastSymbol();
    	ParameterizedSymbol actionSymbol = action.getBaseSymbol();
    	SymbolicSuffix actionSuffix = new SymbolicSuffix(sub, prefix.suffix(1), restrictionBuilder);
    	int actionArity = actionSymbol.getArity();
    	int suffixArity = DataWords.paramLength(suffixActions);
    	DataType[] suffixDataTypes = dataTypes(suffixActions);
    	Map<Register, SuffixValue> actionParameters = buildParameterMap(sub, action, piv);

    	Set<SuffixValue> freeValues = new LinkedHashSet<>();
    	Map<Integer, SuffixValue> dataValues = new LinkedHashMap<>();

    	SuffixValueGenerator svGen = new SuffixValueGenerator();
    	for (int i = 0; i < actionArity; i++) {
    		SuffixValue sv = actionSuffix.getDataValue(i+1);
    		SuffixValue suffixValue = dataValues.values().contains(sv) ?
    				sv :
    				svGen.next(actionSymbol.getPtypes()[i]);
    		dataValues.put(i+1, suffixValue);
    		if (actionSuffix.getFreeValues().contains(suffixValue))
    			freeValues.add(suffixValue);
    	}

    	for (int i = 0; i < suffixArity; i++) {
    		int pos = i + actionArity + 1;
    		SuffixValue sv = new SuffixValue(suffixDataTypes[i], i+1);
    		Set<SymbolicDataValue> comparands = new LinkedHashSet<>();
    		sdtPath.stream().forEach((g) -> { comparands.addAll(g.getComparands(sv)); });
    		Set<SDTGuard> guards = getGuards(sdtPath, sv);
    		assert !guards.isEmpty();

    		boolean free = true;
    		SuffixValue equalSV = null;

    		if (guards.size() > 1) {
    			free = true;
    		} else {
    			SDTGuard guard = guards.iterator().next();
    			if (guard instanceof SDTTrueGuard) {
    				free = false;
    			} else if (guard instanceof EqualityGuard || guard instanceof DisequalityGuard) {
    				SuffixValue comparedSV = null;
    				if (comparands.size() > 1) {
    					free = true;
    				} else {
    					assert comparands.size() == 1;
    					SymbolicDataValue sdv = comparands.iterator().next();
    					if (sdv.isSuffixValue()) {
    						comparedSV = dataValues.get(sdv.getId()+actionArity);
    					} else if (sdv.isRegister()) {
    						comparedSV = actionParameters.get(sdv);
    					}
    					if (comparedSV != null && !freeValues.contains(comparedSV)) {
    						free = false;
    						if (guard instanceof EqualityGuard)
        						equalSV = comparedSV;
    					}
    					else
    						free = true;
    				}
    			} else {
    				free = true;
    			}
    		}

    		if (equalSV != null) {
    			dataValues.put(pos, equalSV);
    		} else {
    			SuffixValue suffixValue = svGen.next(suffixDataTypes[i]);
    			if (free)
    				freeValues.add(suffixValue);
				dataValues.put(pos, suffixValue);
    		}
    	}

    	Word<ParameterizedSymbol> actions = suffixActions.prepend(actionSymbol);
    	return new SymbolicSuffix(actions, dataValues, freeValues);
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
