package de.learnlib.ralib.dt;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class DTTest {

    private DT buildFullTreePrimesOnly(TreeOracle oracle) {
        Word<PSymbolInstance> prePop = Word.fromSymbols(
            new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(1))));
        Word<PSymbolInstance> prePush = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))));
        Word<PSymbolInstance> prePushPush = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))),
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(2))));
        Word<PSymbolInstance> epsilon = Word.epsilon();

        SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);
        SymbolicSuffix suffPop = new SymbolicSuffix(epsilon, prePop);
        SymbolicSuffix suffPush = new SymbolicSuffix(epsilon, prePush);

        SDT tqrPop = oracle.treeQuery(prePop, suffEps);
        SDT tqrEps = oracle.treeQuery(epsilon, suffPop);
        SDT tqrPush = oracle.treeQuery(prePush, suffPush);
        SDT tqrPushPush = oracle.treeQuery(prePushPush, suffPush);

        DTInnerNode nodeEps = new DTInnerNode(suffEps);
        DTInnerNode nodePop = new DTInnerNode(suffPop);
        DTInnerNode nodePush = new DTInnerNode(suffPush);

        PathResult rPop = PathResult.computePathResult(oracle, new MappedPrefix(prePop, new Bijection<>()), nodeEps.getSuffixes(), false);
        PathResult rEps = PathResult.computePathResult(oracle, new MappedPrefix(epsilon, new Bijection<>()), nodePop.getSuffixes(), false);
        PathResult rPush = PathResult.computePathResult(oracle, new MappedPrefix(prePush, new Bijection<>()), nodePush.getSuffixes(), false);
        PathResult rPushPush = PathResult.computePathResult(oracle, new MappedPrefix(prePushPush, new Bijection<>()), nodePush.getSuffixes(), false);
        PathResult rInnerPop = PathResult.computePathResult(oracle, new MappedPrefix(epsilon, new Bijection<>()), nodeEps.getSuffixes(), false);
        PathResult rInnerPush = PathResult.computePathResult(oracle, new MappedPrefix(prePush, new Bijection<>()), nodePop.getSuffixes(), false);

        DTLeaf leafPop = new DTLeaf(new MappedPrefix(prePop, new Bijection<>()), oracle);
        DTLeaf leafEps = new DTLeaf(new MappedPrefix(epsilon, new Bijection<>()), oracle);
        DTLeaf leafPush = new DTLeaf(new MappedPrefix(prePush, new Bijection<>()), oracle);
        DTLeaf leafPushPush = new DTLeaf(new MappedPrefix(prePushPush, new Bijection<>()), oracle);
        leafPop.setParent(nodeEps);
        leafEps.setParent(nodePop);
        leafPush.setParent(nodePush);
        leafPushPush.setParent(nodePush);

        DTBranch brPop = new DTBranch(leafPop, rPop);
        DTBranch brEps = new DTBranch(leafEps, rEps);
        DTBranch brPush = new DTBranch(leafPush, rPush);
        DTBranch brPushPush = new DTBranch(leafPushPush, rPushPush);
        DTBranch brInnerPush = new DTBranch(nodePush, rInnerPush);
        DTBranch brInnerPop = new DTBranch(nodePop, rInnerPop);

        leafPush.getPrimePrefix().addTQR(suffPop, oracle.treeQuery(prePush, suffPop));
        leafPushPush.getPrimePrefix().addTQR(suffPop, oracle.treeQuery(prePushPush, suffPop));

        nodeEps.addBranch(brPop);
        nodeEps.addBranch(brInnerPop);
        nodePop.addBranch(brEps);
        nodePop.addBranch(brInnerPush);
        nodePush.addBranch(brPush);
        nodePush.addBranch(brPushPush);

        return new DT(nodeEps, oracle, false, new Constants(), I_PUSH, I_POP);
    }

    private DT buildSimpleTree(TreeOracle oracle) {
        Word<PSymbolInstance> prePop = Word.fromSymbols(
            new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(1))));
        Word<PSymbolInstance> epsilon = Word.epsilon();

        SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);
        SymbolicSuffix suffPop = new SymbolicSuffix(epsilon, prePop);

        SDT tqrPop = oracle.treeQuery(prePop, suffEps);
        SDT tqrEps = oracle.treeQuery(epsilon, suffPop);

        DTInnerNode nodeEps = new DTInnerNode(suffEps);
        DTInnerNode nodePop = new DTInnerNode(suffPop);

        PathResult rPop = PathResult.computePathResult(oracle, new MappedPrefix(prePop, new Bijection<>()), nodeEps.getSuffixes(), false);
        PathResult rEps = PathResult.computePathResult(oracle, new MappedPrefix(epsilon, new Bijection<>()), nodePop.getSuffixes(), false);
        PathResult rInnerPop= PathResult.computePathResult(oracle, new MappedPrefix(epsilon, new Bijection<>()), nodeEps.getSuffixes(), false);

        DTLeaf leafPop = new DTLeaf(new MappedPrefix(prePop, new Bijection<>()), oracle);
        DTLeaf leafEps = new DTLeaf(new MappedPrefix(epsilon, new Bijection<>()), oracle);
        leafPop.setParent(nodeEps);
        leafEps.setParent(nodePop);

        DTBranch brPop = new DTBranch(leafPop, rPop);
        DTBranch brEps = new DTBranch(leafEps, rEps);
        DTBranch brInnerPop = new DTBranch(nodePop, rInnerPop);

        nodeEps.addBranch(brPop);
        nodeEps.addBranch(brInnerPop);
        nodePop.addBranch(brEps);

        return new DT(nodeEps, oracle, false, new Constants(), I_PUSH, I_POP);
    }

    @Test
    public void testSiftDT() {
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        DT dt = buildFullTreePrimesOnly(mto);

        Word<PSymbolInstance> prePush1Pop1 = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))),
            new PSymbolInstance(I_POP,  new DataValue(T_INT, new BigDecimal(1))));

        Word<PSymbolInstance> prePush1Pop2 = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))),
            new PSymbolInstance(I_POP,  new DataValue(T_INT, new BigDecimal(2))));

        Word<PSymbolInstance> prePushPushPop = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))),
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(2))),
            new PSymbolInstance(I_POP,  new DataValue(T_INT, new BigDecimal(2))));

        Word<PSymbolInstance> accessEps = Word.epsilon();
        Word<PSymbolInstance> accessPop = Word.fromSymbols(
            new PSymbolInstance(I_POP, new DataValue(T_INT,new BigDecimal(1))));
        Word<PSymbolInstance> accessPush = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT,new BigDecimal(1))));

        DTLeaf leafPush1Pop1 = dt.sift(prePush1Pop1, true);
        DTLeaf leafPush1Pop2 = dt.sift(prePush1Pop2, true);
        DTLeaf leafPushPushPop = dt.sift(prePushPushPop, true);

        Assert.assertEquals(accessEps, leafPush1Pop1.getAccessSequence());
        Assert.assertEquals(accessPop, leafPush1Pop2.getAccessSequence());
        Assert.assertEquals(accessPush, leafPushPushPop.getAccessSequence());

        // test en passant discovery
        dt = buildSimpleTree(mto);
        int leavesBeforeDiscovery = dt.getLeaves().size();
        Word<PSymbolInstance> prePush = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))));
        Word<PSymbolInstance> prePushPush = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))),
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(2))));
        DTLeaf newLeaf = dt.sift(prePush, true);

        Assert.assertEquals(dt.getLeaves().size(), leavesBeforeDiscovery + 1);
        Assert.assertTrue(newLeaf.getAllPrefixes().contains(prePushPush));
        Assert.assertTrue(dt.getLeaf(accessEps).getAllPrefixes().contains(prePush1Pop1));
        Assert.assertTrue(dt.getLeaf(accessPop).getAllPrefixes().contains(prePush1Pop2));
    }

    @Test
    public void testSplitDT() {
        Constants consts = new Constants();
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants(), solver);
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        DT dt = new DT(mto, false, consts, I_PUSH, I_POP);
        dt.initialize();

        DTHyp hyp = new DTHyp(consts, dt);

        Word<PSymbolInstance> prePush = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(0))));
	Word<PSymbolInstance> prePop = Word.fromSymbols(
            new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(0))));
        Word<PSymbolInstance> eps = Word.epsilon();
        SymbolicSuffix suffPop = new SymbolicSuffix(eps, prePop);

        DTLeaf leafEps = dt.getLeaf(eps);
        leafEps.elevatePrefix(dt, prePush, hyp, slo);

        dt.split(prePush, suffPop, leafEps);

        // assert new leaf added for PUSH(0)
        DTLeaf leafPush = dt.getLeaf(prePush);
        Assert.assertEquals(prePush, leafPush.getAccessSequence());

        // assert epsilon and push(0) are both children of inner node pop
        Assert.assertEquals(suffPop, leafEps.getParent().getSuffix());
        Assert.assertEquals(suffPop, leafPush.getParent().getSuffix());
    }
}
