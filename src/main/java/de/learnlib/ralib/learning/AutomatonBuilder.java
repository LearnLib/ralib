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
package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.smt.ReplacingValuesVisitor;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

/**
 * Constructs Register Automata from observation tables
 *
 * @author falk
 */
public class AutomatonBuilder {

    private final Map<Word<PSymbolInstance>, LocationComponent> components;

    private final Map<Word<PSymbolInstance>, RALocation> locations = new LinkedHashMap<>();

    private final Hypothesis automaton;

    protected final Constants consts;

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomatonBuilder.class);

    public AutomatonBuilder(Map<Word<PSymbolInstance>, LocationComponent> components, Constants consts) {
        this.consts = consts;
        this.components = components;
        this.automaton = new Hypothesis(consts);
    }

    public AutomatonBuilder(Map<Word<PSymbolInstance>, LocationComponent> components, Constants consts, DT dt) {
    	this.consts = consts;
    	this.components = components;
    	this.automaton = new DTHyp(consts, dt);
    }

    public Hypothesis toRegisterAutomaton() {
        LOGGER.debug(Category.EVENT, "computing hypothesis");
        computeLocations();
        computeTransitions();
        return this.automaton;
    }

    private void computeLocations() {
    	LocationComponent c = components.get(RaStar.EMPTY_PREFIX);
        LOGGER.debug(Category.EVENT, "{0}", c);
        RALocation loc = this.automaton.addInitialState(c.isAccepting());
        this.locations.put(RaStar.EMPTY_PREFIX, loc);
        this.automaton.setAccessSequence(loc, RaStar.EMPTY_PREFIX);

        for (Entry<Word<PSymbolInstance>, LocationComponent> e : this.components.entrySet()) {
            if (!e.getKey().equals(RaStar.EMPTY_PREFIX)) {
                LOGGER.debug(Category.EVENT, "{0}", e.getValue());
                loc = this.automaton.addState(e.getValue().isAccepting());
                this.locations.put(e.getKey(), loc);
                this.automaton.setAccessSequence(loc, e.getKey());
            }
        }
    }

    private void computeTransitions() {
        for (LocationComponent c : components.values()) {
            computeTransition(c, c.getPrimePrefix());
            for (PrefixContainer r : c.getOtherPrefixes()) {
                computeTransition(c, r);
            }
        }
    }


    private void computeTransition(LocationComponent dest_c, PrefixContainer r) {
        if (r.getPrefix().length() < 1) {
            return;
        }

        LOGGER.debug(Category.EVENT, "computing transition: {1} to {0}", new Object[]{dest_c, r});

        Word<PSymbolInstance> dest_id = dest_c.getAccessSequence();
        Word<PSymbolInstance> src_id = r.getPrefix().prefix(r.getPrefix().length() -1);

        assert src_id != null;
        LocationComponent src_c = null;
        for (LocationComponent c : this.components.values()) {
            if (c.getPrimePrefix().getPrefix().equals(src_id) || c.getOtherPrefixes().stream().map(PrefixContainer::getPrefix).toList().contains(src_id)) {
                src_id = c.getAccessSequence();
                src_c = c;
                break;
            }
        }
        //this.components.get(src_id);
        assert src_c != null;

//        if (src_c == null && automaton instanceof DTHyp)
//        	return;

        // locations
        RALocation src_loc = this.locations.get(src_id);
        RALocation dest_loc = this.locations.get(dest_id);

        // action
        ParameterizedSymbol action = r.getPrefix().lastSymbol().getBaseSymbol();

        // guard
        Branching b = src_c.getBranching(action);
        //System.out.println("b.getBranches is  " + b.getBranches().toString());
        //System.out.println("getting guard for  " + r.getPrefix().toString());
        Expression<Boolean> guard = b.getBranches().get(r.getPrefix());
        //System.out.println("assignment: " + src_c.getPrimePrefix().getAssignment());
        if (guard == null) {
            guard = findMatchingGuard(dest_id, src_c.getPrimePrefix().getAssignment(), b.getBranches(), consts);
        }

        ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
        guard = rvv.apply(guard, src_c.getPrimePrefix().getAssignment());

        // TODO: better solution
        // guard is null because r is transition from a short prefix
        if (automaton instanceof DTHyp && guard == null)
            return;

        assert true;
        assert guard != null;

        // assignment
        RegisterAssignment srcAssign = src_c.getPrimePrefix().getAssignment();
        RegisterAssignment destAssign = dest_c.getPrimePrefix().getAssignment();
        Bijection<DataValue> remapping = dest_c.getRemapping(r);
        Assignment assign = computeAssignment(r.getPrefix(), srcAssign, destAssign, remapping);

        //System.out.println(assign);

        // create transition
        Transition  t = createTransition(action, guard, src_loc, dest_loc, assign);
        if (t != null) {
            LOGGER.debug(Category.EVENT, "computed transition {0}", t);
            this.automaton.addTransition(src_loc, action, t);
            this.automaton.setTransitionSequence(t, r.getPrefix());
        }
    }

    protected Transition createTransition(ParameterizedSymbol action, Expression<Boolean> guard,
            RALocation src_loc, RALocation dest_loc, Assignment assign) {
        return new Transition(action, guard, src_loc, dest_loc, assign);
    }

    public static Expression<Boolean> findMatchingGuard(Word<PSymbolInstance> dw, RegisterAssignment div, Map<Word<PSymbolInstance>, Expression<Boolean>> branches, Constants consts) {
    	//System.out.println("findMatchingGuard: " + div);
        ParameterValuation pars = ParameterValuation.fromPSymbolWord(dw);
    	//RegisterValuation vars = div.registerValuation();
    	for (Expression<Boolean> g : branches.values()) {
    		if (g.evaluateSMT(SMTUtil.compose(pars, consts))) {
    			return g;
    		}
    	}
    	return null;
    }

    public static Assignment computeAssignment(Word<PSymbolInstance> prefix, RegisterAssignment srcAssign,
                                               RegisterAssignment destAssign, Bijection<DataValue> remapping) {

        VarMapping<Register, SymbolicDataValue> assignments = new VarMapping<>();
        ParameterizedSymbol action = prefix.lastSymbol().getBaseSymbol();
        DataValue[] pvals = DataWords.valsOf(prefix);
        for (Entry<DataValue, DataValue> e : remapping.entrySet()) {
            Register rNew = destAssign.get(e.getValue());
            assert rNew != null;
            if (srcAssign.containsKey(e.getKey())) {
                // has been stored in a register before => copy register to register
                Register rOld = srcAssign.get(e.getKey());
                assert rOld != null;
                assignments.put(rNew, rOld);
            } else {
                // has not been stored before => copy parameter to register
                int id = 0;
                for (int i = 0; i < action.getArity(); i++) {
                    if (pvals[pvals.length-action.getArity()+i].equals(e.getKey())) {
                        id = i;
                        break;
                    }
                }
                Parameter pNew = new Parameter(e.getKey().getDataType(), id + 1);
                assert pNew.getId() > 0;
                assignments.put(rNew, pNew);
            }
        }
        return new Assignment(assignments);
    }

}
