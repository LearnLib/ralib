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
package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.learnlib.ralib.smt.SMTUtils;
import gov.nasa.jpf.constraints.api.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.dt.DT;
import de.learnlib.ralib.dt.DTHyp;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
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

    private static Logger LOGGER = LoggerFactory.getLogger(AutomatonBuilder.class);

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
        LocationComponent src_c = this.components.get(src_id);

//        if (src_c == null && automaton instanceof DTHyp)
//        	return;

        // locations
        RALocation src_loc = this.locations.get(src_id);
        RALocation dest_loc = this.locations.get(dest_id);

        // action
        ParameterizedSymbol action = r.getPrefix().lastSymbol().getBaseSymbol();

        // guard
        Branching b = src_c.getBranching(action);
//        System.out.println("b.getBranches is  " + b.getBranches().toString());
//        System.out.println("getting guard for  " + r.getPrefix().toString());
        Expression<Boolean> guard = b.getBranches().get(r.getPrefix());
        if (guard == null) {
        	guard = findMatchingGuard(dest_id, src_c.getPrimePrefix().getParsInVars(), b.getBranches(), consts);
        }

        // TODO: better solution
        // guard is null because r is transition from a short prefix
        if (automaton instanceof DTHyp && guard == null)
        	return;

        if (guard == null) {
        	assert true;
        }
        assert guard!=null;

        // assignment
        VarMapping assignments = new VarMapping();
        int max = DataWords.paramLength(DataWords.actsOf(src_id));
        PIV parsInVars_Src = src_c.getPrimePrefix().getParsInVars();
        PIV parsInVars_Row = r.getParsInVars();
        VarMapping remapping = dest_c.getRemapping(r);

//        LOGGER.trace(Category.EVENT, "PIV ROW: {}", parsInVars_Row);
//        LOGGER.trace(Category.EVENT, "PIV SRC: {}", parsInVars_Src);
//        LOGGER.trace(Category.EVENT, "REMAP: {}", remapping);

        //System.out.println(parsInVars_Row);
        for (Entry<Parameter, Register> e : parsInVars_Row) {
            // param or register
            Parameter p = e.getKey();
            // remapping is null for prime rows ...
            Register rNew = (remapping == null) ? e.getValue() : (Register) remapping.get(e.getValue());
            if (p.getId() > max) {
                Parameter pNew = new Parameter(p.getDataType(), p.getId() - max);
                assignments.put(rNew, pNew);
            } else {
                Register rOld = parsInVars_Src.get(p);
                assert rOld != null;
                assignments.put(rNew, rOld);
            }
        }
        Assignment assign = new Assignment(assignments);
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

    public static Expression<Boolean> findMatchingGuard(Word<PSymbolInstance> dw, PIV piv, Map<Word<PSymbolInstance>, Expression<Boolean>> branches, Constants consts) {
    	ParValuation pars = new ParValuation(dw);
    	VarValuation vars = DataWords.computeVarValuation(new ParValuation(dw.prefix(dw.length() - 1)), piv);
    	for (Expression<Boolean> g : branches.values()) {
    		if (g.evaluateSMT(SMTUtils.compose(vars, pars, consts))) {
    			return g;
    		}
    	}
    	return null;
    }

}
