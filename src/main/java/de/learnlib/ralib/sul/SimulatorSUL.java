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
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SimulatorSUL extends DataWordSUL {

    private final RegisterAutomaton model;
    
    private final Constants consts;
    private final Map<DataType, Theory> teachers;
    
    private RALocation loc = null;
    private VarValuation register = null;
    private Word<PSymbolInstance> prefix = null;
    
    private static LearnLogger log = LearnLogger.getLogger(SimulatorSUL.class);
    
    public SimulatorSUL(RegisterAutomaton model, Map<DataType, Theory> teachers,
            Constants consts) {
        this.model = model;
        this.teachers = teachers;
        this.consts = consts;
    }

    @Override
    public void pre() {
        countResets(1);
        loc = this.model.getInitialState();
        register = this.model.getInitialRegisters();
        prefix = Word.epsilon();
    }

    @Override
    public void post() {
        loc = null;
        register = null;
        prefix = null;
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        log.log(Level.FINEST, "step: {0} from {1} with regs {2}", new Object[] {i, loc, register});
        prefix = prefix.append(i);
        
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
        
        OutputTransition ot = getOutputTransition(loc, register);
        PSymbolInstance out = createOutputSymbol(ot);
        prefix = prefix.append(out);
        
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
            if (!mapping.getOutput().keySet().contains(p)) {
                List<DataValue> old = computeOld(t, pval);
                DataValue dv = teachers.get(t).getFreshValue(old);
                vals[i] = new FreshValue(dv.getType(), dv.getId());
            }
            else {
                SymbolicDataValue sv = mapping.getOutput().get(p);                
                if (sv.isRegister()) {                
                    vals[i] = register.get( (Register) sv);
                }
                else if (sv.isConstant()) {
                    vals[i] = consts.get( (Constant) sv);                    
                }
                else if (sv.isParameter()) {
                    throw new UnsupportedOperationException("not supported yet.");
                }
                else {
                    throw new IllegalStateException("this case is not supported.");
                }
            }
            assert vals[i] != null;
            pval.put(p, vals[i]);
            i++;
        }
        return new PSymbolInstance(ot.getLabel(), vals);
    }
    
    private OutputTransition getOutputTransition(RALocation loc, VarValuation reg) {
        for (Transition t : loc.getOut()) {
            OutputTransition ot = (OutputTransition) t;
            if (ot.canBeEnabled(reg, consts) && ot.getDestination().isAccepting()) {
                return ot;
            }
        }
        throw new IllegalStateException("No suitable output transition.");
    }

    private List<DataValue> computeOld(DataType t, ParValuation pval) {
        Set<DataValue> set = new LinkedHashSet<>();        
        set.addAll(DataWords.valSet(prefix, t));
        for (DataValue d : pval.values()){
            if (d.getType().equals(t)) {
                set.add(d);
            }
        }    
        return new ArrayList<>(set);
    }
    
    public RALocation getLocation() {
        return loc;
    }
    
}
