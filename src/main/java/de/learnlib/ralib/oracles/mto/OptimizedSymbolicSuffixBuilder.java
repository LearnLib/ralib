package de.learnlib.ralib.oracles.mto;

import java.util.*;

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.*;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class OptimizedSymbolicSuffixBuilder {

    private final Constants consts;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

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
     * @param suffix
     * @param registers - a list of registers that must be revealed by the suffix
     * @return a new suffix formed by prepending suffix with the last symbol of prefix
     */
    public SymbolicSuffix extendSuffix(Word<PSymbolInstance> prefix, SDT sdt, SymbolicSuffix suffix, DataValue... values) {
        Word<ParameterizedSymbol> suffixActions = suffix.getActions();
        if (values.length > 0) {
            SymbolicSuffix s = extendSuffixRevealingRegisters(prefix, sdt, suffixActions, values);
            return s;
        }

        Set<List<SDTGuard>> paths = sdt.getAllPaths(new ArrayList<>()).keySet();
        SymbolicSuffix coalesced = null;
        for (List<SDTGuard> path : paths) {
            SymbolicSuffix extended = extendSuffix(prefix, path, suffixActions);
            if (coalesced == null) {
                coalesced = extended;
            } else {
                coalesced = coalesceSuffixes(coalesced, extended);
            }
        }
        return coalesced;
    }

    private SymbolicSuffix extendSuffixRevealingRegisters(Word<PSymbolInstance> prefix, SDT sdt, Word<ParameterizedSymbol> suffixActions, DataValue[] registers) {
        SDT prunedSDT = pruneSDT(sdt, registers);
        Set<List<SDTGuard>> paths = prunedSDT.getAllPaths(new ArrayList<>()).keySet();
        assert paths.size() > 0 : "All paths in SDT were pruned";
        SymbolicSuffix suffix = null;
        for (List<SDTGuard> path : paths) {
            SymbolicSuffix extended = extendSuffix(prefix, path, suffixActions);
            if (suffix == null) {
                suffix = extended;
            } else {
                suffix = mergeSuffixes(extended, suffix);
            }
        }
        return suffix;
    }

    SymbolicSuffix extendSuffix(Word<PSymbolInstance> prefix, List<SDTGuard> sdtPath, Word<ParameterizedSymbol> suffixActions) {
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

        List<DataValue> subVals = Arrays.stream(DataWords.valsOf(prefix)).toList();
        SDTRelabeling renaming = new SDTRelabeling();
        sdtPath.stream()
                .map(SDTGuard::getRegisters)
                .flatMap(Set::stream)
                .filter(SDTGuardElement::isDataValue)
                .map( x -> (DataValue) x)
                .distinct().forEach( d -> {
                    int dPos = subVals.indexOf(d);
                    if (dPos >= subArity) {
                        SuffixValue sv = new SuffixValue(d.getDataType(), dPos+1-subArity);
                        renaming.put(d, sv);
                    }
                });

        for (SDTGuard guard : sdtPath) {
            SuffixValue oldSV = guard.getParameter();
            SuffixValue newSV = new SuffixValue(oldSV.getDataType(), oldSV.getId()+actionArity);
            renaming.put(oldSV, newSV);
            SDTGuard renamedGuard = SDTGuard.relabel(guard, renaming);
            SuffixValueRestriction restr = restrictionBuilder.restrictSuffixValue(renamedGuard, restrictions);
            restrictions.put(newSV, restr);
        }

        Word<ParameterizedSymbol> actions = suffixActions.prepend(actionSymbol);
        return new SymbolicSuffix(actions, restrictions);

    }

    SDT pruneSDT(SDT sdt, DataValue[] registers) {
        LabeledSDT lsdt = new LabeledSDT(0, sdt);
        LabeledSDT pruned = pruneSDTNode(lsdt, lsdt, registers);
        return pruned.toUnlabeled();
    }

    private LabeledSDT pruneSDTNode(LabeledSDT lsdt, LabeledSDT node, DataValue[] registers) {
        LabeledSDT pruned = lsdt;
        int nodeLabel = node.getLabel();
        for (int label : node.getChildIndices()) {
            if (pruned.getNode(nodeLabel).getChildren().size() < 2) {
                break;
            }
            pruned = pruneSDTBranch(pruned, label, registers);
        }
        for (int label : pruned.getNode(nodeLabel).getChildIndices()) {
            LabeledSDT parent = pruned.getNode(label);
            if (parent != null) {
                pruned = pruneSDTNode(pruned, parent, registers);
            }
        }
        return pruned;
    }

    private LabeledSDT pruneSDTBranch(LabeledSDT lsdt, int label, DataValue[] registers) {
        if (branchContainsRegister(lsdt.getNode(label), registers) ||
            guardOnRegisters(lsdt.getGuard(label), registers)) {
            return lsdt;
        }
        LabeledSDT pruned = LabeledSDT.pruneBranch(lsdt, label);
        SDT prunedSDT = pruned.toUnlabeled();
        int revealedRegisters = 0;
        for (DataValue r : registers) {
            if (guardsOnRegisterHaveBothOutcomes(prunedSDT, r)) {
            revealedRegisters++;
            }
        }
        if (revealedRegisters < registers.length) {
            return lsdt;
        }
        return pruned;
    }

    private boolean branchContainsRegister(LabeledSDT node, DataValue[] registers) {
        for (Map.Entry<SDTGuard, LabeledSDT> e : node.getChildren().entrySet()) {
            SDTGuard guard = e.getKey();
            LabeledSDT child = e.getValue();
            Set<SDTGuardElement> comparands = SDTGuard.getComparands(guard,guard.getParameter());
            for (DataValue sdv : registers) {
                if (comparands.contains(sdv)) {
                    return true;
                }
            }
            boolean childContainsRegister = branchContainsRegister(child, registers);
            if (childContainsRegister)
                return true;
        }
        return false;
    }

    private boolean guardOnRegisters(SDTGuard guard, DataValue[] registers) {
        SuffixValue sv = guard.getParameter();
        for (DataValue r : registers) {
            if (SDTGuard.getComparands(guard, sv).contains(r)) {
            return true;
            }
        }
        return false;
    }

    private SymbolicSuffix mergeSuffixes(SymbolicSuffix suffix1, SymbolicSuffix suffix2) {
        assert suffix1.getActions().equals(suffix2.getActions());

        Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
        for (SuffixValue sv : suffix1.getDataValues()) {
            SuffixValueRestriction restr1 = suffix1.getRestriction(sv);
            SuffixValueRestriction restr2 = suffix2.getRestriction(sv);
            if (restr1.equals(restr2)) {
                restrictions.put(sv, restr1);
            } else {
                restrictions.put(sv, new UnrestrictedSuffixValue(sv));
            }
        }
        return new SymbolicSuffix(suffix1.getActions(), restrictions);
    }

    public boolean sdtRevealsRegister(SDT sdt, SymbolicDataValue register) {
        if (sdt instanceof SDTLeaf) {
            return false;
        }

        Map<SDTGuard, SDT> children = sdt.getChildren();
        Set<SDTGuard> guards = new LinkedHashSet<>();
        for (Map.Entry<SDTGuard, SDT> branch : children.entrySet()) {
            SDTGuard guard = branch.getKey();
            SDT s = branch.getValue();
            if (SDTGuard.getComparands(guard, guard.getParameter()).contains(register)) {
                guards.add(guard);
            } else {
                boolean revealed = sdtRevealsRegister(s, register);
                if (revealed) {
                    return true;
                }
            }
        }

        // cannot have both outcomes if not at least 2 branches
        if (guards.size() < 2) {
            return false;
        }

        // find a guard that can accept
        SDTGuard guardA = null;
        for (SDTGuard g : guards) {
            SDT s = children.get(g);
            if (!s.getPaths(true).isEmpty()) {
                guardA = g;
                break;
            }
        }
        if (guardA == null) {
            return false;
        }
        // exists other guard which can reject?
        for (SDTGuard g : guards) {
            if (g == guardA) {
                continue;
            }
            SDT s = children.get(g);
            if (!s.getPaths(false).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean guardsOnRegisterHaveBothOutcomes(SDT sdt, DataValue register) {
        if (sdt instanceof SDTLeaf)
            return true;

        Map<SDTGuard, SDT> childrenWithRegister = new LinkedHashMap<>();
        for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
            if (!SDTGuard.getComparands(e.getKey(), register).isEmpty()) {
            childrenWithRegister.put(e.getKey(), e.getValue());
            }
        }
        if (!childrenWithRegister.isEmpty()) {
            if (!guardHasBothOutcomes(childrenWithRegister)) {
            return false;
            }
        }
        boolean ret = true;
        for (SDT child : sdt.getChildren().values()) {
            ret = ret && guardsOnRegisterHaveBothOutcomes(child, register);
        }
        return ret;
    }

    private boolean guardHasBothOutcomes(Map<SDTGuard, SDT> children) {
        if (children.size() < 2)
            return false;
        Iterator<SDTGuard> guards = children.keySet().iterator();
        SDT firstSDT = children.get(guards.next());
        boolean hasAccepting = !firstSDT.getPaths(true).isEmpty();
        while(guards.hasNext()) {
            // if hasAccepting, find rejecting
            // else find accepting
            if (!children.get(guards.next()).getPaths(!hasAccepting).isEmpty()) {
                return true;
            }
        }
        if (hasAccepting) {
            // could not find rejecting, see if first sdt has rejecting
            return !firstSDT.getPaths(false).isEmpty();
        }
        return false;
    }

    /**
     * Provides a one-symbol extension of an (optimized) suffix for two non-empty prefixes leading to inequivalent locations,
     * based on the SDTs and associated PIVs that revealed the source of the inequivalence.
     */
    public SymbolicSuffix extendDistinguishingSuffix(Word<PSymbolInstance> prefix1, SDT sdt1,
            Word<PSymbolInstance> prefix2,  SDT sdt2,  SymbolicSuffix suffix) {
        assert !prefix1.isEmpty() && !prefix2.isEmpty() && prefix1.lastSymbol().getBaseSymbol().equals(prefix2.lastSymbol().getBaseSymbol());
        // prefix1 = subprefix1 + sym(d1); prefix2 = subprefix2 + sym(d2)
        // our new_suffix will be sym(s1) + suffix
        // we first determine if s1 is free (extended to all parameters in sym, if there are more)
        SymbolicSuffix suffix1 = extendSuffix(prefix1, sdt1, suffix);
        SymbolicSuffix suffix2 = extendSuffix(prefix2, sdt2, suffix);

        return coalesceSuffixes(suffix1, suffix2);
    }

    /**
     * Provides an optimized suffix to distinguish two inequivalent locations specified by prefixes,
     * based on the SDTs and associated PIVs that revealed the source of the inequivalence.
     */
    public SymbolicSuffix distinguishingSuffixFromSDTs(Word<PSymbolInstance> prefix1, SDT sdt1,
            Word<PSymbolInstance> prefix2,  SDT sdt2,  Word<ParameterizedSymbol> suffixActions, ConstraintSolver solver) {

        // we relabel SDTs and PIV such that they use different registers
        SymbolicDataValueGenerator.ParameterGenerator rgen = new SymbolicDataValueGenerator.ParameterGenerator();
        SDTRelabeling relabellingSdt1 = new SDTRelabeling();
        /*
        if (true) throw new RuntimeException("fix PIV");
        for (Object r : sdt1.getDataValues()) {
            relabellingSdt1.put( r, rgen.next( ((TypedValue)r).getDataType()));
        }
        SDT relSdt1 =  sdt1.relabel(relabellingSdt1);
        PIV relPiv1 = piv1.relabel(relabellingSdt1);

        VarMapping<Register, Register> relabellingSdt2 = new VarMapping<>();
        for (Register r : piv2.values()) {
            relabellingSdt2.put(r, rgen.next(r.getDataType()));
        }
        SDT relSdt2 =  sdt2.relabel(relabellingSdt2);
        PIV relPiv2 = piv2.relabel(relabellingSdt2);
        */
        // we build valuations which we use to determine satisfiable paths
        Mapping<SymbolicDataValue, DataValue> valuationSdt1 = buildValuation(prefix1, consts);
        Mapping<SymbolicDataValue, DataValue> valuationSdt2 = buildValuation(prefix2, consts);
        Mapping<SymbolicDataValue, DataValue> combined = new Mapping<>();
        combined.putAll(valuationSdt1);
        combined.putAll(valuationSdt2);
        SymbolicSuffix suffix = distinguishingSuffixFromSDTs(prefix1, sdt1, prefix2, sdt2, combined, suffixActions, solver);
        return suffix;
    }

    private SymbolicSuffix distinguishingSuffixFromSDTs(Word<PSymbolInstance> prefix1, SDT sdt1,
            Word<PSymbolInstance> prefix2, SDT sdt2,
            Mapping<SymbolicDataValue, DataValue> valuation, Word<ParameterizedSymbol> suffixActions, ConstraintSolver solver) {
        SymbolicSuffix best = null;
        for (boolean b : new boolean [] {true, false}) {
            // we check for paths
            List<List<SDTGuard>> pathsSdt1 = sdt1.getPaths(b);
            List<List<SDTGuard>> pathsSdt2 = sdt2.getPaths(!b);
            for (List<SDTGuard> pathSdt1 : pathsSdt1) {
                Expression<Boolean>  expr1 = toGuardExpression(pathSdt1);
                for (List<SDTGuard> pathSdt2 : pathsSdt2) {
                    Expression<Boolean>  expr2 = toGuardExpression(pathSdt2);
                    if (solver.isSatisfiable(ExpressionUtil.and(expr1, expr2), valuation)) {
                        SymbolicSuffix suffix = buildOptimizedSuffix(prefix1, pathSdt1, prefix2, pathSdt2, suffixActions);
                        best = pickBest(best, suffix);
                    }
                }
            }
        }

        return best;
    }

    private SymbolicSuffix buildOptimizedSuffix(Word<PSymbolInstance> prefix1, List<SDTGuard> pathSdt1,
            Word<PSymbolInstance> prefix2, List<SDTGuard> pathSdt2,
            Word<ParameterizedSymbol> suffixActions) {
        SymbolicSuffix suffix1 = extendSuffix(prefix1, pathSdt1, suffixActions);
        SymbolicSuffix suffix2 = extendSuffix(prefix2, pathSdt2, suffixActions);

        return coalesceSuffixes(suffix1, suffix2);
    }

    SymbolicSuffix coalesceSuffixes(SymbolicSuffix suffix1, SymbolicSuffix suffix2) {
        assert suffix1.getActions().equals(suffix2.getActions());

        Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();

        SymbolicDataValueGenerator.SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
        for (int i=0; i<DataWords.paramLength(suffix1.getActions()); i++) {
            DataType type = suffix1.getDataValue(i+1).getDataType();
            SuffixValue sv = sgen.next(type);
            SuffixValueRestriction restr1 = suffix1.getRestriction(sv);
            SuffixValueRestriction restr2 = suffix2.getRestriction(sv);
            SuffixValueRestriction restr = restr1.merge(restr2, restrictions);
            restrictions.put(sv, restr);
        }

        return new SymbolicSuffix(suffix1.getActions(), restrictions);
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


    private Expression<Boolean> toGuardExpression(List<SDTGuard> guards) {
        List<Expression<Boolean>> expr = new ArrayList<>();
        for (SDTGuard g : guards) {
            expr.add(SDTGuard.toExpr(g));
        }
        Expression<Boolean> [] exprArr = new Expression[expr.size()];
        return ExpressionUtil.and(expr.toArray(exprArr));
    }

    private Mapping<SymbolicDataValue, DataValue> buildValuation(Word<PSymbolInstance> prefix, Constants constants) {
        Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<SymbolicDataValue, DataValue>();
        DataValue[] values = DataWords.valsOf(prefix);
        constants.forEach((c, dv) -> valuation.put(c, dv));
        return valuation;
    }
}
