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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import de.learnlib.ralib.data.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
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
public class UntypedBranchingTest extends RaLibTestSuite {

    public UntypedBranchingTest() {
    }


    @Test
    public void testBranching() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/login.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
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

        ParameterizedSymbol reg = new InputSymbol(
                "IRegister", intType, intType);

        ParameterizedSymbol log = new InputSymbol(
                "ILogin", intType, intType);

        ParameterizedSymbol ok = new OutputSymbol(
                "OOK");

        DataValue u = new DataValue(intType, BigDecimal.ZERO);
        DataValue p = new DataValue(intType, BigDecimal.ONE);

        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(reg, u, p),
                new PSymbolInstance(ok));

        Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(log, u, p),
                new PSymbolInstance(ok));

        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);

        logger.log(Level.FINE, "{0}", prefix);
        logger.log(Level.FINE, "{0}", suffix);
        logger.log(Level.FINE, "{0}", symSuffix);

        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        logger.log(Level.FINE, "SDT: {0}", res.sdt());

        SDT sdt = res.sdt();

        ParameterGenerator pgen = new ParameterGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        Parameter p1 = pgen.next(intType);
        Parameter p2 = pgen.next(intType);
        Register r1 = rgen.next(intType);
        Register r2 = rgen.next(intType);

        SDTRelabeling map = new SDTRelabeling();

        sdt = sdt.relabel(map);

        Branching bug2 = mto.getInitialBranching(prefix, log);
        bug2 = mto.updateBranching(prefix, log, bug2, sdt);

        // This set had only one word, there should be three
        Assert.assertEquals(bug2.getBranches().size(), 3);

    }

}
