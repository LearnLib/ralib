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
package de.learnlib.ralib.equivalence;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.jconstraints.JContraintsUtil;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.SymbolicSearchEngine;
import gov.nasa.jstateexplorer.datastructures.searchImage.SearchIterationImage;
import gov.nasa.jstateexplorer.transitionSystem.SymbolicTransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.SynchronisedTransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.TransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.words.Word;

/**
 * Uses symbolic search on the cross product of two register automaton models
 * for finding counterexamples.
 *
 * TODOS: - no support for constants currently - no support for types currently
 *
 * @author fh
 */
public class IOEquivalenceMC implements IOEquivalenceOracle {

    private final static Type TYPE = BuiltinTypes.SINT32;

    protected static final Logger logger
            = Logger.getLogger(IOEquivalenceMC.class.getName());

    private static class StatePair {

        private final RALocation idModel;
        private final RALocation idHyp;

        private StatePair(RALocation idModel, RALocation idHyp) {
            this.idModel = idModel;
            this.idHyp = idHyp;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.idModel);
            hash = 97 * hash + Objects.hashCode(this.idHyp);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StatePair other = (StatePair) obj;
            if (!Objects.equals(this.idModel, other.idModel)) {
                return false;
            }
            if (!Objects.equals(this.idHyp, other.idHyp)) {
                return false;
            }
            return true;
        }

    }

    private static class TransitionTuple {

        private final ParameterizedSymbol a;
        private final gov.nasa.jstateexplorer.transitionSystem.Transition t;
        private String info;

        private TransitionTuple(ParameterizedSymbol a,
                gov.nasa.jstateexplorer.transitionSystem.Transition t,
                String info) {
            this.a = a;
            this.t = t;
            this.info = info;
        }

    }

    private final Set<StatePair> visited = new HashSet<>();
    private final Queue<StatePair> queue = new LinkedList<>();

    private final Map<String, TransitionTuple> tuples = new HashMap<>();

    private final RegisterAutomaton model;
    private final Collection<ParameterizedSymbol> inputs;

    private RegisterAutomaton hyp;

    private final Map<Register, Variable> hypRegs = new HashMap<>();
    private final Map<Register, Variable> modelRegs = new HashMap<>();

    private final Variable mq = new Variable(TYPE, "__mq");
    private final Variable hq = new Variable(TYPE, "__hq");

    private final gov.nasa.jpf.constraints.api.ConstraintSolver solver;

    private int tId = 0;

    private final IOOracle ioOracle;

    public IOEquivalenceMC(RegisterAutomaton model,
            Collection<ParameterizedSymbol> inputs, IOOracle ioOracle) {

        this.model = model;
        this.inputs = inputs;
        this.ioOracle = ioOracle;

        gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory fact
                = new gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory();

        solver = fact.createSolver("z3");
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(
            RegisterAutomaton a, Collection<? extends PSymbolInstance> clctn) {

        this.hyp = a;

        hypRegs.clear();
        modelRegs.clear();
        buildVarMaps();
        tuples.clear();

        logger.info("Building transition system ...");
        TransitionSystem ts = buildTransitionSystem();
        logger.log(Level.FINE, "Transition System: \n{0}", ts.completeToString());

        TransitionHelper helper = new SynchronisedTransitionHelper();
        ts.setHelper(helper);
        logger.info("Starting model checking ...");
        SearchIterationImage image = SymbolicSearchEngine.
                symbolicBreadthFirstSearch(
                        ts, solver, Integer.MIN_VALUE);

        if (image.getHistoryForCE() != null) {
            Word<PSymbolInstance> ceWord = buildCE(image.getHistoryForCE());
            Word<PSymbolInstance> ceTrace = ioOracle.trace(ceWord);

            DefaultQuery<PSymbolInstance, Boolean> ce
                    = new DefaultQuery<>(ceTrace);
            ce.answer(Boolean.TRUE);

            logger.log(Level.INFO, "Found CE: {0}", ce.getInput());
            return ce;
        }
        return null;
    }

    private TransitionSystem buildTransitionSystem() {

        Valuation initVal = computeInitialState();
        logger.log(Level.FINE, "Initial: {0}", initVal);

        List<TransitionTuple> trans = new ArrayList<>();

        visited.clear();
        queue.clear();
        StatePair i = new StatePair(model.getInitialState(), hyp.getInitialState());
        queue.add(i);
        visited.add(i);

        while (!queue.isEmpty()) {
            StatePair p = queue.poll();
            RALocation ml = p.idModel;
            RALocation hl = p.idHyp;

            boolean output = isOutputLoc(ml);
            if (isOutputLoc(ml) ^ isOutputLoc(hl)) {
                continue;
            }

            if (output) {
                oloc(ml, hl, trans);
            } else {
                iloc(ml, hl, trans);
            }
        }

        List<gov.nasa.jstateexplorer.transitionSystem.Transition> tt = new ArrayList<>();
        trans.stream().forEach((t) -> {
            tt.add(t.t);
            tuples.put(t.t.getId(), t);
        });
        return new TransitionSystem(initVal, tt, new SymbolicTransitionHelper());
    }

    private void oloc(RALocation ml, RALocation hl, List<TransitionTuple> ret) {

        assert ml.getOut().size() == 1;
        assert hl.getOut().size() == 1;

        for (Transition mt : ml.getOut()) {
            for (Transition ht : hl.getOut()) {
                //assert mt.getLabel().equals(ht.getLabel());
                logger.log(Level.FINE, "OUT: {0} : {1}", new Object[]{mt, ht});
                Map<Parameter, Variable> pmap = buildParMap(mt.getLabel());
                otrans(ml, hl, (OutputTransition) mt, (OutputTransition) ht, pmap, ret);
            }
        }
    }

    private void iloc(RALocation ml, RALocation hl, List<TransitionTuple> ret) {

        for (ParameterizedSymbol ps : inputs) {
            Map<Parameter, Variable> pmap = buildParMap(ps);

            Collection<Transition> mTrans = ml.getOut(ps);
            Collection<Transition> hTrans = hl.getOut(ps);

            if (mTrans == null && hTrans == null) {
                continue;
            }

            if (mTrans == null ^ hTrans == null) {

                Expression<Boolean> guard = (mTrans == null)
                        ? or(hTrans, pmap, hypRegs)
                        : or(mTrans, pmap, modelRegs);

                guard = ExpressionUtil.and(locGuard(ml, hl), guard);

                gov.nasa.jstateexplorer.transitionSystem.Transition t
                        = new gov.nasa.jstateexplorer.transitionSystem.Transition(
                                guard, new HashMap<>(),
                                getId(null, null, false), true, true);

                if (solver.isSatisfiable(guard) == Result.SAT) {
                    ret.add(new TransitionTuple(ps, t, "iloc one is null: " + ps));
                    logger.log(Level.FINE, "Transition: {0}", t);
                }
                return;
            }

            for (Transition mt : ml.getOut(ps)) {
                for (Transition ht : hl.getOut(ps)) {
                    logger.log(Level.FINE, "IN: {0} : {1}", new Object[]{mt, ht});
                    itrans(ml, hl, (InputTransition) mt, (InputTransition) ht,
                            pmap, ret);
                }
            }

            Expression<Boolean> mAll = or(mTrans, pmap, modelRegs);
            Expression<Boolean> hAll = or(hTrans, pmap, hypRegs);

            assert mAll != null;
            assert hAll != null;

            Expression<Boolean> guard = new PropositionalCompound(
                    mAll, LogicalOperator.XOR, hAll);

            guard = ExpressionUtil.and(locGuard(ml, hl), guard);

            // error catch alll
            gov.nasa.jstateexplorer.transitionSystem.Transition t
                    = new gov.nasa.jstateexplorer.transitionSystem.Transition(
                            guard, new HashMap<>(), getId(null, null, false), true, true);

            if (solver.isSatisfiable(guard) == Result.SAT) {
                ret.add(new TransitionTuple(ps, t, "iloc error catch all: " + ps));
                logger.log(Level.FINE, "Transition: {0}", t);
            }
        }
    }

    private void itrans(RALocation ml, RALocation hl, InputTransition mt,
            InputTransition ht, Map<Parameter, Variable> pmap,
            List<TransitionTuple> ret) {

        Expression<Boolean> guard = buildInputTransitionGuard(ml, hl, mt, ht, pmap);
        Map<Variable, Expression<Boolean>> effects = new HashMap<>();
        computeEffects(mt, ht, effects, pmap);

        gov.nasa.jstateexplorer.transitionSystem.Transition t
                = new gov.nasa.jstateexplorer.transitionSystem.Transition(
                        guard, effects, getId(mt, ht, true), true, false);

        if (solver.isSatisfiable(guard) == Result.SAT) {
            ret.add(new TransitionTuple(mt.getLabel(), t, "itrans: " + mt.toString() + " : " + ht.toString()));
            logger.log(Level.FINE, "Transition: {0}", t);
            StatePair p = new StatePair(mt.getDestination(), ht.getDestination());
            if (!visited.contains(p)) {
                visited.add(p);
                queue.add(p);
            }
        }
    }

    private void otrans(RALocation ml, RALocation hl, OutputTransition mt,
            OutputTransition ht, Map<Parameter, Variable> pmap,
            List<TransitionTuple> ret) {

        if (!mt.getLabel().equals(ht.getLabel())) {
            // error with true guard;
            Expression<Boolean> guard = locGuard(ml, hl);
            gov.nasa.jstateexplorer.transitionSystem.Transition t
                    = new gov.nasa.jstateexplorer.transitionSystem.Transition(
                            guard, new HashMap<>(), getId(mt, ht, false), true, true);

            if (solver.isSatisfiable(guard) == Result.SAT) {
                ret.add(new TransitionTuple(mt.getLabel(), t, "otrans labels: " +  mt.toString() + " : " + ht.toString()));
                logger.log(Level.FINE, "Transition: {0}", t);
            }
            return;
        }

        // ok guard
        // p1 == p1 and p2 == p2 and ...
        Expression<Boolean> okGuard
                = buildOutputTransitionGuardOK(ml, hl, mt, ht, pmap);
        Map<Variable, Expression<Boolean>> effects = new HashMap<>();
        computeEffects(mt, ht, effects, pmap);
        gov.nasa.jstateexplorer.transitionSystem.Transition t
                = new gov.nasa.jstateexplorer.transitionSystem.Transition(
                        okGuard, effects, getId(mt, ht, true), true, false);

        if (solver.isSatisfiable(okGuard) == Result.SAT) {
            ret.add(new TransitionTuple(mt.getLabel(), t, "otrans guards: " + mt.toString() + " : " + ht.toString()));
            logger.log(Level.FINE, "Transition: {0}", t);
            StatePair p = new StatePair(mt.getDestination(), ht.getDestination());
            if (!visited.contains(p)) {
                visited.add(p);
                queue.add(p);
            }
        }

        // error guard        
        // p1 != p1 or p2 != p2 or ...
        Expression<Boolean> errGuard
                = buildOutputTransitionGuardError(ml, hl, mt, ht, pmap);
        t = new gov.nasa.jstateexplorer.transitionSystem.Transition(
                errGuard, new HashMap<>(),
                getId(mt, ht, false), true, true);

        if (solver.isSatisfiable(errGuard) == Result.SAT) {
            ret.add(new TransitionTuple(mt.getLabel(), t, "otrans guards: " +  mt.toString() + " : " + ht.toString()));
            logger.log(Level.FINE, "Transition: {0}", t);
        }
    }

    private Expression<Boolean> buildOutputTransitionGuardOK(
            RALocation ml, RALocation hl, OutputTransition mt,
            OutputTransition ht, Map<Parameter, Variable> pmap) {

        Expression<Boolean> guard = locGuard(ml, hl);
        assert pmap.size() == mt.getOutput().getOutput().size();
        assert pmap.size() == ht.getOutput().getOutput().size();

        for (Parameter p : pmap.keySet()) {
            guard = ExpressionUtil.and(guard, new NumericBooleanExpression(
                    modelRegs.get(mt.getOutput().getOutput().get(p)),
                    NumericComparator.EQ,
                    hypRegs.get(ht.getOutput().getOutput().get(p))));
        }

        return guard;
    }

    private Expression<Boolean> buildOutputTransitionGuardError(
            RALocation ml, RALocation hl, OutputTransition mt,
            OutputTransition ht, Map<Parameter, Variable> pmap) {

        Expression<Boolean> guard = ExpressionUtil.FALSE;
        assert pmap.size() == mt.getOutput().getOutput().size();
        assert pmap.size() == ht.getOutput().getOutput().size();

        for (Parameter p : pmap.keySet()) {
            Expression<Boolean> temp = new NumericBooleanExpression(
                    modelRegs.get(mt.getOutput().getOutput().get(p)),
                    NumericComparator.NE,
                    hypRegs.get(ht.getOutput().getOutput().get(p)));

            guard = ExpressionUtil.or(guard, temp);
        }

        return ExpressionUtil.and(locGuard(ml, hl), guard);
    }

    private Expression<Boolean> buildInputTransitionGuard(
            RALocation ml, RALocation hl, InputTransition mt,
            InputTransition ht, Map<Parameter, Variable> pmap) {

        Expression<Boolean> mGuard = translateGuardCondition(
                mt.getLabel(), mt.getGuard().getCondition(), modelRegs, pmap);
        Expression<Boolean> hGuard = translateGuardCondition(
                ht.getLabel(), ht.getGuard().getCondition(), hypRegs, pmap);
        Expression<Boolean> locs = locGuard(ml, hl);

        Expression<Boolean> tGuard = ExpressionUtil.and(mGuard, hGuard, locs);
        return tGuard;
    }

    private Expression<Boolean> locGuard(RALocation ml, RALocation hl) {
        return ExpressionUtil.and(
                new NumericBooleanExpression(
                        mq, NumericComparator.EQ, new Constant<>(TYPE, ml.hashCode())),
                new NumericBooleanExpression(
                        hq, NumericComparator.EQ, new Constant<>(TYPE, hl.hashCode())));
    }

    private void computeEffects(Transition mt, Transition ht,
            Map<Variable, Expression<Boolean>> effects,
            Map<Parameter, Variable> pmap) {

        computeEffectsTrans(mt, effects, pmap, modelRegs);
        computeEffectsTrans(ht, effects, pmap, hypRegs);
        computeEffectsLoc(mt.getDestination(), ht.getDestination(), effects);
    }

    private void computeEffectsLoc(RALocation ml, RALocation hl,
            Map<Variable, Expression<Boolean>> effects) {
        effects.put(mq, new Constant<>(TYPE, ml.hashCode()));
        effects.put(hq, new Constant<>(TYPE, hl.hashCode()));
    }

    private void computeEffectsTrans(Transition t,
            Map<Variable, Expression<Boolean>> effects,
            Map<Parameter, Variable> pmap,
            Map<Register, Variable> rmap) {

        for (Entry<Register, ? extends SymbolicDataValue> e
                : t.getAssignment().getAssignment()) {

            // TODO: what about constants?            
            Variable v = rmap.get(e.getKey());
            Expression expr = (e.getValue() instanceof Register)
                    ? rmap.get(e.getValue()) : pmap.get(e.getValue());

            effects.put(v, expr);
        }
    }

    private Expression<Boolean> translateGuardCondition(InputSymbol label,
            GuardExpression condition, Map<Register, Variable> regs,
            Map<Parameter, Variable> pmap) {

        Map<SymbolicDataValue, Variable> atoms = new HashMap<>();
        atoms.putAll(pmap);
        atoms.putAll(regs);

        return JContraintsUtil.toExpression(condition, atoms);
    }

    private boolean isOutputLoc(RALocation loc) {
        return !loc.getOut().isEmpty()
                && loc.getOut().iterator().next() instanceof OutputTransition;
    }

    private Valuation computeInitialState() {
        Valuation val = new Valuation();

        val.setValue(mq, new Constant<>(TYPE, model.getInitialState().hashCode()));
        val.setValue(hq, new Constant<>(TYPE, hyp.getInitialState().hashCode()));

        for (Register r : model.getRegisters()) {
            val.setValue(modelRegs.get(r), model.getInitialRegisters().get(r).getId());
        }
        for (Register r : hyp.getRegisters()) {
            val.setValue(hypRegs.get(r), new Constant<>(TYPE, 0));
        }

        return val;
    }

    private void buildVarMaps() {
        for (Register r : model.getRegisters()) {
            modelRegs.put(r, new Variable(TYPE, "m" + "_" + r.toString()));
        }
        for (Register r : hyp.getRegisters()) {
            hypRegs.put(r, new Variable(TYPE, "h" + "_" + r.toString()));
        }
    }

    private Map<Parameter, Variable> buildParMap(ParameterizedSymbol ps) {
        Map<Parameter, Variable> pmap = new HashMap<>();
        ParameterGenerator pgen = new ParameterGenerator();
        for (int i = 0; i < ps.getArity(); i++) {
            pmap.put(pgen.next(ps.getPtypes()[i]),
                    new Variable(TYPE, "" + ps.getName() + "_" + i));
        }
        return pmap;
    }

    private String getId(Transition mt, Transition ht, boolean ok) {
        return "t_" + (tId++) + (ok ? "OK" : "ERR");
    }

    private Expression<Boolean> or(Collection<Transition> trans,
            Map<Parameter, Variable> pmap, Map<Register, Variable> regs) {

        Expression<Boolean> ret = null;
        for (Transition t : trans) {
            Expression<Boolean> temp = translateGuardCondition(
                    (InputSymbol) t.getLabel(), t.getGuard().getCondition(), regs, pmap);

            ret = (ret == null) ? temp : ExpressionUtil.or(ret, temp);
        }
        return ret;
    }

    private Word<PSymbolInstance> buildCE(
            List<gov.nasa.jstateexplorer.transitionSystem.Transition> ceTrace) {

        List<TransitionTuple> tupleList = new ArrayList<>();
        for (gov.nasa.jstateexplorer.transitionSystem.Transition t : ceTrace) {
            tupleList.add(tuples.get(t.getId()));
        }

        Expression<Boolean> instCheck = stitchTogether(tupleList);

        Valuation val = new Valuation();
        Result res = solver.solve(instCheck, val);
        assert Result.SAT == res;

        //System.out.println(instCheck);
        //System.out.println(val);

        PSymbolInstance psi[] = new PSymbolInstance[ceTrace.size()];
        int i = 0;
        for (TransitionTuple t : tupleList) {
            ParameterizedSymbol ps = t.a;
            //System.out.println(t.info);

            String prefix = "__" + i + "_";

            DataValue[] dvs = new DataValue[ps.getArity()];
            for (int j = 0; j < ps.getArity(); j++) {
                String varname = prefix + ps.getName() + "_" + j;
                dvs[j] = new DataValue(ps.getPtypes()[j], val.getValue(varname));
                //System.out.println(varname + " : " + val.getValue(varname));
            }
            psi[i] = new PSymbolInstance(ps, dvs);
            i++;
        }
        return Word.fromSymbols(psi);
    }

    private Expression<Boolean> stitchTogether(List<TransitionTuple> tList) {
        int oldId = 0;
        Expression<Boolean>[] temp = new Expression[tList.size() + 1];

        // initial state
        Valuation initial = computeInitialState();
        temp[0] = ExpressionUtil.addPrefix(
                ExpressionUtil.and(
                        initialStateAsExpression(modelRegs, initial),
                        initialStateAsExpression(hypRegs, initial)
                ), "__0_");

        // transitions
        for (TransitionTuple t : tList) {
            temp[oldId+1] = ExpressionUtil.and(
                    ExpressionUtil.addPrefix(t.t.getGuard(), "__" + oldId + "_"),
                    asExpression("__" + oldId + "_", "__" + (oldId + 1) + "_", t.t.getEffects()));
            
            for (int j = 0; j < t.a.getArity(); j++) {
                Variable v = new Variable(TYPE, "__" + oldId + "_" + t.a.getName() + "_" + j);
                temp[oldId+1] = ExpressionUtil.and(temp[oldId+1],
                        new NumericBooleanExpression(v, NumericComparator.EQ, v));
            }
            oldId++;
        }

        return ExpressionUtil.and(temp);
    }

    private Expression<Boolean> asExpression(String oldPrefix, String newPrefix,
            Map<Variable, Expression<Boolean>> effects) {

        Expression<Boolean>[] temp = new Expression[modelRegs.size() + hypRegs.size()];
        int i = 0;
        for (Variable v : modelRegs.values()) {
            temp[i++] = effectAsExpression(v, oldPrefix, newPrefix, effects);
        }
        for (Variable v : hypRegs.values()) {
            temp[i++] = effectAsExpression(v, oldPrefix, newPrefix, effects);
        }
        return ExpressionUtil.and(temp);
    }

    private Expression<Boolean> initialStateAsExpression(
            Map<Register, Variable> regs, Valuation vals) {

        Expression<Boolean>[] temp
                = new Expression[regs.size()];
        int idx = 0;
        for (Entry<Register, Variable> e : regs.entrySet()) {
            Object d = vals.getValue(e.getValue());
            temp[idx++] = new NumericBooleanExpression(e.getValue(),
                    NumericComparator.EQ, new Constant<>(TYPE, d));
        }
        return ExpressionUtil.and(temp);
    }

    private Expression<Boolean> effectAsExpression(
            Variable v, String oldPrefix, String newPrefix,
            Map<Variable, Expression<Boolean>> effects) {

        Expression expr = effects.get(v);
        if (expr == null) {
            expr = v;
        }
        
        return new NumericBooleanExpression(
                ExpressionUtil.addPrefix(v, newPrefix),
                NumericComparator.EQ,
                ExpressionUtil.addPrefix(expr, oldPrefix));
    }
}
