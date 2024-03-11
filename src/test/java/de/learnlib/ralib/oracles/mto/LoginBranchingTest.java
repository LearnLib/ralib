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

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.automatalib.serialization.xml.ra.RegisterAutomatonImporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import net.automatalib.automaton.ra.impl.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.symbol.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class LoginBranchingTest extends RaLibTestSuite {

    @Test
    public void testBranching() {

        RegisterAutomatonImporter loader = TestUtil.getLoader("/de/learnlib/ralib/automata/xml/login_typed.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(new ParameterizedSymbol[] {});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            teachers.put(t, new IntegerEqualityTheory(t));
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        MultiTheoryTreeOracle mto = TestUtil.createMTO(sul, ERROR, teachers, consts, new SimpleConstraintSolver(),
                inputs);

        DataType uid = TestUtil.getType("uid", loader.getDataTypes());
        DataType pwd = TestUtil.getType("pwd", loader.getDataTypes());

        ParameterizedSymbol reg = new InputSymbol("IRegister", new DataType[] { uid, pwd });

        ParameterizedSymbol log = new InputSymbol("ILogin", new DataType[] { uid, pwd });

        ParameterizedSymbol ok = new OutputSymbol("OOK", new DataType[] {});

        DataValue u = new DataValue(uid, 0);
        DataValue p = new DataValue(pwd, 0);

        Word<PSymbolInstance> prefix = Word.fromSymbols(new PSymbolInstance(reg, new DataValue[] { u, p }),
                new PSymbolInstance(ok, new DataValue[] {}));

        Word<PSymbolInstance> suffix = Word.fromSymbols(new PSymbolInstance(log, new DataValue[] { u, p }),
                new PSymbolInstance(ok, new DataValue[] {}));

        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);

        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Conc. Suffix: {0}", suffix);
        logger.log(Level.FINE, "Sym. Suffix: {0}", symSuffix);

        TreeQueryResult tqr = mto.treeQuery(prefix, symSuffix);
        logger.log(Level.FINE, "PIV: {0}", tqr.getPiv());
        logger.log(Level.FINE, "SDT: {0}", tqr.getSdt());

        // initial branching bug
        // Regression: Why does the last word in the set have a password val. of 2

        Branching bug1 = mto.getInitialBranching(prefix, log, tqr.getPiv(), tqr.getSdt());
        final String expectedKeyset = "[IRegister[0[uid], 0[pwd]] OOK[] ILogin[0[uid], 0[pwd]],"
                + " IRegister[0[uid], 0[pwd]] OOK[] ILogin[0[uid], 1[pwd]],"
                + " IRegister[0[uid], 0[pwd]] OOK[] ILogin[1[uid], 1[pwd]]]";

        String keyset = Arrays.toString(bug1.getBranches().keySet().toArray());
        Assert.assertEquals(keyset, expectedKeyset);

        // updated branching bug
        // Regression: This keyset has only one word, there should be three.

        Branching bug2 = mto.getInitialBranching(prefix, log, new PIV());
        bug2 = mto.updateBranching(prefix, log, bug2, tqr.getPiv(), tqr.getSdt());
        String keyset2 = Arrays.toString(bug2.getBranches().keySet().toArray());
        Assert.assertEquals(keyset2, expectedKeyset);

    }

    /*
     * Test that updating a refined branching with a coarse SDT does not change it.
     */
    @Test
    public void testUpdateRefinedBranching() {

        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        Word<PSymbolInstance> prefix = Word
                .fromLetter(new PSymbolInstance(I_REGISTER, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1)));

        SymbolicSuffix suffix1 = new SymbolicSuffix(prefix,
                Word.fromLetter(new PSymbolInstance(I_LOGIN, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1))));

        TreeQueryResult tqr1 = mto.treeQuery(prefix, suffix1);

        SymbolicSuffix suffix2 = new SymbolicSuffix(prefix,
                Word.fromSymbols(new PSymbolInstance(I_LOGIN, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1)),
                        new PSymbolInstance(I_REGISTER, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1))));

        TreeQueryResult tqr2 = mto.treeQuery(prefix, suffix2);

        Branching initial = mto.getInitialBranching(prefix, I_LOGIN, new PIV());
        Branching finer = mto.updateBranching(prefix, I_LOGIN, initial, tqr1.getPiv(), tqr1.getSdt());
        Branching actual = mto.updateBranching(prefix, I_LOGIN, finer, tqr1.getPiv(), tqr2.getSdt());
        Assert.assertEquals(actual.getBranches().toString(), finer.getBranches().toString());
    }

    /*
     * Test that initializing a branching with given SDTs produces the same results
     * as initializing the branching with no SDTs, and then updating it.
     */
    @Test
    public void testInitialBranchingWithSDTs() {
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        Word<PSymbolInstance> prefix = Word
                .fromLetter(new PSymbolInstance(I_REGISTER, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1)));

        SymbolicSuffix suffix1 = new SymbolicSuffix(prefix,
                Word.fromLetter(new PSymbolInstance(I_LOGIN, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1))));

        TreeQueryResult tqr1 = mto.treeQuery(prefix, suffix1);

        SymbolicSuffix suffix2 = new SymbolicSuffix(prefix,
                Word.fromSymbols(new PSymbolInstance(I_LOGIN, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1)),
                        new PSymbolInstance(I_REGISTER, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1))));

        TreeQueryResult tqr2 = mto.treeQuery(prefix, suffix2);

        Branching initialFiner = mto.getInitialBranching(prefix, I_LOGIN, tqr1.getPiv(), tqr1.getSdt(), tqr2.getSdt());
        Branching initial = mto.getInitialBranching(prefix, I_LOGIN, new PIV());
        Branching finer = mto.updateBranching(prefix, I_LOGIN, initial, tqr1.getPiv(), tqr1.getSdt());
        Assert.assertEquals(initialFiner.getBranches().toString(), finer.getBranches().toString());
    }

    @Test
    public void testUpdateBranchingNoSDTsMeansNoUpdate() {
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        Word<PSymbolInstance> prefix = Word
                .fromLetter(new PSymbolInstance(I_REGISTER, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1)));

        SymbolicSuffix suffix1 = new SymbolicSuffix(prefix,
                Word.fromLetter(new PSymbolInstance(I_LOGIN, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1))));

        TreeQueryResult tqr1 = mto.treeQuery(prefix, suffix1);

        SymbolicSuffix suffix2 = new SymbolicSuffix(prefix,
                Word.fromSymbols(new PSymbolInstance(I_LOGIN, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1)),
                        new PSymbolInstance(I_REGISTER, new DataValue<>(T_UID, 1), new DataValue<>(T_PWD, 1))));

        TreeQueryResult tqr2 = mto.treeQuery(prefix, suffix2);

        Branching initialFiner = mto.getInitialBranching(prefix, I_LOGIN, tqr1.getPiv(), tqr1.getSdt(), tqr2.getSdt());
        Branching update = mto.updateBranching(prefix, I_LOGIN, initialFiner, tqr1.getPiv());
        Assert.assertEquals(update.getBranches().toString(), initialFiner.getBranches().toString());
    }

}
