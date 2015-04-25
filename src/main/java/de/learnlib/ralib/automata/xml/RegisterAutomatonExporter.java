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

package de.learnlib.ralib.automata.xml;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.bind.JAXB;


/**
 *
 * @author falk
 */
public class RegisterAutomatonExporter {
    
    private static final ObjectFactory factory = new ObjectFactory();
    
    private static RegisterAutomaton.Constants exportConstants(Constants consts) {
        RegisterAutomaton.Constants ret = factory.createRegisterAutomatonConstants();
        for (Entry<Constant, DataValue<?>> e : consts) {
            RegisterAutomaton.Constants.Constant c = factory.createRegisterAutomatonConstantsConstant();
            c.setName(e.getKey().toString());
            c.setType(e.getKey().getType().getName());
            c.setValue(e.getValue().getId().toString());
            ret.getConstant().add(c);
        }        
        return ret;
    }

    private static RegisterAutomaton.Globals exportRegisters(Collection<Register> reg, Map<String,DataType> extra) {
        RegisterAutomaton.Globals ret = factory.createRegisterAutomatonGlobals();
        for (Register r : reg) {
            RegisterAutomaton.Globals.Variable v = factory.createRegisterAutomatonGlobalsVariable();
            v.setName(r.toString());
            v.setType(r.getType().getName());
            v.setValue("0");
            ret.getVariable().add(v);
        }        
        for (Entry<String, DataType> e : extra.entrySet()) {
            RegisterAutomaton.Globals.Variable v = factory.createRegisterAutomatonGlobalsVariable();
            v.setName(e.getKey());
            v.setType(e.getValue().getName());
            v.setValue("0");
            ret.getVariable().add(v);        
        }
        return ret;
    }
    
    private static RegisterAutomaton.Alphabet.Inputs exportInputs(Collection<InputSymbol> is) {
        RegisterAutomaton.Alphabet.Inputs ret = factory.createRegisterAutomatonAlphabetInputs();
        for (InputSymbol input : is) {
            RegisterAutomaton.Alphabet.Inputs.Symbol s = factory.createRegisterAutomatonAlphabetInputsSymbol();
            s.setName(input.getName());
            int idx=1;
            for (DataType t : input.getPtypes()) {
                RegisterAutomaton.Alphabet.Inputs.Symbol.Param param =
                        factory.createRegisterAutomatonAlphabetInputsSymbolParam();
                param.setName("p" + (idx++));
                param.setType(t.getName());
                s.getParam().add(param);
            }
            ret.getSymbol().add(s);
        }        
        return ret;
    }

    private static RegisterAutomaton.Alphabet.Outputs exportOutputs(Collection<OutputSymbol> os) {
        RegisterAutomaton.Alphabet.Outputs ret = factory.createRegisterAutomatonAlphabetOutputs();
        for (OutputSymbol output : os) {
            RegisterAutomaton.Alphabet.Outputs.Symbol s = factory.createRegisterAutomatonAlphabetOutputsSymbol();
            s.setName(output.getName());
            int idx=1;
            for (DataType t : output.getPtypes()) {
                RegisterAutomaton.Alphabet.Outputs.Symbol.Param param =
                        factory.createRegisterAutomatonAlphabetOutputsSymbolParam();
                param.setName("p" + (idx++));
                param.setType(t.getName());
                s.getParam().add(param);
            }
            ret.getSymbol().add(s);
        }        
        return ret;
    }
       
    private static RegisterAutomaton.Locations exportLocations(
            de.learnlib.ralib.automata.RegisterAutomaton ra, Collection<RALocation> locs) {
        
        RegisterAutomaton.Locations ret = factory.createRegisterAutomatonLocations();
        for (RALocation loc : locs) {
            RegisterAutomaton.Locations.Location l = 
                    factory.createRegisterAutomatonLocationsLocation();
            
            l.setName(loc.getName());
            if (ra.getInitialState().equals(loc)) {
                l.setInitial("true");
            }
            ret.getLocation()   .add(l);
        }
        return ret;
    }
    
    private static RegisterAutomaton.Transitions exportTransitions(Collection<Transition> trans, Map<String, DataType> tmp) {
        RegisterAutomaton.Transitions ret = 
                factory.createRegisterAutomatonTransitions();
        
        for (Transition t : trans) {
            if (t instanceof OutputTransition) {
                ret.getTransition().add( exportOutputTransition( (OutputTransition)t, tmp ));
            }
            else {
                ret.getTransition().add( exportInputTransition( t ));
            }
        }
        return ret;
    }
    
