/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.tools;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.equivalence.IORandomWordOracle;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.external.ExternalTreeOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import static de.learnlib.ralib.tools.AbstractToolWithRandomWalk.OPTION_LOGGING_LEVEL;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.tools.external.SymbolConfig;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.statistics.SimpleProfiler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author falk
 */
public class External extends AbstractToolWithRandomWalk {

    private static final ConfigurationOption.StringOption OPTION_INPUTS
            = new ConfigurationOption.StringOption("inputs",
                    "input action signatures. format: i1(type,type) + i1() + ...", null, false);

    private static final ConfigurationOption.StringOption OPTION_OUTPUTS
            = new ConfigurationOption.StringOption("outputs",
                    "output action signatures. format: o2(type,type) + o2() + ...", null, false);

    private static final ConfigurationOption.StringOption OPTION_ORACLE_CMD
            = new ConfigurationOption.StringOption("cmd",
                    "oracle command", null, false);

    private static final ConfigurationOption.StringOption OPTION_QUERY_FILE
            = new ConfigurationOption.StringOption("query",
                    "file for json queries", null, false);

    private static final ConfigurationOption.StringOption OPTION_SDT_FILE
            = new ConfigurationOption.StringOption("sdt",
                    "file for json sdts", null, false);
            
    private static final ConfigurationOption[] OPTIONS = new ConfigurationOption[]{
        OPTION_ORACLE_CMD,
        OPTION_QUERY_FILE,
        OPTION_SDT_FILE,
        OPTION_LOGGING_LEVEL,
        OPTION_LOGGING_CATEGORY,
        OPTION_INPUTS,
        OPTION_OUTPUTS,
        OPTION_RANDOM_SEED,
        OPTION_TEACHERS,
        OPTION_EXPORT_MODEL,
        OPTION_USE_RWALK,
        OPTION_MAX_ROUNDS,
        OPTION_TIMEOUT,
        OPTION_RWALK_FRESH_PROB,
        OPTION_RWALK_RESET_PROB,
        OPTION_RWALK_MAX_DEPTH,
        OPTION_RWALK_MAX_RUNS,
        OPTION_RWALK_RESET,
        OPTION_USE_CEOPT        
    };

    private RegisterAutomaton model;

    private IORandomWordOracle randomWalk = null;

    private RaStar rastar;

    private long tqLearn = 0;
    private long tqTest = 0;

    private Constants consts;
    
    private ExternalTreeOracle mto;

    private final Map<String, DataType> types = new LinkedHashMap<>();
    
    private final Map<DataType, Theory> teachers = new LinkedHashMap<>();

    @Override
    public String description() {
        return "uses an external tree oracle";
    }

    @Override
    public void setup(Configuration config) throws ConfigurationException {
        super.setup(config);

        config.list(System.out);

        List<ParameterizedSymbol> inList = new ArrayList<>();
        List<ParameterizedSymbol> actList = new ArrayList<>();
        
        // create inputs
        String[] inStrings = OPTION_INPUTS.parse(config).split("\\+");
        for (String is : inStrings) {            
            SymbolConfig sc = new SymbolConfig(is, this.types);
            InputSymbol i = new InputSymbol(sc.getName(), sc.getcTypes());
            inList.add(i);            
        }        
        actList.addAll(inList);
        
        // create outputs
        String[] outStrings = OPTION_OUTPUTS.parse(config).split("\\+");
        for (String os : outStrings) {            
            SymbolConfig sc = new SymbolConfig(os, this.types);
            OutputSymbol i = new OutputSymbol(sc.getName(), sc.getcTypes());
            actList.add(i);            
        }         
        
        // create teachers
        for (String tName : teacherClasses.keySet()) {
            DataType t = types.get(tName);
            TypedTheory theory = teacherClasses.get(t.getName());
            theory.setType(t);
            if (this.useSuffixOpt) {
                theory.setUseSuffixOpt(this.useSuffixOpt);
            }
            teachers.put(t, theory);
        }

        ParameterizedSymbol[] inputSymbols = inList.toArray(new ParameterizedSymbol[] {});

        ParameterizedSymbol[] actions = actList.toArray(new ParameterizedSymbol[] {});
        System.out.println("Actions: " + Arrays.toString(actions));
        
        consts = parseConstants(config, types);
        System.out.println("Constants: " + consts);

        //final ParameterizedSymbol ERROR
        //        = new OutputSymbol("_io_err", new DataType[]{});

        //IOOracle back = new SULOracle(sulLearn, ERROR);
        //IOCache ioCache = new IOCache(back);
        //IOFilter ioOracle = new IOFilter(ioCache, inputSymbols);

        //if (useFresh) {
        //    for (Theory t : teachers.values()) {
        //        ((TypedTheory) t).setCheckForFreshOutputs(true, ioCache);
        //    }
        //}

        String cmd = OPTION_ORACLE_CMD.parse(config);
        String qfile = OPTION_QUERY_FILE.parse(config);
        String sfile = OPTION_SDT_FILE.parse(config);
        
        mto = new ExternalTreeOracle(teachers, consts, solver, cmd, qfile, sfile);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        final long timeout = this.timeoutMillis;
        TreeOracleFactory hypFactory = new TreeOracleFactory() {
            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                DataWordOracle hypOracle = new SimulatorOracle(hyp);
                if (timeout > 0L) {
                    hypOracle = new TimeOutOracle(hypOracle, timeout);
                }
                return new MultiTheoryTreeOracle(hypOracle, teachers, consts, solver);
            }
        };

