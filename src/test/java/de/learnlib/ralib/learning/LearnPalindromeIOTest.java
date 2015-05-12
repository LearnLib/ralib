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
package de.learnlib.ralib.learning;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheoryMS;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class LearnPalindromeIOTest {

    public LearnPalindromeIOTest() {
    }

    @Test
    public void learnLoginExampleIO() {

        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.INFO);
//            h.setFilter(new CategoryFilter(EnumSet.of(
//                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
//                    Category.EVENT, Category.PHASE, Category.MODEL)));
        }

        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});

        RegisterAutomatonImporter loader = new RegisterAutomatonImporter(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/palindrome.xml"));
//                       "/de/learnlib/ralib/automata/xml/sip.xml"));


        RegisterAutomaton model = loader.getRegisterAutomaton();
        System.out.println("SYS:------------------------------------------------");
        System.out.println(model);
        System.out.println("----------------------------------------------------");

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();

        long seed = -4888188516884158586L;
        //long seed = (new Random()).nextLong();
        System.out.println("SEED=" + seed);
        final Random random = new Random(seed);
        
        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        for (final DataType t : loader.getDataTypes()) {
            teachers.put(t, new EqualityTheoryMS<Integer>(true) {
                @Override
                public DataValue getFreshValue(List<DataValue<Integer>> vals) {
                    //System.out.println("GENERATING FRESH: " + vals.size());
                    int dv = -1;
                    for (DataValue<Integer> d : vals) {
                        dv = Math.max(dv, d.getId());
                    }
                        
                    return new DataValue(t, dv + 1);
                }
            });
        }

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);

        //SimulatorOracle oracle = new SimulatorOracle(model);
        
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts);

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts);
            }
        };

        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);

            IOEquivalenceTest ioEquiv = new IOEquivalenceTest(
                    model, teachers, consts, true, actions);
        
        IORandomWalk iowalk = new IORandomWalk(random,
                sul,
                false, // do not draw symbols uniformly 
                0.1, // reset probability 
                0.8, // prob. of choosing a fresh data value
                10000, // 1000 runs 
                100, // max depth
                consts,
                false, // reset runs 
                teachers,
                inputs);
        
        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);                        
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);
                        
                        
        int check = 0;
        while (true && check < 100) {
            
            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();
            System.out.println("HYP:------------------------------------------------");
            System.out.println(hyp);
            System.out.println("----------------------------------------------------");

              
            DefaultQuery<PSymbolInstance, Boolean> _ce = 
                    ioEquiv.findCounterExample(hyp, null);

            if (_ce != null) {
                System.out.println("EQ-TEST found counterexample: " + _ce);
            } else {
                System.out.println("EQ-TEST did not find counterexample!");                
            }

            DefaultQuery<PSymbolInstance, Boolean> ce = 
                    iowalk.findCounterExample(hyp, null);
           
            System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }

//            ce = loops.optimizeCE(ce.getInput(), hyp);
//            System.out.println("Shorter CE: " + ce);
//            ce = asrep.optimizeCE(ce.getInput(), hyp);
//            System.out.println("New Prefix CE: " + ce);           
//            ce = pref.optimizeCE(ce.getInput(), hyp);
//            System.out.println("Prefix of CE is CE: " + ce);
            
            assert model.accepts(ce.getInput());
            assert !hyp.accepts(ce.getInput());
            
            rastar.addCounterexample(ce);

        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        System.out.println("LAST:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

        System.out.println("Seed:" + seed);
        System.out.println("IO-Oracle MQ: " + ioOracle.getQueryCount());
        System.out.println("SUL resets: " + sul.getResets());
        System.out.println("SUL inputs: " + sul.getInputs());
        System.out.println("Rounds: " + check);
        
    }
}
