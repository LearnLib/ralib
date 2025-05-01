/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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

import java.math.BigDecimal;
import java.util.Arrays;
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
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.SDT;
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
public class ConstantsSDTBranchingTest extends RaLibTestSuite {

    @Test
    public void testModelswithOutput3() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/abp.output.xml");

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

        //ParameterizedSymbol iack = new InputSymbol(
        //        "IAck", new DataType[] {intType});
        ParameterizedSymbol iin = new InputSymbol("IIn", intType);
        ParameterizedSymbol ook = new OutputSymbol("OOK");
        ParameterizedSymbol isend = new InputSymbol("ISendFrame");
        ParameterizedSymbol oframe = new OutputSymbol("OFrame", intType, intType);

        DataValue d2 = new DataValue(intType, new BigDecimal(2));
        //DataValue c1 = new DataValue(intType, BigDecimal.ZERO);

        //****** ROW:  IIn OOK ISendFrame
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(iin, d2),
                new PSymbolInstance(ook),
                new PSymbolInstance(isend));

        //**** [s1, s2]((OFrame[s1, s2]))
        Word<PSymbolInstance> suffix1 =  Word.fromSymbols(
                new PSymbolInstance(oframe, d2, d2));
        SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix, suffix1);

        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix1);

        SDT tqr = mto.treeQuery(prefix, symSuffix1);
        logger.log(Level.FINE, "SDT: {0}", tqr);

        final String expected = "[((2 == 'p1') && ('c1' == 'p2')), ((2 == 'p1') && ('c1' != 'p2')), ((2 != 'p1') && true)]";

        Branching b = mto.getInitialBranching(prefix, oframe, tqr);
        String bString = Arrays.toString(b.getBranches().values().toArray());

        Assert.assertEquals(b.getBranches().size(), 3);
        Assert.assertEquals(bString, expected);
    }

}
