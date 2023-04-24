/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.oracles.mto;

import java.util.Map;
import java.util.logging.Level;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheorySDTLogicOracle implements SDTLogicOracle {

    private final ConstraintSolver solver;

    private final Constants consts;

    private static LearnLogger log = LearnLogger.getLogger(MultiTheorySDTLogicOracle.class);

    public MultiTheorySDTLogicOracle(Constants consts, ConstraintSolver solver) {
        this.solver = solver;
        this.consts = consts;
    }

    @Override
    public boolean hasCounterexample(Word<PSymbolInstance> prefix, SymbolicDecisionTree sdt1, PIV piv1,
            SymbolicDecisionTree sdt2, PIV piv2, TransitionGuard guard, Word<PSymbolInstance> rep) {

        // Collection<SymbolicDataValue> join = piv1.values();

        log.finest("Searching for counterexample in SDTs");
        log.log(Level.FINEST, "SDT1: {0}", sdt1);
        log.log(Level.FINEST, "SDT2: {0}", sdt2);
        log.log(Level.FINEST, "Guard: {0}", guard);

        SDT _sdt1 = (SDT) sdt1;
        SDT _sdt2 = (SDT) sdt2;

        GuardExpression expr1 = _sdt1.getAcceptingPaths(consts);
        GuardExpression expr2 = _sdt2.getAcceptingPaths(consts);
        GuardExpression exprG = guard.getCondition();

        VarMapping<SymbolicDataValue, SymbolicDataValue> gremap = new VarMapping<>();
        for (SymbolicDataValue sv : exprG.getSymbolicDataValues()) {
            if (sv instanceof Parameter) {
                gremap.put(sv, new SuffixValue(sv.getType(), sv.getId()));
            }
        }

        exprG = exprG.relabel(gremap);

        VarMapping<Register, Register> remap = piv2.createRemapping(piv1);

        GuardExpression expr2r = expr2.relabel(remap);

        GuardExpression left = new Conjunction(exprG, expr1, new Negation(expr2r));

        GuardExpression right = new Conjunction(exprG, expr2r, new Negation(expr1));

        GuardExpression test = new Disjunction(left, right);

//        System.out.println("A1:  " + expr1);
//        System.out.println("A2:  " + expr2);
//        System.out.println("G:   " + exprG);
//        System.out.println("MAP: " + remap);
//        System.out.println("A2': " + expr2r);
//        System.out.println("TEST:" + test);
//
//        System.out.println("HAS CE: " + test);
        boolean r = solver.isSatisfiable(test, new Mapping<>());
        log.log(Level.FINEST, "Res:" + r);
        return r;
    }

    @Override
    public boolean doesRefine(TransitionGuard refining, PIV pivRefining, TransitionGuard refined, PIV pivRefined, Mapping<SymbolicDataValue, DataValue<?>> valuation) {

        log.log(Level.FINEST, "refining: {0}", refining);
        log.log(Level.FINEST, "refined: {0}", refined);
        log.log(Level.FINEST, "pivRefining: {0}", pivRefining);
        log.log(Level.FINEST, "pivRefined: {0}", pivRefined);

        VarMapping<Register, Register> remap = pivRefined.createRemapping(pivRefining);

        GuardExpression exprRefining = refining.getCondition();
        GuardExpression exprRefined = refined.getCondition().relabel(remap);

        // is there any case for which refining is true but refined is false?
        GuardExpression test = new Conjunction(exprRefining, new Negation(exprRefined));

        log.log(Level.FINEST, "MAP: " + remap);
        log.log(Level.FINEST, "TEST:" + test);

        boolean r = solver.isSatisfiable(test, valuation);
        return !r;
    }

    public boolean areMutuallyExclusive(TransitionGuard guard1, PIV piv1, TransitionGuard guard2,
            PIV piv2, Mapping<SymbolicDataValue, DataValue<?>> valuation) {
        log.log(Level.FINEST, "guard1: {0}", guard1);
        log.log(Level.FINEST, "guard2: {0}", guard2);
        log.log(Level.FINEST, "piv1: {0}", piv1);
        log.log(Level.FINEST, "piv2: {0}", piv2);

        VarMapping<Register, Register> remap = piv2.createRemapping(piv1);

        GuardExpression exprGuard1 = guard1.getCondition();
        GuardExpression exprGuard2 = guard2.getCondition().relabel(remap);

        GuardExpression test = new Conjunction(exprGuard1, exprGuard2);

        log.log(Level.FINEST, "MAP: " + remap);
        log.log(Level.FINEST, "TEST:" + test);

        boolean r = solver.isSatisfiable(test, valuation);
        return !r;
    }

    public boolean areEquivalent(TransitionGuard guard1, PIV piv1, TransitionGuard guard2,
            PIV piv2, Mapping<SymbolicDataValue, DataValue<?>> valuation) {
        log.log(Level.FINEST, "guard1: {0}", guard1);
        log.log(Level.FINEST, "guard2: {0}", guard2);
        log.log(Level.FINEST, "piv1: {0}", piv1);
        log.log(Level.FINEST, "piv2: {0}", piv2);

        VarMapping<Register, Register> remap = piv2.createRemapping(piv1);

        GuardExpression exprGuard1 = guard1.getCondition();
        GuardExpression exprGuard2 = guard2.getCondition().relabel(remap);
        GuardExpression test = new Disjunction(new Conjunction(exprGuard1, new Negation(exprGuard2)),
        		                               new Conjunction(new Negation(exprGuard1), exprGuard2));

        log.log(Level.FINEST, "MAP: " + remap);
        log.log(Level.FINEST, "TEST:" + test);

        boolean r = solver.isSatisfiable(test, valuation);
        return !r;

    }

    public boolean accepts(Word<PSymbolInstance> word, Word<PSymbolInstance> prefix, SymbolicDecisionTree sdt, PIV piv) {
        assert prefix.isPrefixOf(word) : "invalid prefix";
        SDT _sdt =  (SDT) sdt;
        assert _sdt.getHeight() == DataWords.paramValLength(word.suffix(word.length() - prefix.length()))  :
            "The height of the tree is not consistent with the number of parameters in the word";
        Mapping<SymbolicDataValue, DataValue<?>> valuation = new Mapping<>();
        valuation.putAll(consts);
        DataValue[] vals = DataWords.valsOf(prefix);
        for (Map.Entry<Parameter, Register> entry : piv.entrySet()) {
             DataValue parVal = vals[entry.getKey().getId()-1];
             valuation.put(entry.getValue(), parVal);
        }

        boolean accepts = accepts(word, prefix, prefix.length(), _sdt, valuation);
        return accepts;
    }

    private boolean accepts(Word<PSymbolInstance> word, Word<PSymbolInstance> prefix, int symIndex, SDT sdt,
            Mapping<SymbolicDataValue, DataValue<?>> valuation) {
        boolean accepts;
        if (symIndex == word.length()) {
            accepts =  sdt.isAccepting();
        } else {
            PSymbolInstance sym = word.getSymbol(symIndex);
            if (sym.getBaseSymbol().getArity() == 0) {
                accepts = accepts(word, prefix, symIndex + 1, sdt, valuation);
            } else {
                SDT nextSdt = sdt;
                Mapping<SymbolicDataValue, DataValue<?>> newValuation = new Mapping<>();
                newValuation.putAll(valuation);
                for (int i = 0; i < sym.getBaseSymbol().getArity(); i++) {
                    DataValue value = sym.getParameterValues()[i];
                    SuffixValue suffixValue = nextSdt.getChildren().keySet().iterator().next().getParameter();
                    newValuation.put(suffixValue, value);
                    boolean found = false;
                    for (Map.Entry<SDTGuard, SDT> entry : nextSdt.getChildren().entrySet()) {
                        TransitionGuard guardExpr = entry.getKey().toTG();
                        if (solver.isSatisfiable(guardExpr.getCondition(), newValuation)) {
                            nextSdt = entry.getValue();
                            found = true;
                            break;
                        }
                    }
                    assert found : "Could not find a satisfiable guard";
                }
                accepts = accepts(word, prefix, symIndex+1, nextSdt, newValuation);
            }
        }
        return accepts;
    }
}
