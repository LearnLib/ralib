/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.tools;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceMC;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import static de.learnlib.ralib.tools.AbstractToolWithRandomWalk.OPTION_LOGGING_LEVEL;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author falk
 */
public class RAModelChecker implements RaLibTool {

    protected final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});  
    
    protected static final ConfigurationOption.BooleanOption OPTION_SYMBOLIC_ENGINE
            = new ConfigurationOption.BooleanOption("symbolic", "use symbolic engine", false, true);
    
//    protected static final ConfigurationOption.BooleanOption OPTION_CHECK_INCLUSION
//            = new ConfigurationOption.BooleanOption("inclusion", "check language inclusion", false, true);

    protected static final ConfigurationOption.StringOption OPTION_FILE_IMPL
            = new ConfigurationOption.StringOption("impl", "impl file", null, false);

    protected static final ConfigurationOption.StringOption OPTION_FILE_SPEC
            = new ConfigurationOption.StringOption("spec", "spec file", null, false);
    
    
    private RegisterAutomaton impl;

    private RegisterAutomaton spec;
    
    private boolean symbolic;
    
    private boolean inclusion;
    
    private Collection<ParameterizedSymbol> inputs;

    private IOOracle ioOracle;
    
    private Map<DataType, Theory> teachers;
    
    private Constants consts;
    
    @Override
    public String description() {
        return "ra mnodel checker";
    }

    @Override
    public String help() {
        return "no help for now";
    }

    @Override
    public void setup(Configuration config) throws ConfigurationException {
         config.list(System.out);
        Logger root = Logger.getLogger("");
        Level lvl = Level.FINEST;
        root.setLevel(lvl);
    
        symbolic = OPTION_SYMBOLIC_ENGINE.parse(config);
        inclusion = false; //OPTION_CHECK_INCLUSION.parse(config);
        
        String implFile = OPTION_FILE_IMPL.parse(config);
        FileInputStream fsi1;        
        try {
            fsi1 = new FileInputStream(implFile);
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        RegisterAutomatonImporter loader1 = new RegisterAutomatonImporter(fsi1);
        this.impl = loader1.getRegisterAutomaton();        
        
        String specFile = OPTION_FILE_SPEC.parse(config);
        FileInputStream fsi2;        
        try {
            fsi2 = new FileInputStream(specFile);
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        RegisterAutomatonImporter loader2 = new RegisterAutomatonImporter(fsi2);
        this.spec = loader2.getRegisterAutomaton();
        
         inputs = loader2.getActions();
         
        teachers = new LinkedHashMap<>();
        loader2.getDataTypes().stream().forEach((t) -> {
            IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        consts = loader2.getConstants();
        DataWordSUL sul = new SimulatorSUL(spec, teachers, loader2.getConstants());
        ioOracle = new SULOracle(sul, ERROR);         
    }

    @Override
    public void run() throws RaLibToolException {
        
        IOEquivalenceOracle oracle;
        if (symbolic) {
            oracle = new IOEquivalenceMC(spec, inputs, ioOracle);
            ((IOEquivalenceMC)oracle).setInclusion(inclusion);
        }
        else {
            oracle = new IOEquivalenceTest(spec, teachers, consts, true, 
                    inputs.toArray(new ParameterizedSymbol[] {}));
            
            ((IOEquivalenceTest)oracle).setInclusion(inclusion);
        }

       
        DefaultQuery<PSymbolInstance, Boolean> ce = 
                oracle.findCounterExample(impl, null);
        
        if (ce == null) {
            System.out.println("OK!");
        } 
        else {
            System.out.println("ERR: " + ce.getInput());
        }
    }
    
}
