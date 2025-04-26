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
public class MultiSDTBranchingTest extends RaLibTestSuite {

    @Test
    public void testModelswithOutput() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

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

        ParameterizedSymbol ipr = new InputSymbol(
                "IPRACK", intType);

        ParameterizedSymbol inv = new InputSymbol(
                "IINVITE", intType);

        ParameterizedSymbol o100 = new OutputSymbol(
                "O100", intType);

        ParameterizedSymbol o200 = new OutputSymbol(
                "O200", intType);

        DataValue d0 = new DataValue(intType, BigDecimal.ZERO);
        DataValue d1 = new DataValue(intType, BigDecimal.ONE);

        //****** ROW: IINVITE[0[int]] O100[0[int]] IPRACK[0[int]] O200[0[int]] IINVITE[1[int]]
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(inv, d0),
                new PSymbolInstance(o100, d0),
                new PSymbolInstance(ipr, d0),
                new PSymbolInstance(o200, d0),
                new PSymbolInstance(inv, d1));

        //**** Cell: [s1]((O100[s1])) : [p5>r1,]
        //[r1]-+
        //    [-]-[(s1!=r1)]
        //     |    [-]
        //     +-(s1=r1)
        //          [+]
        Word<PSymbolInstance> suffix1 = Word.fromSymbols(
                new PSymbolInstance(o100, d0));
        SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix, suffix1);

        //**** Cell: [s1, s2, s3]((O100[s1] IPRACK[s2] O200[s3])) : []
        //[]-+
        //  [-]-[else]
        //        [-]-[else]
        //              [-]-[else]
        //                    [-]
        Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(o100, d0),
                new PSymbolInstance(ipr, d0),
                new PSymbolInstance(o200, d0));
        SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix, suffix2);

        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix 1: {0}", symSuffix1);
        logger.log(Level.FINE, "Suffix 2: {0}", symSuffix2);

        SDT tqr1 = mto.treeQuery(prefix, symSuffix1);
        SDT tqr2 = mto.treeQuery(prefix, symSuffix2);

        logger.log(Level.FINE, "SDT 1: {0}", tqr1);
        logger.log(Level.FINE, "SDT 2: {0}", tqr2);

        Branching b1 = mto.getInitialBranching(prefix, o100, tqr1);
        logger.log(Level.FINE, "B.1 initial: {0}",
                Arrays.toString(b1.getBranches().values().toArray()));
        Assert.assertEquals(b1.getBranches().size(), 2);

        b1 = mto.updateBranching(prefix, o100, b1, tqr1, tqr2);
        logger.log(Level.FINE, "B.1 updated: {0}",
                Arrays.toString(b1.getBranches().values().toArray()));
        Assert.assertEquals(b1.getBranches().size(), 2);

        Branching b2 = mto.getInitialBranching(prefix, o100, tqr2);
        logger.log(Level.FINE, "B.2 initial: {0}",
                Arrays.toString(b2.getBranches().values().toArray()));
        Assert.assertEquals(b2.getBranches().size(), 1);

        b2 = mto.updateBranching(prefix, o100, b2, tqr2, tqr1);
        logger.log(Level.FINE, "B.2 updated: {0}",
                Arrays.toString(b2.getBranches().values().toArray()));
        Assert.assertEquals(b2.getBranches().size(), 2);

    }

}
