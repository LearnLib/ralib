package de.learnlib.ralib.theory;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import de.learnlib.ralib.smt.ConstraintSolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;

import de.learnlib.ralib.tools.theories.UniqueIntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class TestUniqueEqualityTheory extends RaLibTestSuite {

    @Test
    public void testLoginExample1() {

        DataWordOracle oracle = new SimulatorOracle(AUTOMATON);

        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(T_UID, new UniqueIntegerEqualityTheory(T_UID));
        theories.put(T_PWD, new UniqueIntegerEqualityTheory(T_PWD));

        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(
                oracle, theories, new Constants(), new ConstraintSolver());

        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN,
                        new DataValue(T_UID, BigDecimal.ONE),
                        new DataValue(T_PWD, BigDecimal.ONE)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN,
                        new DataValue(T_UID, new BigDecimal(2)),
                        new DataValue(T_PWD, new BigDecimal(2))));

        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER,
                        new DataValue(T_UID, BigDecimal.ONE),
                        new DataValue(T_PWD, BigDecimal.ONE)));


        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2, s3, s4
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        String expectedTree = "[]-+\n" +
                "  []-TRUE: s1\n" +
                "        []-TRUE: s2\n" +
                "              []-TRUE: s3\n" +
                "                    []-TRUE: s4\n" +
                "                          [Leaf-]\n";

        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedTree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);

        SymbolicDataValue.Parameter p1 = new SymbolicDataValue.Parameter(T_UID, 1);
        SymbolicDataValue.Parameter p2 = new SymbolicDataValue.Parameter(T_PWD, 2);

        PIV testPiv =  new PIV();
        testPiv.put(p1, new SymbolicDataValue.Register(T_UID, 1));
        testPiv.put(p2, new SymbolicDataValue.Register(T_PWD, 2));

        Branching b = treeOracle.getInitialBranching(prefix, I_LOGIN, testPiv, sdt);

        Assert.assertEquals(b.getBranches().size(), 1);
        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }

}
