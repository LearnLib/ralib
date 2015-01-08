/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.automata.xml;


import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.ElseGuard;
import de.learnlib.ralib.automata.guards.IfGuard;
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
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ConstantGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXB;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.SimpleAlphabet;

/**
 *
 * @author falk
 */
public class RegisterAutomatonLoader {

    private final Map<String, ParameterizedSymbol> sigmaMap = new HashMap<>();
    private final Map<ParameterizedSymbol, String[]> paramNames = new HashMap<>();
    private final Map<String, RALocation> stateMap = new HashMap<>();
    private final Map<String, Constant> constMap = new HashMap<>();
    private final Map<String, Register> regMap = new HashMap<>();   
    private final Map<String, DataType> typeMap = new HashMap<>();
    
    private final VarValuation initialRegs = new VarValuation();
    private final Constants consts = new Constants();
    
    private MutableRegisterAutomaton iora;

    public RegisterAutomatonLoader(InputStream is) {
        loadModel(is);
    }

    public de.learnlib.ralib.automata.RegisterAutomaton getRegisterAutomaton() {
        return this.iora;
    }

    private void loadModel(InputStream is) {

        RegisterAutomaton a = unmarschall(is);

        Alphabet sigma = getAlphabet(a.getAlphabet());
        
        getConstants(a.getConstants());
        getRegisters(a.getGlobals());

        iora = new MutableRegisterAutomaton(consts, initialRegs);

        // create loc map
        for (RegisterAutomaton.Locations.Location l : a.getLocations().getLocation()) {
            if (l.getInitial() != null && l.getInitial().equals("true")) {
                stateMap.put(l.getName(), iora.addInitialState());
            } else {
                stateMap.put(l.getName(), iora.addState());
            }
        }

        // transitions
        for (RegisterAutomaton.Transitions.Transition t : a.getTransitions().getTransition()) {

            // read basic data
            ParameterizedSymbol ps = sigmaMap.get(t.getSymbol());
            RALocation from = stateMap.get(t.getFrom());
            RALocation to = stateMap.get(t.getTo());
            String[] pnames = paramNames.get(ps);
            if (t.getParams() != null) {
                pnames = t.getParams().split(",");
            }
            Map<String, Parameter> paramMap = paramMap(ps, pnames);
            
            // guard
            String gstring = t.getGuard();
            TransitionGuard p = new ElseGuard();            
            if (gstring != null) {
                Map<String, SymbolicDataValue> map = buildValueMap(
                        constMap, regMap, paramMap);
                ExpressionParser parser = new ExpressionParser(gstring, map);
                p = new IfGuard(parser.getPredicate());
            }
            
            // assignment
            VarMapping<Register, SymbolicDataValue> assignments = new VarMapping<>();
            if (t.getAssignments() != null) {
                for (RegisterAutomaton.Transitions.Transition.Assignments.Assign ass : 
                        t.getAssignments().getAssign()) {
                    Register left = regMap.get(ass.to);
                    SymbolicDataValue right; 
                    if (paramMap.containsKey(ass.value)) {                        
                        right = paramMap.get(ass.value);
                    }
                    else if (constMap.containsKey(ass.value)) {
                        right = constMap.get(ass.value);
                    }
                    else {
                        right = regMap.get(ass.value);                        
                    }
                    assignments.put(left, right);
                }
            }
            Assignment assign = new Assignment(assignments);

            // output
            if (((String) ps.getName()).startsWith("O")) {

                VarMapping<Parameter, SymbolicDataValue> outputs = new VarMapping<>();
                for (String s : pnames) {
                    Parameter param = paramMap.get(s);
                    SymbolicDataValue source = null;
                    // check if there was an assignment,
                    // these seem to be meant to 
                    // happen before the output
                    if (regMap.containsKey(s)) {
                        Register r = regMap.get(s);
                        source = assignments.containsKey(r) ?
                                assignments.get(r) : r;
                    } else if (constMap.containsKey(s)) {
                        source = constMap.get(s);
                    } else {
                        throw new IllegalStateException("No source for output parameter.");
                    }
                    outputs.put(param, source);
                }
                
                OutputMapping outMap = new OutputMapping(outputs);
                OutputTransition tOut = new OutputTransition(outMap, ps, from, to, assign);
                iora.addTransition(from, ps, tOut);
                System.out.println("Loading: " + tOut);
            } // input
            else {
                System.out.println("Guard: " + gstring);
                InputTransition tIn = new InputTransition(p, ps, from, to, assign);
                System.out.println("Loading: " + tIn);
                iora.addTransition(from, ps, tIn);
            }
        }
    }

