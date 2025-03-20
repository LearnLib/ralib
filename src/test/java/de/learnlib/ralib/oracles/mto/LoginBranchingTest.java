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
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.smt.ConstraintSolver;
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
        MultiTheoryTreeOracle mto = TestUtil.createMTO(sul, ERROR, teachers, consts, new ConstraintSolver(),
                inputs);

        DataType uid = TestUtil.getType("uid", loader.getDataTypes());
        DataType pwd = TestUtil.getType("pwd", loader.getDataTypes());

        ParameterizedSymbol reg = new InputSymbol("IRegister", uid, pwd);

        ParameterizedSymbol log = new InputSymbol("ILogin", uid, pwd);

        ParameterizedSymbol ok = new OutputSymbol("OOK");

        DataValue u = new DataValue(uid, BigDecimal.ZERO);
        DataValue p = new DataValue(pwd, BigDecimal.ZERO);

        Word<PSymbolInstance> prefix = Word.fromSymbols(new PSymbolInstance(reg, u, p),
                new PSymbolInstance(ok));

        Word<PSymbolInstance> suffix = Word.fromSymbols(new PSymbolInstance(log, u, p),
                new PSymbolInstance(ok));

        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);

        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Conc. Suffix: {0}", suffix);
        logger.log(Level.FINE, "Sym. Suffix: {0}", symSuffix);

        TreeQueryResult tqr = mto.treeQuery(prefix, symSuffix);
        logger.log(Level.FINE, "SDT: {0}", tqr.sdt());

        // initial branching bug
        // Regression: Why does the last word in the set have a password val. of 2

        Branching bug1 = mto.getInitialBranching(prefix, log, tqr.sdt());
        final String expectedKeyset = "[IRegister[0[uid], 0[pwd]] OOK[] ILogin[0[uid], 0[pwd]],"
                + " IRegister[0[uid], 0[pwd]] OOK[] ILogin[0[uid], 1[pwd]],"
                + " IRegister[0[uid], 0[pwd]] OOK[] ILogin[1[uid], 1[pwd]]]";

        String keyset = Arrays.toString(bug1.getBranches().keySet().toArray());
        Assert.assertEquals(keyset, expectedKeyset);

        // updated branching bug
        // Regression: This keyset has only one word, there should be three.

        Branching bug2 = mto.getInitialBranching(prefix, log);
        bug2 = mto.updateBranching(prefix, log, bug2, tqr.sdt());
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

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        Word<PSymbolInstance> prefix = Word
                .fromLetter(new PSymbolInstance(I_REGISTER, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)));

        SymbolicSuffix suffix1 = new SymbolicSuffix(prefix,
                Word.fromLetter(new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE))));

        TreeQueryResult tqr1 = mto.treeQuery(prefix, suffix1);

        SymbolicSuffix suffix2 = new SymbolicSuffix(prefix,
                Word.fromSymbols(new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)),
                        new PSymbolInstance(I_REGISTER, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE))));

        TreeQueryResult tqr2 = mto.treeQuery(prefix, suffix2);

        Branching initial = mto.getInitialBranching(prefix, I_LOGIN);
        Branching finer = mto.updateBranching(prefix, I_LOGIN, initial, tqr1.sdt());
        Branching actual = mto.updateBranching(prefix, I_LOGIN, finer, tqr2.sdt());
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

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        Word<PSymbolInstance> prefix = Word
                .fromLetter(new PSymbolInstance(I_REGISTER, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)));

        SymbolicSuffix suffix1 = new SymbolicSuffix(prefix,
                Word.fromLetter(new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE))));

        TreeQueryResult tqr1 = mto.treeQuery(prefix, suffix1);

        SymbolicSuffix suffix2 = new SymbolicSuffix(prefix,
                Word.fromSymbols(new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)),
                        new PSymbolInstance(I_REGISTER, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE))));

        TreeQueryResult tqr2 = mto.treeQuery(prefix, suffix2);

        Branching initialFiner = mto.getInitialBranching(prefix, I_LOGIN, tqr1.sdt(), tqr2.sdt());
        Branching initial = mto.getInitialBranching(prefix, I_LOGIN);
        Branching finer = mto.updateBranching(prefix, I_LOGIN, initial, tqr1.sdt());
        Assert.assertEquals(initialFiner.getBranches().toString(), finer.getBranches().toString());
    }

    @Test
    public void testUpdateBranchingNoSDTsMeansNoUpdate() {
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        Word<PSymbolInstance> prefix = Word
                .fromLetter(new PSymbolInstance(I_REGISTER, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)));

        SymbolicSuffix suffix1 = new SymbolicSuffix(prefix,
                Word.fromLetter(new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE))));

        TreeQueryResult tqr1 = mto.treeQuery(prefix, suffix1);

        SymbolicSuffix suffix2 = new SymbolicSuffix(prefix,
                Word.fromSymbols(new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE)),
                        new PSymbolInstance(I_REGISTER, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ONE))));

        TreeQueryResult tqr2 = mto.treeQuery(prefix, suffix2);

        Branching initialFiner = mto.getInitialBranching(prefix, I_LOGIN, tqr1.sdt(), tqr2.sdt());
        Branching update = mto.updateBranching(prefix, I_LOGIN, initialFiner);
        Assert.assertEquals(update.getBranches().toString(), initialFiner.getBranches().toString());
    }

}