    private static RegisterAutomaton.Transitions.Transition exportInputTransition(Transition t) {
        RegisterAutomaton.Transitions.Transition ret = 
                factory.createRegisterAutomatonTransitionsTransition();
        
        ret.setFrom(t.getSource().getName());
        ret.setTo(t.getDestination().getName());
        ret.setSymbol(t.getLabel().getName());                
        ret.setAssignments(exportAssignments(t.getAssignment()));
        String g = exportGuard(t.getGuard());
        if (g != null && g.length() > 0) {
            ret.setGuard(g);    
        }
        return ret;
    }

    private static RegisterAutomaton.Transitions.Transition exportOutputTransition(
            OutputTransition t, Map<String, DataType> tmp) {
        RegisterAutomaton.Transitions.Transition ret = 
                factory.createRegisterAutomatonTransitionsTransition();

        ret.setFrom(t.getSource().getName());
        ret.setTo(t.getDestination().getName());
        ret.setSymbol(t.getLabel().getName());  
        
        RegisterAutomaton.Transitions.Transition.Assignments assign =
                exportAssignments(t.getAssignment());
        
        //ret.setGuard(exportGuard(t.getGuard()));        

        String params = "";
        OutputMapping outMap = t.getOutput();
        ParameterGenerator pgen = new ParameterGenerator();
        int idx=1;
        for (DataType type : t.getLabel().getPtypes()) {
            Parameter p = pgen.next(type);
            if (outMap.getFreshParameters().contains(p)) {
                params += p.toString() + ",";
            }
            else {
                SymbolicDataValue out = outMap.getOutput().get(p);
                // assignments are assumed to happen before 
                // output by the parser
                if (out instanceof Register) {
                    String tmpName = "tmp_" + out.getType() + "_" + idx;
                    tmp.put(tmpName, out.getType());
                    RegisterAutomaton.Transitions.Transition.Assignments.Assign a =
                            factory.createRegisterAutomatonTransitionsTransitionAssignmentsAssign();
                    
                    a.setTo(tmpName);
                    a.setValue(out.toString());                    
                    assign.getAssign().add(a);
                    params += tmpName + ",";
                } else {
                    params += out.toString() + ",";
                }
            }
            idx++;
        }        
        
        if (params.length() > 0) {
            params = params.substring(0, params.length() -1);
            ret.setParams(params);
        }
        
        ret.setAssignments(assign);
        
        return ret;
    }
    
    private static String exportGuard(TransitionGuard guard) {
        String g = guard.toString();
        if (!g.contains("=")) {
            return null;
        }
        g = g.replaceAll("TRUE", "true");
        g = g.replaceAll("true \\&\\&", "");
        g = g.replaceAll("\\&\\& true", "");
        g = g.replaceAll("\\(", "").replaceAll("\\)", "");
        return g;
    }
    
    private static RegisterAutomaton.Transitions.Transition.Assignments exportAssignments(Assignment as) {
        RegisterAutomaton.Transitions.Transition.Assignments ret = 
                factory.createRegisterAutomatonTransitionsTransitionAssignments();
        for (Entry<Register, ? extends SymbolicDataValue> e : as.getAssignment()) {
            RegisterAutomaton.Transitions.Transition.Assignments.Assign a = 
                    factory.createRegisterAutomatonTransitionsTransitionAssignmentsAssign();
            
            if (!e.getKey().equals(e.getValue())) {
                a.setTo(e.getKey().toString());
                a.setValue(e.getValue().toString());
                ret.getAssign().add(a);
            }
        }
        return ret;
    }
    
    private static void marschall(RegisterAutomaton ra, OutputStream os) {
        JAXB.marshal(ra, os);
    }
    
    public static void wtite(de.learnlib.ralib.automata.RegisterAutomaton ra, Constants c, OutputStream os) {

        RegisterAutomaton ret = factory.createRegisterAutomaton();
        
        Set<InputSymbol>  inputs  = new HashSet<>();
        Set<OutputSymbol> outputs = new HashSet<>();
        for (Transition t : ra.getTransitions()) {
            ParameterizedSymbol ps = t.getLabel();
            if (ps instanceof InputSymbol) {
                inputs.add((InputSymbol)ps);
            }
            else {
                outputs.add((OutputSymbol) ps);
            }
        }
        
        RegisterAutomaton.Alphabet acts = factory.createRegisterAutomatonAlphabet();
        acts.setInputs(exportInputs(inputs));
        acts.setOutputs(exportOutputs(outputs));
        Map<String, DataType> tmp = new HashMap<>();
        ret.setAlphabet(acts);
        ret.setConstants(exportConstants(c));
        ret.setLocations(exportLocations(ra, ra.getStates()));
        ret.setTransitions(exportTransitions(ra.getTransitions(), tmp));
        ret.setGlobals(exportRegisters(ra.getRegisters(), tmp));
        
        marschall(ret, os);
    }
    
}
