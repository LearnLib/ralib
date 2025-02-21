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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import de.learnlib.ralib.smt.ConstraintSolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class MultipleSpecialCasesTest extends RaLibTestSuite {

    @Test
    public void testModelswithOutput() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/passport.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            teachers.put(t, new IntegerEqualityTheory(t));
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        MultiTheoryTreeOracle mto = TestUtil.createMTO(sul, ERROR,
                teachers, consts, new ConstraintSolver(), inputs);

        DataType intType = TestUtil.getType("int", loader.getDataTypes());

        ParameterizedSymbol igc = new InputSymbol(
                "IGetChallenge", new DataType[] {});

        ParameterizedSymbol icb = new InputSymbol(
                "ICompleteBAC", new DataType[] {});

         ParameterizedSymbol irf = new InputSymbol(
                "IReadFile", new DataType[] {intType});

         ParameterizedSymbol ook = new OutputSymbol(
                "OOK", new DataType[] {});

         DataValue d0 = consts.values().iterator().next();

        //****** IGetChallenge[] OOK[] ICompleteBAC[]
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(igc),
                new PSymbolInstance(ook),
                new PSymbolInstance(icb));

        //**** [s1]((OOK[] IReadFile[s1] OOK[]))
        Word<PSymbolInstance> suffix =  Word.fromSymbols(
                new PSymbolInstance(ook),
                new PSymbolInstance(irf, d0),
                new PSymbolInstance(ook));

        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);

        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        TreeQueryResult tqr = mto.treeQuery(prefix, symSuffix);
        String tree = tqr.getSdt().toString();

        logger.log(Level.FINE, "PIV: {0}", tqr.getPiv());
        logger.log(Level.FINE, "SDT: {0}", tree);

        String expectedTree = "[]-+\n" +
"  []-(s1=c1)\n" +
"   |    [Leaf+]\n" +
"   +-(s1=c3)\n" +
"   |    [Leaf+]\n" +
"   +-ANDCOMPOUND: s1[(s1!=c1), (s1!=c3)]\n" +
"        [Leaf-]\n";

        Assert.assertEquals(tree, expectedTree);
    }


}
