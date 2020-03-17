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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.HypVerifier.PositiveResult;
import de.learnlib.ralib.equivalence.IOHypVerifier;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.example.ineq.BharatExampleSUL;
import de.learnlib.ralib.example.ineq.BharatExampleSUL.Actions;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author Bharat Garhewal
 */
public class LearnBharatExampleTest {

    public static final DataType doubleType = new DataType("DOUBLE", Double.class);

    public LearnBharatExampleTest() {
    }

    @Test
    public void LearnBharatExampleIO() {

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
        final ParameterizedSymbol OK
                = new OutputSymbol("_ok", new DataType[]{});
        final ParameterizedSymbol NOK
                = new OutputSymbol("_not_ok", new DataType[]{});

        final ParameterizedSymbol PUT = new InputSymbol("put", new DataType[]{doubleType,doubleType,doubleType});

        List<ParameterizedSymbol> inputList = new ArrayList<ParameterizedSymbol>();
        List<ParameterizedSymbol> outputList = new ArrayList<ParameterizedSymbol>();

        inputList.add(PUT);
        outputList.add(OK);
        outputList.add(NOK);

        final ParameterizedSymbol[] inputArray = inputList.toArray(new ParameterizedSymbol[inputList.size()]);

        List<ParameterizedSymbol> actionList = new ArrayList<ParameterizedSymbol>();
        actionList.addAll(inputList);
        actionList.addAll(outputList);
        final ParameterizedSymbol[] actionArray = actionList.toArray(new ParameterizedSymbol[actionList.size()]);

        Map<BharatExampleSUL.Actions, ParameterizedSymbol> inputs = new LinkedHashMap<BharatExampleSUL.Actions, ParameterizedSymbol>();
        inputs.put(Actions.PUT, PUT);

        Map<BharatExampleSUL.Actions, ParameterizedSymbol> outputs = new LinkedHashMap<BharatExampleSUL.Actions, ParameterizedSymbol>();
        outputs.put(Actions.OK, OK);
        outputs.put(Actions.NOK, NOK);

        final Constants consts = new Constants();

        long seed = -4750580074638681533L;
        //long seed = (new Random()).nextLong();
        System.out.println("SEED=" + seed);
        final Random random = new Random(seed);

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        class Cpr implements Comparator<DataValue<Double>> {

            public int compare(DataValue<Double> one, DataValue<Double> other) {
                return one.getId().compareTo(other.getId());
            }
        }
        teachers.put(doubleType, new DoubleInequalityTheory(doubleType));
                
       DataWordSUL sul = new BharatExampleSUL(teachers, consts, inputs, outputs);

        BasicSULOracle ioOracle = new BasicSULOracle(sul, ERROR);
        IOFilter ioFilter = new IOFilter(new IOCacheOracle(ioOracle), inputArray);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, ioOracle, teachers, consts, TestUtil.getZ3Solver());
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, TestUtil.getZ3Solver());

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), ioOracle, teachers, consts, TestUtil.getZ3Solver());
            }
        };
        
        IOHypVerifier hypVerifier = new IOHypVerifier(teachers, consts);

        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, teachers, hypVerifier, TestUtil.getZ3Solver(), actionArray);

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
                inputArray);

        // IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        // IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        // IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);

        int check = 0;
        while (true && check
                < 10) {

            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();
            System.out.println("HYP:------------------------------------------------");
            System.out.println(hyp);
            System.out.println("----------------------------------------------------");

            DefaultQuery<PSymbolInstance, Boolean> ce
                    = iowalk.findCounterExample(hyp, null);

            System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }

            rastar.addCounterexample(ce);

        }
        
        RegisterAutomaton hyp = rastar.getHypothesis();

        PSymbolInstance inp = new PSymbolInstance(PUT, new DataValue(doubleType, 1.0), new DataValue(doubleType, 2.0), new DataValue(doubleType, 2.0));
        PSymbolInstance out = new PSymbolInstance(NOK);
//        Word<PSymbolInstance> badTrace = Word.fromSymbols(inp, new PSymbolInstance(NOK));
//        GeneralizedSymbolicSuffix gsuffix = GeneralizedSymbolicSuffix.fullSuffix(Word.fromSymbols(PUT, NOK), teachers);
        
        PositiveResult res = hypVerifier.isCEForHyp(Word.fromSymbols(inp, out), hyp);
        Assert.assertNull(res);

        System.out.println(
                "LAST:------------------------------------------------");
        System.out.println(hyp);

        System.out.println(
                "----------------------------------------------------");

        System.out.println(
                "Seed:" + seed);
        System.out.println(
                "SUL resets: " + sul.getResets());
        System.out.println(
                "SUL inputs: " + sul.getInputs());
        System.out.println(
                "Rounds: " + check);

    }
}