    private Alphabet<ParameterizedSymbol> getAlphabet(RegisterAutomaton.Alphabet a) {
        Alphabet<ParameterizedSymbol> ret = new SimpleAlphabet<>();
        for (RegisterAutomaton.Alphabet.Inputs.Symbol s : a.getInputs().getSymbol()) {
            int pcount = s.getParam().size();
            String[] pNames = new String[pcount];
            DataType[] pTypes = new DataType[pcount];
            int idx = 0;
            for (RegisterAutomaton.Alphabet.Inputs.Symbol.Param p : s.getParam()) {
                pNames[idx] = p.name;
                pTypes[idx] = getOrCreateType(p.type);
                idx++; 
            }
            String sName = s.getName().startsWith("I") ? s.getName() : "I" + s.getName();
            ParameterizedSymbol ps = new ParameterizedSymbol(sName, pTypes);
            ret.add(ps);
            sigmaMap.put(s.getName(), ps);
            paramNames.put(ps, pNames);
            System.out.println("Loading: " + ps);
        }
        for (RegisterAutomaton.Alphabet.Outputs.Symbol s : a.getOutputs().getSymbol()) {
            int pcount = s.getParam().size();
            String[] pNames = new String[pcount];
            DataType[] pTypes = new DataType[pcount];
            int idx = 0;
            for (RegisterAutomaton.Alphabet.Outputs.Symbol.Param p : s.getParam()) {
                pNames[idx] = p.name;
                pTypes[idx] = getOrCreateType(p.type);
                idx++; 
            }
            String sName = s.getName().startsWith("O") ? s.getName() : "O" + s.getName();
            ParameterizedSymbol ps = new ParameterizedSymbol(sName, pTypes);
            ret.add(ps);
            sigmaMap.put(s.getName(), ps);
            paramNames.put(ps, pNames);
            System.out.println("Loading: " + ps);
        }
        return ret;
    }

    private void getConstants(RegisterAutomaton.Constants xml) {
        ConstantGenerator cgen = new ConstantGenerator();
        for (RegisterAutomaton.Constants.Constant def : xml.getConstant()) {
            DataType type = getOrCreateType(def.type);
            Constant c = cgen.next(type);            
            constMap.put(def.value, c);
            constMap.put(def.name, c);
            System.out.println(def.name + " ->" + c);
            DataValue dv = new DataValue(type, Integer.parseInt(def.value));
            consts.put(c, dv);
        }
        System.out.println("Loading: " + consts);
    }

    private void getRegisters(RegisterAutomaton.Globals g) {
        RegisterGenerator rgen = new RegisterGenerator();
        for (RegisterAutomaton.Globals.Variable def : g.getVariable()) {            
            DataType type = getOrCreateType(def.type);
            Register r = rgen.next(type);
            regMap.put(def.name, r);
            System.out.println(def.name + " ->" + r);
            DataValue dv = new DataValue(type, Integer.parseInt(def.value));
            initialRegs.put(r, dv);
        }
        System.out.println("Loading: " + initialRegs);
    }

    private RegisterAutomaton unmarschall(InputStream is) {
        return JAXB.unmarshal(is, RegisterAutomaton.class);
    }

    private DataType getOrCreateType(String name) {
        DataType t = typeMap.get(name);
        if (t == null) {
            t = new DataType(name, Integer.class) {};
            typeMap.put(name, t);
        }
        return t;
    }
    
    private Map<String, SymbolicDataValue> buildValueMap(
            Map<String, ? extends SymbolicDataValue> ... maps) {        
        Map<String, SymbolicDataValue> ret = new HashMap<>();
        for (Map<String, ? extends SymbolicDataValue> m : maps) {
            ret.putAll(m);
        }
        return ret;
    }
    
    private Map<String, Parameter> paramMap(
            ParameterizedSymbol ps, String ... pNames) {
        
        if (pNames == null) {
            pNames = paramNames.get(ps);
        }        
        
        Map<String, Parameter> ret = new HashMap<>();
        ParameterGenerator pgen = new ParameterGenerator();
        int idx = 0;
        for (String name : pNames) {
            ret.put(name, pgen.next(ps.getPtypes()[idx]));
            idx++;
        }
        return ret;
    }
    
}
