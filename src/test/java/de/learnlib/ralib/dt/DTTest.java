package de.learnlib.ralib.dt;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DTTest {

	private DT buildFullTreePrimesOnly(TreeOracle oracle) {
		Word<PSymbolInstance> prePop = Word.fromSymbols(
				new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> prePush = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> prePushPush = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));
		Word<PSymbolInstance> epsilon = Word.epsilon();

		SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);
		SymbolicSuffix suffPop = new SymbolicSuffix(epsilon, prePop);
		SymbolicSuffix suffPush = new SymbolicSuffix(epsilon, prePush);

		TreeQueryResult tqrPop = oracle.treeQuery(prePop, suffEps);
		TreeQueryResult tqrEps = oracle.treeQuery(epsilon, suffPop);
		TreeQueryResult tqrPush = oracle.treeQuery(prePush, suffPush);
		TreeQueryResult tqrPushPush = oracle.treeQuery(prePushPush, suffPush);
		TreeQueryResult tqrInnerPop = oracle.treeQuery(epsilon, suffEps);
		TreeQueryResult tqrInnerPush = oracle.treeQuery(prePush, suffPop);

		DTInnerNode nodeEps = new DTInnerNode(suffEps);
		DTInnerNode nodePop = new DTInnerNode(suffPop);
		DTInnerNode nodePush = new DTInnerNode(suffPush);

		DTLeaf leafPop = new DTLeaf(new MappedPrefix(prePop, tqrPop.getPiv()), oracle);
		DTLeaf leafEps = new DTLeaf(new MappedPrefix(epsilon, tqrEps.getPiv()), oracle);
		DTLeaf leafPush = new DTLeaf(new MappedPrefix(prePush, tqrPush.getPiv()), oracle);
		DTLeaf leafPushPush = new DTLeaf(new MappedPrefix(prePushPush, tqrPushPush.getPiv()), oracle);
		leafPop.setParent(nodeEps);
		leafEps.setParent(nodePop);
		leafPush.setParent(nodePush);
		leafPushPush.setParent(nodePush);

		DTBranch brPop = new DTBranch(tqrPop.getSdt(), leafPop);
		DTBranch brEps = new DTBranch(tqrEps.getSdt(), leafEps);
		DTBranch brPush = new DTBranch(tqrPush.getSdt(), leafPush);
		DTBranch brPushPush = new DTBranch(tqrPushPush.getSdt(), leafPushPush);
		DTBranch brInnerPush = new DTBranch(tqrInnerPush.getSdt(), nodePush);
		DTBranch brInnerPop = new DTBranch(tqrInnerPop.getSdt(), nodePop);

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
				new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> epsilon = Word.epsilon();

		SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);
		SymbolicSuffix suffPop = new SymbolicSuffix(epsilon, prePop);

		TreeQueryResult tqrPop = oracle.treeQuery(prePop, suffEps);
		TreeQueryResult tqrEps = oracle.treeQuery(epsilon, suffPop);
		TreeQueryResult tqrInnerPop = oracle.treeQuery(epsilon, suffEps);

		DTInnerNode nodeEps = new DTInnerNode(suffEps);
		DTInnerNode nodePop = new DTInnerNode(suffPop);

		DTLeaf leafPop = new DTLeaf(new MappedPrefix(prePop, tqrPop.getPiv()), oracle);
		DTLeaf leafEps = new DTLeaf(new MappedPrefix(epsilon, tqrEps.getPiv()), oracle);
		leafPop.setParent(nodeEps);
		leafEps.setParent(nodePop);

		DTBranch brPop = new DTBranch(tqrPop.getSdt(), leafPop);
		DTBranch brEps = new DTBranch(tqrEps.getSdt(), leafEps);
		DTBranch brInnerPop = new DTBranch(tqrInnerPop.getSdt(), nodePop);

		nodeEps.addBranch(brPop);
		nodeEps.addBranch(brInnerPop);
		nodePop.addBranch(brEps);

		return new DT(nodeEps, oracle, false, new Constants(), I_PUSH, I_POP);
	}

	@Test
	public void siftTest() {

	      Constants consts = new Constants();
	      RegisterAutomaton sul = AUTOMATON;
	      DataWordOracle dwOracle = new SimulatorOracle(sul);

	      final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	      teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	      ConstraintSolver solver = new SimpleConstraintSolver();

	      MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
	              dwOracle, teachers, new Constants(), solver);

	      DT dt = buildFullTreePrimesOnly(mto);

	      Word<PSymbolInstance> prePush1Pop1 = Word.fromSymbols(
	              new PSymbolInstance(I_PUSH,
	                      new DataValue(T_INT, 1)),
	              new PSymbolInstance(I_POP,
	                      new DataValue(T_INT, 1)));

	      Word<PSymbolInstance> prePush1Pop2 = Word.fromSymbols(
	              new PSymbolInstance(I_PUSH,
	                      new DataValue(T_INT, 1)),
	              new PSymbolInstance(I_POP,
	                      new DataValue(T_INT, 2)));

	      Word<PSymbolInstance> prePushPushPop = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH,
	    				  new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_PUSH,
	    				  new DataValue(T_INT, 2)),
	    		  new PSymbolInstance(I_POP,
	    				  new DataValue(T_INT, 2)));

	      Word<PSymbolInstance> accessEps = Word.epsilon();
	      Word<PSymbolInstance> accessPop = Word.fromSymbols(
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT,1)));
	      Word<PSymbolInstance> accessPush = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT,1)));

	      DTLeaf leafPush1Pop1 = dt.sift(prePush1Pop1, true);
	      DTLeaf leafPush1Pop2 = dt.sift(prePush1Pop2, true);
	      DTLeaf leafPushPushPop = dt.sift(prePushPushPop, true);

	      Assert.assertTrue(leafPush1Pop1.getAccessSequence().equals(accessEps));
	      Assert.assertTrue(leafPush1Pop2.getAccessSequence().equals(accessPop));
	      Assert.assertTrue(leafPushPushPop.getAccessSequence().equals(accessPush));

	      // test en passant discovery
	      dt = buildSimpleTree(mto);
	      int leavesBeforeDiscovery = dt.getLeaves().size();
	      Word<PSymbolInstance> prePush = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)));
	      Word<PSymbolInstance> prePushPush = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));
	      DTLeaf newLeaf = dt.sift(prePush, true);

	      Assert.assertEquals(dt.getLeaves().size(), leavesBeforeDiscovery + 1);
	      Assert.assertTrue(newLeaf.getAllPrefixes().contains(prePushPush));
	      Assert.assertTrue(dt.getLeaf(accessEps).getAllPrefixes().contains(prePush1Pop1));
	      Assert.assertTrue(dt.getLeaf(accessPop).getAllPrefixes().contains(prePush1Pop2));
	}

	@Test
	public void splitTest() {

	      Constants consts = new Constants();
	      RegisterAutomaton sul = AUTOMATON;
	      DataWordOracle dwOracle = new SimulatorOracle(sul);

	      final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	      teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	      ConstraintSolver solver = new SimpleConstraintSolver();

	      MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
	              dwOracle, teachers, new Constants(), solver);
	      SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

	      DT dt = new DT(mto, false, consts, I_PUSH, I_POP);
	      dt.initialize();

	      DTHyp hyp = new DTHyp(consts, dt);

	      Word<PSymbolInstance> prePush = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)));
	      Word<PSymbolInstance> prePop = Word.fromSymbols(
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));
	      Word<PSymbolInstance> eps = Word.epsilon();
	      SymbolicSuffix suffPop = new SymbolicSuffix(eps, prePop);

	      DTLeaf leafEps = dt.getLeaf(eps);
	      leafEps.elevatePrefix(dt, prePush, hyp, slo);

	      dt.split(prePush, suffPop, leafEps);

	      // assert new leaf added for PUSH(0)
	      DTLeaf leafPush = dt.getLeaf(prePush);
	      Assert.assertTrue(leafPush.getAccessSequence().equals(prePush));

	      // assert epsilon and push(0) are both children of inner node pop
	      Assert.assertTrue(leafEps.getParent().getSuffix().equals(suffPop));
	      Assert.assertTrue(leafPush.getParent().getSuffix().equals(suffPop));
	}
}
