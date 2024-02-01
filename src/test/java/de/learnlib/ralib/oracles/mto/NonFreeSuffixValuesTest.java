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
package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.repeater.RepeaterSUL.IPUT;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.OECHO;
import static de.learnlib.ralib.example.repeater.RepeaterSUL.TINT;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.repeater.RepeaterSUL;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class NonFreeSuffixValuesTest extends RaLibTestSuite {


    @Test
    public void testModelswithOutputFifo() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/fifo7.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            TypedTheory<Integer> theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });
        SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        MultiTheoryTreeOracle mto = TestUtil.createMTO(sul, ERROR,
                teachers, consts, new SimpleConstraintSolver(), inputs);

        DataType intType = TestUtil.getType("int", loader.getDataTypes());

        ParameterizedSymbol iput = new InputSymbol(
                "IPut", new DataType[] {intType});

        ParameterizedSymbol iget = new InputSymbol(
                "IGet", new DataType[] {});

         ParameterizedSymbol oget = new OutputSymbol(
                "OGet", new DataType[] {intType});

         ParameterizedSymbol ook = new OutputSymbol(
                "OOK", new DataType[] {});

         DataValue d0 = new DataValue(intType, 0);
         DataValue d1 = new DataValue(intType, 1);
         DataValue d6 = new DataValue(intType, 6);

        //****** IPut[0[int]] OOK[] IPut[1[int]] OOK[]
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(iput,d0),
                new PSymbolInstance(ook)
                ,new PSymbolInstance(iput,d1),
                new PSymbolInstance(ook)
                );

        //**** [s2, s3, s4, s5]((IPut[s1] OOK[] IPut[s2] OOK[] IGet[] OGet[s3] IGet[] OGet[s4] IGet[] OGet[s1] IGet[] OGet[s5]))
        Word<PSymbolInstance> suffix =  Word.fromSymbols(
                new PSymbolInstance(iput, d6),
                new PSymbolInstance(ook),
                new PSymbolInstance(iput,d0),
                new PSymbolInstance(ook),

                new PSymbolInstance(iget),
                new PSymbolInstance(oget,d0),
                new PSymbolInstance(iget),
                new PSymbolInstance(oget,d0),
                new PSymbolInstance(iget),
                new PSymbolInstance(oget, d6),
                new PSymbolInstance(iget),
                new PSymbolInstance(oget,d0));

        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, restrictionBuilder);

        String expectedTree = "[r1, r2]-+\n" +
                "        []-TRUE: s1\n" +
                "              []-TRUE: s2\n" +
                "                    []-(s3=r1)\n" +
                "                     |    []-(s4=r2)\n" +
                "                     |     |    []-(s5=s1)\n" +
                "                     |     |          []-(s6=s2)\n" +
                "                     |     |           |    [Leaf+]\n" +
                "                     |     |           +-(s6!=s2)\n" +
                "                     |     |                [Leaf-]\n" +
                "                     |     +-(s4!=r2)\n" +
                "                     |          []-(s5=s1)\n" +
                "                     |                []-TRUE: s6\n" +
                "                     |                      [Leaf-]\n" +
                "                     +-(s3!=r1)\n" +
                "                          []-TRUE: s4\n" +
                "                                []-(s5=s1)\n" +
                "                                      []-TRUE: s6\n" +
                "                                            [Leaf-]\n";

        checkTreeForSuffix(prefix, symSuffix, mto, expectedTree);
    }


    @Test
    public void testModelswithOutputPalindrome() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/palindrome.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            teachers.put(t, new IntegerEqualityTheory(t));
        });
        SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        MultiTheoryTreeOracle mto = TestUtil.createMTO(sul, ERROR,
                teachers, consts, new SimpleConstraintSolver(), inputs);

        DataType intType = TestUtil.getType("int", loader.getDataTypes());

        ParameterizedSymbol i4 = new InputSymbol(
                "IPalindrome4", new DataType[] {intType, intType, intType, intType});

         ParameterizedSymbol oyes = new OutputSymbol(
                "OYes", new DataType[] {});

         DataValue d0 = new DataValue(intType, 0);
         DataValue d1 = new DataValue(intType, 1);
         DataValue d2 = new DataValue(intType, 2);
         DataValue d3 = new DataValue(intType, 3);
         DataValue d4 = new DataValue(intType, 4);
         DataValue d5 = new DataValue(intType, 5);
         DataValue d6 = new DataValue(intType, 6);
         DataValue d7 = new DataValue(intType, 7);

        //******
        Word<PSymbolInstance> prefix1 = Word.fromSymbols();

        Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(i4, d0, d1, d2, d3),
                new PSymbolInstance(oyes));

        //**** []((IPalindrome4[s1, s2, s2, s1] OYes[]))
        Word<PSymbolInstance> suffix =  Word.fromSymbols(
                new PSymbolInstance(i4, d4, d5, d6, d7),
                new PSymbolInstance(oyes));

        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix2, suffix, restrictionBuilder);


        String expectedTree = "[]-+\n" +
"  []-TRUE: s1\n" +
"        []-TRUE: s2\n" +
"              []-(s3=s2)\n" +
"               |    []-(s4=s1)\n" +
"               |     |    [Leaf+]\n" +
"               |     +-(s4!=s1)\n" +
"               |          [Leaf-]\n" +
"               +-(s3!=s2)\n" +
"                    []-TRUE: s4\n" +
"                          [Leaf-]\n";

        checkTreeForSuffix(prefix1, symSuffix, mto, expectedTree);
    }

    @Test
    public void testNonFreeNonFresh() {
        Constants consts = new Constants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory theory = new IntegerEqualityTheory(TINT);
        theory.setUseSuffixOpt(true);
        teachers.put(TINT, theory);
        SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);

        RepeaterSUL sul = new RepeaterSUL(-1, 2);
        IOOracle ioOracle = new SULOracle(sul, RepeaterSUL.ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter oracle = new IOFilter(ioCache, sul.getInputSymbols());

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                oracle, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo =
                new MultiTheorySDTLogicOracle(consts, solver);

        Word<PSymbolInstance> word = Word.fromSymbols(
                new PSymbolInstance(IPUT, new DataValue(TINT, 0)),
                new PSymbolInstance(OECHO, new DataValue(TINT, 0)),
                new PSymbolInstance(IPUT, new DataValue(TINT, 1)),
                new PSymbolInstance(OECHO, new DataValue(TINT, 1)),
                new PSymbolInstance(IPUT, new DataValue(TINT, 2)),
                new PSymbolInstance(OECHO, new DataValue(TINT, 2)));


        SymbolicSuffix suffix = new SymbolicSuffix(word.prefix(2), word.suffix(4), restrictionBuilder);
        Assert.assertTrue(suffix.getFreeValues().isEmpty());

        String expectedTree = "[]-+\n" +
"  []-TRUE: s1\n" +
"        []-(s2=s1)\n" +
"              []-TRUE: s3\n" +
"                    []-(s4=s3)\n" +
"                          [Leaf-]\n";

        checkTreeForSuffix(word.prefix(2), suffix, mto, expectedTree);
    }

    private void checkTreeForSuffix(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, MultiTheoryTreeOracle mto, String expectedTree) {
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", suffix);

        TreeQueryResult tqr = mto.treeQuery(prefix, suffix);
        String tree = tqr.getSdt().toString();

        logger.log(Level.FINE, "PIV: {0}", tqr.getPiv());
        logger.log(Level.FINE, "SDT: {0}",tree);

        Assert.assertEquals(tree, expectedTree);
    }
}
