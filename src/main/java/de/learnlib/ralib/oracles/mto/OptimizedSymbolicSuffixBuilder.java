package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class OptimizedSymbolicSuffixBuilder {

    private final ConstraintSolver solver;

    private final Constants consts;

    private static LearnLogger log = LearnLogger.getLogger(OptimizedSymbolicSuffixBuilder.class);

    public OptimizedSymbolicSuffixBuilder(Constants consts, ConstraintSolver solver) {
        this.solver = solver;
        this.consts = consts;
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


        return null;
    }

    /**
     * Provides an optimized suffix to distinguish two inequivalent locations specified by prefixes,
     * based on the SDTs and associated PIVs that revealed the source of the inequivalence.
     */
    public SymbolicSuffix distinguishingSuffixFromSDTs(Word<PSymbolInstance> prefix1, SDT sdt1, PIV piv1,
            Word<PSymbolInstance> prefix2,  SDT sdt2, PIV piv2,  Word<ParameterizedSymbol> suffixActions) {

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
        SymbolicSuffix suffix = distinguishingSuffixFromSDTs(relSdt1, relSdt2, combined, suffixActions);
        return suffix;
    }

    private SymbolicSuffix distinguishingSuffixFromSDTs(SDT sdt1, SDT sdt2, Mapping<SymbolicDataValue, DataValue<?>> valuation, Word<ParameterizedSymbol> suffixActions) {
        SymbolicSuffix best = new SymbolicSuffix(suffixActions);
        for (boolean b : new boolean [] {true, false}) {
            // we check for paths
            List<List<SDTGuard>> pathsSdt1 = sdt1.getPaths(b);
            List<List<SDTGuard>> pathsSdt2 = sdt2.getPaths(!b);
            for (List<SDTGuard> pathSdt1 : pathsSdt1) {
                GuardExpression expr1 = toGuardExpression(pathSdt1);
                for (List<SDTGuard> pathSdt2 : pathsSdt2) {
                    GuardExpression expr2 = toGuardExpression(pathSdt2);
                    if (solver.isSatisfiable(new Conjunction(expr1, expr2), valuation)) {
                        SymbolicSuffix suffix = buildOptimizedSuffix(pathSdt1, pathSdt2, suffixActions);
                        best = pickBest(best, suffix);
                    }
                }
            }
        }

        return best;
    }

    private SymbolicSuffix buildOptimizedSuffix(List<SDTGuard> pathSdt1, List<SDTGuard> pathSdt2,
            Word<ParameterizedSymbol> suffixActions) {
        SymbolicSuffix suffix1 = buildOptimizedSuffix(pathSdt1, suffixActions);
        SymbolicSuffix suffix2 = buildOptimizedSuffix(pathSdt2, suffixActions);
        return coalesceSuffixes(suffix1, suffix2);
    }

    private SymbolicSuffix coalesceSuffixes(SymbolicSuffix suffix1, SymbolicSuffix suffix2) {
        Set<SuffixValue> freeValues = new LinkedHashSet<>();
        Map<Integer, SuffixValue> dataValues = new LinkedHashMap<>();
        Map<SuffixValue, SuffixValue> sValMapping = new LinkedHashMap<>();
        SymbolicDataValueGenerator.SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
        Set<SuffixValue> freeVals1 = suffix1.getFreeValues();
        Set<SuffixValue> freeVals2 = suffix2.getFreeValues();
        Set<SuffixValue> seenVals1 = new LinkedHashSet<>();
        Set<SuffixValue> seenVals2 = new LinkedHashSet<>();

        for (int i=0; i<DataWords.paramLength(suffix1.getActions()); i++) {
            DataType type = suffix1.getDataValue(i+1).getType();
            SuffixValue sv1 = suffix1.getDataValue(i+1);
            SuffixValue sv2 = suffix2.getDataValue(i+1);
            SuffixValue sv = null;
            if (sv1.equals(sv2) && seenVals1.contains(sv1) && seenVals2.contains(sv2)) {
                sv = sValMapping.get(sv1);
            } else {
                sv = sgen.next(type);
                if (freeVals1.contains(sv1) || freeVals2.contains(sv2)) {
                    freeValues.add(sv);
                } else {
                    if (!sv1.equals(sv2) || Boolean.logicalOr(seenVals1.contains(sv1), seenVals2.contains(sv2))) {
                        freeValues.add(sv);
                    }
                }
            }
            seenVals1.add(sv1);
            seenVals2.add(sv2);
            sValMapping.put(sv1, sv);
            dataValues.put(i+1, sv);
        }
        return new SymbolicSuffix(suffix1.getActions(), dataValues, freeValues);
    }

    private SymbolicSuffix buildOptimizedSuffix(List<SDTGuard> pathSdt, Word<ParameterizedSymbol> suffixActions) {
        Set<SuffixValue> freeValues =  new LinkedHashSet<>();
        Map<Integer, SuffixValue> dataValues = new LinkedHashMap<>();
        Map<SuffixValue, SuffixValue> sValMapping = new LinkedHashMap<>();
        SymbolicDataValueGenerator.SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
        for (int i=0; i<pathSdt.size(); i++) {
            SDTGuard guard = pathSdt.get(i);
            DataType type = guard.getParameter().getType();
            SuffixValue sv = null;
            if (guard instanceof EqualityGuard) {
                SymbolicDataValue rightSdv = ((EqualityGuard) guard).getRegister();
                if (rightSdv.isRegister() || rightSdv.isConstant()) {
                    sv = sgen.next(type);
                    freeValues.add(sv);
                } else {
                    assert rightSdv instanceof SuffixValue;
                    sv = sValMapping.get(rightSdv);
                }
            } else {
                sv = sgen.next(type);
            }
            dataValues.put(i+1, sv);
            sValMapping.put(guard.getParameter(), sv);
        }
        return new SymbolicSuffix(suffixActions, dataValues, freeValues);
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