        this.rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);

        if (findCounterexamples) {

            boolean drawUniformly = OPTION_RWALK_DRAW.parse(config);
            double resetProbabilty = OPTION_RWALK_RESET_PROB.parse(config);
            double freshProbability = OPTION_RWALK_FRESH_PROB.parse(config);
            long maxTestRuns = OPTION_RWALK_MAX_RUNS.parse(config);
            int maxDepth = OPTION_RWALK_MAX_DEPTH.parse(config);
            boolean resetRuns = OPTION_RWALK_RESET.parse(config);

            this.randomWalk = new IORandomWordOracle(random,
                    mto,
                    drawUniformly, // do not draw symbols uniformly 
                    resetProbabilty, // reset probability 
                    freshProbability, // prob. of choosing a fresh data value
                    maxTestRuns, // 1000 runs 
                    maxDepth, // max depth
                    consts,
                    resetRuns, // reset runs 
                    teachers,
                    inputSymbols);

        }

    }

    @Override
    public void run() {

        System.out.println("=============================== START ===============================");
        
        final String __RUN__ = "overall execution time";
        final String __LEARN__ = "learning";
        final String __SEARCH__ = "ce searching";
        final String __EQ__ = "eq tests";

        System.out.println("SYS:------------------------------------------------");
        System.out.println(model);
        System.out.println("----------------------------------------------------");

        SimpleProfiler.start(__RUN__);
        SimpleProfiler.start(__LEARN__);

//        boolean eqTestfoundCE = false;
//        ArrayList<Integer> ceLengths = new ArrayList<>();
//        ArrayList<Integer> ceLengthsShortened = new ArrayList<>();
        Hypothesis hyp = null;

        int rounds = 0;
        while (true && (maxRounds < 0 || rounds < maxRounds)) {

            rounds++;
            rastar.learn();
            hyp = rastar.getHypothesis();
            System.out.println("HYP:------------------------------------------------");
            System.out.println(hyp);
            System.out.println("----------------------------------------------------");

            SimpleProfiler.stop(__LEARN__);
            SimpleProfiler.start(__EQ__);
            DefaultQuery<PSymbolInstance, Boolean> ce = null;

            SimpleProfiler.stop(__EQ__);
            SimpleProfiler.start(__SEARCH__);
            mto.setIsLearning(false);

            if (findCounterexamples) {
                ce = null;
            }

            boolean nullCe = false;
            for (int i = 0; i < 3; i++) {

                DefaultQuery<PSymbolInstance, Boolean> ce2 = null;

                if (findCounterexamples) {
                    ce2 = this.randomWalk.findCounterExample(hyp, null);
                } else {
                    ce2 = ce;
                }

                SimpleProfiler.stop(__SEARCH__);
                System.out.println("CE: " + ce2);
                if (ce2 == null) {
                    nullCe = true;
                    break;
                }


            tqLearn = mto.getTqLearn();
            tqTest = mto.getTqTest();

                ce = (ce == null || ce.getInput().length() > ce2.getInput().length())
                        ? ce2 : ce;
            }

            if (nullCe) {
                break;
            }

            SimpleProfiler.start(__LEARN__);
            mto.setIsLearning(true);
            //ceLengths.add(ce.getInput().length());

            //ceLengthsShortened.add(ce.getInput().length());
            assert model.accepts(ce.getInput());
            assert !hyp.accepts(ce.getInput());

            rastar.addCounterexample(ce);
        }

        System.out.println("=============================== STOP ===============================");
        System.out.println(SimpleProfiler.getResults());

        for (Map.Entry<DataType, Theory> e : teachers.entrySet()) {
            System.out.println("Theory: " + e.getKey() + " -> " + e.getValue().getClass().getName());
        }

        // model
        if (hyp != null) {
            System.out.println("Locations: " + hyp.getStates().size());
            System.out.println("Transitions: " + hyp.getTransitions().size());

            // input locations + transitions            
            System.out.println("Input Locations: " + hyp.getInputStates().size());
            System.out.println("Input Transitions: " + hyp.getInputTransitions().size());

            if (this.exportModel) {
                System.out.println("exporting model to model.xml");
                try {
                    FileOutputStream fso = new FileOutputStream("model.xml");
                    RegisterAutomatonExporter.write(hyp, consts, fso);

                } catch (FileNotFoundException ex) {
                    System.out.println("... export failed");
                }
            }
        }

        // tests during learning
        // resets + inputs
        System.out.println("TQ Learning: " + tqLearn);

        // tests during search
        // resets + inputs
        System.out.println("TQ Testing: " + tqTest);

    }

    @Override
    public String help() {
        StringBuilder sb = new StringBuilder();
        for (ConfigurationOption o : OPTIONS) {
            sb.append(o.toString()).append("\n");
        }
        return sb.toString();
    }

}
