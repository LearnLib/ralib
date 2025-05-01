/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class MultiTheorySDTLogicOracle implements SDTLogicOracle {

    private final ConstraintSolver solver;

    private final Constants consts;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTheorySDTLogicOracle.class);

    public MultiTheorySDTLogicOracle(Constants consts, ConstraintSolver solver) {
        this.solver = solver;
        this.consts = consts;
    }

    @Override
    public boolean hasCounterexample(Word<PSymbolInstance> prefix, SDT sdt1,
            SDT sdt2, Expression<Boolean> guard, Word<PSymbolInstance> rep) {

        // Collection<SymbolicDataValue> join = piv1.values();

        LOGGER.trace("Searching for counterexample in SDTs");
        LOGGER.trace("SDT1: {0}", sdt1);
        LOGGER.trace("SDT2: {0}", sdt2);
        LOGGER.trace("Guard: {0}", guard);

        Expression<Boolean> expr1 = sdt1.getAcceptingPaths(consts);
        Expression<Boolean> expr2 = sdt2.getAcceptingPaths(consts);
        Expression<Boolean> exprG = guard;

        //System.out.println(exprG);

        VarMapping<SymbolicDataValue, SymbolicDataValue> gremap = new VarMapping<>();
        for (Variable<?> sv : ExpressionUtil.freeVariables(exprG)) {
            if (sv instanceof Parameter p) {
                gremap.put(p, new SuffixValue( p.getDataType(), p.getId()));
            }
        }

        exprG = SMTUtil.renameVars(exprG, gremap);
        VarMapping<Register, Register> remap = new VarMapping<>(); //piv2.createRemapping(piv1);

        Expression<Boolean> expr2r = SMTUtil.renameVars(expr2, remap);
        Expression<Boolean> left = ExpressionUtil.and(exprG, expr1, new Negation(expr2r));
        Expression<Boolean> right = ExpressionUtil.and(exprG, expr2r, new Negation(expr1));
        Expression<Boolean> test = ExpressionUtil.or(left, right);

//        System.out.println("A1:  " + expr1);
//        System.out.println("A2:  " + expr2);
//        System.out.println("G:   " + exprG);
//        System.out.println("MAP: " + remap);
//        System.out.println("A2': " + expr2r);
//        System.out.println("TEST:" + test);
//
//        System.out.println("HAS CE: " + test);
        boolean r = solver.isSatisfiable(test, new Mapping<>());
        LOGGER.trace("Res:" + r);
        return r;
    }

    @Override
    public Expression<Boolean> getCEGuard(Word<PSymbolInstance> prefix, SDT sdt1, SDT sdt2) {

    	SDT _sdt1 =  sdt1;
    	SDT _sdt2 =  sdt2;

    	Map<Expression<Boolean>, Boolean> exprMap1 = _sdt1.getGuardExpressions(consts);
    	Map<Expression<Boolean>, Boolean> exprMap2 = _sdt2.getGuardExpressions(consts);

    	for (Map.Entry<Expression<Boolean>, Boolean> e1 : exprMap1.entrySet()) {
            Expression<Boolean> expr1 = e1.getKey();
    		boolean outcome1 = e1.getValue();
    		for (Map.Entry<Expression<Boolean>, Boolean> e2 : exprMap2.entrySet()) {
                Expression<Boolean> expr2 = e2.getKey();
    			boolean outcome2 = e2.getValue();
    			if (outcome1 != outcome2) {
    				//VarMapping<Register, Register> remap = piv2.createRemapping(piv1);
                    Expression<Boolean> test = ExpressionUtil.and(expr1, expr2);
    				if (solver.isSatisfiable(test, new Mapping<>())) {
    					return expr1;
    				}
    			}
    		}
    	}
    	return null;
    }

    @Override
    public boolean doesRefine(Expression<Boolean> refining, Expression<Boolean> refined, Mapping<SymbolicDataValue, DataValue> valuation) {

        LOGGER.trace("refining: {0}", refining);
        LOGGER.trace("refined: {0}", refined);

        //VarMapping<Register, Register> remap = pivRefined.createRemapping(pivRefining);

        Expression<Boolean> exprRefining = refining;
        Expression<Boolean> exprRefined = refined; //SMTUtil.renameVars(refined, remap);

        // is there any case for which refining is true but refined is false?
        Expression<Boolean> test = ExpressionUtil.and(
                exprRefining, new gov.nasa.jpf.constraints.expressions.Negation(exprRefined));

        // it is important to include constants to see that, e.g., c1==p1 refines c2!=p1
        Mapping<SymbolicDataValue, DataValue> valWithConsts = new Mapping<>();
        valWithConsts.putAll(valuation);
        valWithConsts.putAll(consts);

        //LOGGER.trace("MAP: " + remap);
        LOGGER.trace("TEST:" + test);

        boolean r = solver.isSatisfiable(test, valWithConsts);
        return !r;
    }

    @Override
    public boolean areMutuallyExclusive(Expression<Boolean> guard1, Expression<Boolean> guard2,
            Mapping<SymbolicDataValue, DataValue> valuation) {
        LOGGER.trace("guard1: {0}", guard1);
        LOGGER.trace("guard2: {0}", guard2);

        Expression<Boolean> exprGuard1 = guard1;
        Expression<Boolean> exprGuard2 = guard2;

        Expression<Boolean>  test = ExpressionUtil.and(exprGuard1, exprGuard2);

        LOGGER.trace("TEST:" + test);

        boolean r = solver.isSatisfiable(test, valuation);
        return !r;
    }

    @Override
    public boolean areEquivalent(Expression<Boolean> guard1, Bijection<DataValue> remapping, Expression<Boolean> guard2,
                                 Mapping<SymbolicDataValue, DataValue> valuation) {
        LOGGER.trace("guard1: {0}", guard1);
        LOGGER.trace("guard2: {0}", guard2);
        LOGGER.trace("remapping: {0}", remapping);

        //Mapping<DataValue, DataValue> remap = piv2.createRemapping(piv1);

        Expression<Boolean> g2relabel = SMTUtil.renameVals(guard2, remapping);

        Expression<Boolean> test = ExpressionUtil.or(
                ExpressionUtil.and(guard1, new gov.nasa.jpf.constraints.expressions.Negation(g2relabel)),
                ExpressionUtil.and(new gov.nasa.jpf.constraints.expressions.Negation(guard1), g2relabel)
        );

        LOGGER.trace("MAP: " + remapping);
        LOGGER.trace("TEST:" + test);

        boolean r = solver.isSatisfiable(test, valuation);
        return !r;
    }

    @Override
    public boolean accepts(Word<PSymbolInstance> word, Word<PSymbolInstance> prefix, SDT sdt) {
        assert prefix.isPrefixOf(word) : "invalid prefix";
        SDT _sdt =   sdt;
        assert _sdt.getHeight() == DataWords.paramValLength(word.suffix(word.length() - prefix.length()))  :
            "The height of the tree is not consistent with the number of parameters in the word";
        Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<>();
        valuation.putAll(consts);
        /*DataValue[] vals = DataWords.valsOf(prefix);
        for (Map.Entry<Parameter, Register> entry : piv.entrySet()) {
             DataValue parVal = vals[entry.getKey().getId()-1];
             valuation.put(entry.getValue(), parVal);
        }*/

        boolean accepts = accepts(word, prefix, prefix.length(), _sdt, valuation);
        return accepts;
    }

    private boolean accepts(Word<PSymbolInstance> word, Word<PSymbolInstance> prefix, int symIndex, SDT sdt,
            Mapping<SymbolicDataValue, DataValue> valuation) {
        boolean accepts;
        if (symIndex == word.length()) {
            accepts = sdt.isAccepting();
        } else {
            PSymbolInstance sym = word.getSymbol(symIndex);
            if (sym.getBaseSymbol().getArity() == 0) {
                accepts = accepts(word, prefix, symIndex + 1, sdt, valuation);
            } else {
                SDT nextSdt = sdt;
                Mapping<SymbolicDataValue, DataValue> newValuation = new Mapping<>();
                newValuation.putAll(valuation);
                for (int i = 0; i < sym.getBaseSymbol().getArity(); i++) {
                    DataValue value = sym.getParameterValues()[i];
                    SuffixValue suffixValue = nextSdt.getChildren().keySet().iterator().next().getParameter();
                    newValuation.put(suffixValue, value);
                    boolean found = false;
                    for (Map.Entry<SDTGuard, SDT> entry : nextSdt.getChildren().entrySet()) {
                        Expression<Boolean> guardExpr = SDTGuard.toExpr(entry.getKey());
                        if (solver.isSatisfiable(guardExpr, newValuation)) {
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
