/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.sul;

import de.learnlib.api.SULException;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 * @author falk
 */
public class SimulatorSUL implements DataWordSUL {

    private final RegisterAutomaton model;
    private final Set<ParameterizedSymbol> inputs;
    
    private final Constants consts;
    private final Map<DataType, Theory> teachers;
    
    private RALocation loc = null;
    private VarValuation register = null;

    private static LearnLogger log = LearnLogger.getLogger(SimulatorSUL.class);
    
    public SimulatorSUL(RegisterAutomaton model, Map<DataType, Theory> teachers,
            Constants consts, ParameterizedSymbol[] inputs) {
        this.model = model;
        this.teachers = teachers;
        this.consts = consts;
        this.inputs = new HashSet<>(Arrays.asList(inputs));
    }

    @Override
    public void pre() {
        loc = this.model.getInitialState();
        register = new VarValuation();
    }

    @Override
    public void post() {
        loc = null;
        register = null;
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        log.log(Level.FINEST, "step: {0} from {1}", new Object[] {i, loc});
        boolean found = false;
        for (Transition t : this.model.getTransitions(loc, i.getBaseSymbol())) {
            ParValuation pval = new ParValuation(i);
            if (t.isEnabled(register, pval, consts)) {
                found = true;
                register = t.execute(register, pval, consts);
                loc = t.getDestination();
                break;
            }            
        }
        
        if (!found) {
            throw new IllegalStateException();
        }
        
        Transition t = getOutputTransition(loc, register);
        OutputTransition ot = (OutputTransition) t;
        PSymbolInstance out = createOutputSymbol(ot);
        
        register = ot.execute(register, new ParValuation(out), consts);
        loc = ot.getDestination();
        return out;
    }

    private PSymbolInstance createOutputSymbol(OutputTransition ot) {
        ParameterizedSymbol ps = ot.getLabel();
        OutputMapping mapping = ot.getOutput();
        DataValue[] vals = new DataValue[ps.getArity()];
        SymbolicDataValueGenerator.ParameterGenerator pgen = 
                new SymbolicDataValueGenerator.ParameterGenerator();
        ParValuation pval = new ParValuation();
        int i = 0;
        for (DataType t : ps.getPtypes()) {
            Parameter p = pgen.next(t);
            if (mapping.getFreshParameters().contains(p)) {
                List<DataValue> old = computeOld(t, pval);
                vals[i] = teachers.get(t).getFreshValue(old);
            }
            else {
                vals[i] = register.get( (Register) mapping.getOutput().get(p));
            }
            pval.put(p, vals[i]);
            i++;
        }
        return new PSymbolInstance(ot.getLabel(), vals);
    }
    
    private Transition getOutputTransition(RALocation loc, VarValuation reg) {
        for (Transition t : loc.getOut()) {
            if (t.isEnabled(reg, new ParValuation(), consts)) {
                return t;
            }
        }
        throw new IllegalStateException("No suitable output transition.");
    }

    private List<DataValue> computeOld(DataType t, ParValuation pval) {
        Set<DataValue> set = new HashSet<>();
        for (DataValue d : register.values()){
            if (d.getType().equals(t)) {
                set.add(d);
            }
        }
        for (DataValue d : pval.values()){
            if (d.getType().equals(t)) {
                set.add(d);
            }
        }    
        return new ArrayList<>(set);
    }
    
}