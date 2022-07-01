package de.learnlib.ralib.dt;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.automata.TransitionGuard;
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
		
		DTLeaf leafPop = new DTLeaf(new MappedPrefix(prePop, tqrPop.getPiv()));
		DTLeaf leafEps = new DTLeaf(new MappedPrefix(epsilon, tqrEps.getPiv()));
		DTLeaf leafPush = new DTLeaf(new MappedPrefix(prePush, tqrPush.getPiv()));
		DTLeaf leafPushPush = new DTLeaf(new MappedPrefix(prePushPush, tqrPushPush.getPiv()));
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
		
		nodeEps.addBranch(brPop);
		nodeEps.addBranch(brInnerPop);
		nodePop.addBranch(brEps);
		nodePop.addBranch(brInnerPush);
		nodePush.addBranch(brPush);
		nodePush.addBranch(brPushPush);
		
		return new DT(nodeEps, oracle, false, new Constants(), I_PUSH, I_POP);
	}
	
	private DT buildFullTreeWithoutEpsilon(TreeOracle oracle) {
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
		//TreeQueryResult tqrEps = oracle.treeQuery(epsilon, suffPop);
		TreeQueryResult tqrPush = oracle.treeQuery(prePush, suffPush);
		TreeQueryResult tqrPushPush = oracle.treeQuery(prePushPush, suffPush);
		TreeQueryResult tqrInnerPop = oracle.treeQuery(epsilon, suffEps);
		TreeQueryResult tqrInnerPush = oracle.treeQuery(prePush, suffPop);

		DTInnerNode nodeEps = new DTInnerNode(suffEps);
		DTInnerNode nodePop = new DTInnerNode(suffPop);
		DTInnerNode nodePush = new DTInnerNode(suffPush);
		
		DTLeaf leafPop = new DTLeaf(new MappedPrefix(prePop, tqrPop.getPiv()));
		//DTLeaf leafEps = new DTLeaf(new MappedPrefix(epsilon, tqrEps.getPiv()));
		DTLeaf leafPush = new DTLeaf(new MappedPrefix(prePush, tqrPush.getPiv()));
		DTLeaf leafPushPush = new DTLeaf(new MappedPrefix(prePushPush, tqrPushPush.getPiv()));
		leafPop.setParent(nodeEps);
		//leafEps.setParent(nodePop);
		leafPush.setParent(nodePush);
		leafPushPush.setParent(nodePush);
		
		DTBranch brPop = new DTBranch(tqrPop.getSdt(), leafPop);
		//DTBranch brEps = new DTBranch(tqrEps.getSdt(), leafEps);
		DTBranch brPush = new DTBranch(tqrPush.getSdt(), leafPush);
		DTBranch brPushPush = new DTBranch(tqrPushPush.getSdt(), leafPushPush);
		DTBranch brInnerPush = new DTBranch(tqrInnerPush.getSdt(), nodePush);
		DTBranch brInnerPop = new DTBranch(tqrInnerPop.getSdt(), nodePop);
		
		nodeEps.addBranch(brPop);
		nodeEps.addBranch(brInnerPop);
		//nodePop.addBranch(brEps);
		nodePop.addBranch(brInnerPush);
		nodePush.addBranch(brPush);
		nodePush.addBranch(brPushPush);
		
		return new DT(nodeEps, oracle, false, new Constants(), I_PUSH, I_POP);

	}
	
	private DT buildIncompleteTree(TreeOracle oracle, boolean extraPrefs) {
		Word<PSymbolInstance> prePop = Word.fromSymbols(
				new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));
		Word<PSymbolInstance> prePush = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)));
		Word<PSymbolInstance> prePopPush = Word.fromSymbols(
				new PSymbolInstance(I_POP, new DataValue(T_INT, 0)),
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> prePopPop = Word.fromSymbols(
				new PSymbolInstance(I_POP, new DataValue(T_INT, 0)),
				new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> prePushPush = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> prePushPopEq = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
				new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));
		Word<PSymbolInstance> prePushPopNeq = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
				new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> prePushPushPop = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
				new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));
		Word<PSymbolInstance> prePushPushPopPush = Word.fromSymbols(
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
				new PSymbolInstance(I_POP, new DataValue(T_INT, 1)),
				new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));
		Word<PSymbolInstance> epsilon = Word.epsilon();
		
		SymbolicSuffix suffEps = new SymbolicSuffix(epsilon, epsilon);
		SymbolicSuffix suffPop = new SymbolicSuffix(epsilon, prePop);
		
		TreeQueryResult tqrPop = oracle.treeQuery(prePop, suffEps);
		TreeQueryResult tqrEps = oracle.treeQuery(epsilon, suffPop);
		TreeQueryResult tqrPush = oracle.treeQuery(prePush, suffPop);
		TreeQueryResult tqrPushPush = oracle.treeQuery(prePushPush, suffPop);
		TreeQueryResult tqrInnerPop = oracle.treeQuery(epsilon, suffEps);

		DTInnerNode nodeEps = new DTInnerNode(suffEps);
		DTInnerNode nodePop = new DTInnerNode(suffPop);
		
		DTLeaf leafPop = new DTLeaf(new MappedPrefix(prePop, tqrPop.getPiv()));
		DTLeaf leafEps = new DTLeaf(new MappedPrefix(epsilon, tqrEps.getPiv()));
		DTLeaf leafPush = new DTLeaf(new MappedPrefix(prePush, tqrPush.getPiv()));
		leafPop.addPrefix(new MappedPrefix(prePopPush, tqrPop.getPiv()));
		leafPop.addPrefix(new MappedPrefix(prePopPop, tqrPop.getPiv()));
		leafPop.addPrefix(new MappedPrefix(prePushPopNeq, tqrPop.getPiv()));
		leafPop.addTQRs(tqrPop.getPiv(), suffEps, oracle, true);
		leafEps.addPrefix(new MappedPrefix(prePushPopEq, tqrEps.getPiv()));
		leafEps.addTQRs(tqrEps.getPiv(), suffEps, oracle, true);
		leafEps.addTQRs(tqrEps.getPiv(), suffPop, oracle, true);
		leafPush.addShortPrefix(new MappedPrefix(prePushPush, tqrPushPush.getPiv()));
		leafPush.addPrefix(prePushPushPop);
		if (extraPrefs)
			leafPush.addPrefix(prePushPushPopPush);
		leafPush.addTQRs(tqrPush.getPiv(), suffEps, oracle, true);
		leafPush.addTQRs(tqrPush.getPiv(), suffPop, oracle, true);
		
		leafPop.setParent(nodeEps);
		leafEps.setParent(nodePop);
		leafPush.setParent(nodePop);
		
		DTBranch brPop = new DTBranch(tqrPop.getSdt(), leafPop);
		DTBranch brEps = new DTBranch(tqrEps.getSdt(), leafEps);
		DTBranch brPush = new DTBranch(tqrPush.getSdt(), leafPush);
		DTBranch brInnerPop = new DTBranch(tqrInnerPop.getSdt(), nodePop);
		
		nodeEps.addBranch(brPop);
		nodeEps.addBranch(brInnerPop);
		nodePop.addBranch(brEps);
		nodePop.addBranch(brPush);
		
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
	      
	      Word<PSymbolInstance> p1 = Word.fromSymbols(
	              new PSymbolInstance(I_PUSH, 
	                      new DataValue(T_INT, 1)),
	              new PSymbolInstance(I_POP,
	                      new DataValue(T_INT, 1)));
	      
	      Word<PSymbolInstance> p2 = Word.fromSymbols(
	              new PSymbolInstance(I_PUSH, 
	                      new DataValue(T_INT, 1)),
	              new PSymbolInstance(I_POP,
	                      new DataValue(T_INT, 2)));
	      
	      Word<PSymbolInstance> p3 = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH,
	    				  new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_PUSH,
	    				  new DataValue(T_INT, 2)),
	    		  new PSymbolInstance(I_POP,
	    				  new DataValue(T_INT, 2)));

	      Word<PSymbolInstance> access1 = Word.epsilon();
	      Word<PSymbolInstance> access2 = Word.fromSymbols(
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT,1)));
	      Word<PSymbolInstance> access3 = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT,1)));
	      
	      DTLeaf leaf1 = dt.sift(p1, true);
	      DTLeaf leaf2 = dt.sift(p2, true);
	      DTLeaf leaf3 = dt.sift(p3, true);
	      //int len = leaf1.getShortPrefixes().iterator().next().getPrefix().length();
	      //PSymbolInstance asPush1Pop2 = leaf2.getShortPrefixes().iterator().next().getPrefix().getSymbol(0);
	      //PSymbolInstance asPush1Push2Pop2 = leaf3.getShortPrefixes().iterator().next().getPrefix().getSymbol(0);
	      int len = leaf1.getPrimePrefix().getPrefix().length();
	      PSymbolInstance asPush1Pop2 = leaf2.getPrimePrefix().getPrefix().getSymbol(0);
	      PSymbolInstance asPush1Push2Pop2 = leaf3.getPrimePrefix().getPrefix().getSymbol(0);
	      
	      Assert.assertEquals(len, access1.length());
	      Assert.assertTrue(leaf1.getPrefixes().contains(p1));
	      Assert.assertEquals(asPush1Pop2, access2.getSymbol(0));
	      Assert.assertTrue(leaf2.getPrefixes().contains(p2));
	      Assert.assertEquals(asPush1Push2Pop2, access3.getSymbol(0));
	      Assert.assertTrue(leaf3.getPrefixes().contains(p3));
	      
	      // test en passant discovery
	      dt = buildFullTreeWithoutEpsilon(mto);
	      Word<PSymbolInstance> epsilon = Word.epsilon();
	      DTLeaf leafEps = dt.sift(epsilon, true);
	      
	      //Assert.assertEquals(leafEps.getShortPrefixes().iterator().next().getPrefix().length(), epsilon.length());
	      Assert.assertEquals(leafEps.getPrimePrefix().getPrefix().length(), epsilon.length());
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

	      DT dt = buildIncompleteTree(mto, true);
	      for (DTLeaf l : dt.getLeaves()) {
	    	  l.start(dt, mto, false, I_PUSH, I_POP);
	    	  l.updateBranching(mto, dt);
	      }
	      
		  Word<PSymbolInstance> prePushPush = Word.fromSymbols(
				  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
				  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)));
		  Word<PSymbolInstance> suffPush = Word.fromSymbols(
				  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)));
		  Word<PSymbolInstance> epsilon = Word.epsilon();
		  SymbolicSuffix suffix = new SymbolicSuffix(epsilon, suffPush);
		  
		  dt.split(prePushPush, suffix, dt.getLeaf(prePushPush));

	      Word<PSymbolInstance> p1 = Word.fromSymbols(
	              new PSymbolInstance(I_PUSH, 
	                      new DataValue(T_INT, 0)),
	              new PSymbolInstance(I_POP,
	                      new DataValue(T_INT, 0)));
	      
	      Word<PSymbolInstance> p2 = Word.fromSymbols(
	              new PSymbolInstance(I_PUSH, 
	                      new DataValue(T_INT, 0)),
	              new PSymbolInstance(I_POP,
	                      new DataValue(T_INT, 1)));
	      
	      Word<PSymbolInstance> p3 = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH,
	    				  new DataValue(T_INT, 0)),
	    		  new PSymbolInstance(I_PUSH,
	    				  new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_POP,
	    				  new DataValue(T_INT, 1)));

	      Word<PSymbolInstance> p4 = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));

	      Word<PSymbolInstance> access1 = Word.epsilon();
	      Word<PSymbolInstance> access2 = Word.fromSymbols(
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT,0)));
	      Word<PSymbolInstance> access3 = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT,0)));
	      Word<PSymbolInstance> access4 = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT,0)),
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT,1)));

	      // test sifting after split
	      DTLeaf leaf1 = dt.sift(p1, false);
	      DTLeaf leaf2 = dt.sift(p2, false);
	      DTLeaf leaf3 = dt.sift(p3, false);
	      DTLeaf leaf4 = dt.sift(p4, false);
	      int len = leaf1.getPrimePrefix().getPrefix().length();
	      PSymbolInstance asPush1Pop2 = leaf2.getPrimePrefix().getPrefix().getSymbol(0);
	      PSymbolInstance asPush1Push2Pop2 = leaf3.getPrimePrefix().getPrefix().getSymbol(0);
	      Word<PSymbolInstance> word4 = leaf4.getPrimePrefix().getPrefix();

	      Assert.assertEquals(len, access1.length());
	      Assert.assertEquals(asPush1Pop2, access2.getSymbol(0));
	      Assert.assertEquals(asPush1Push2Pop2, access3.getSymbol(0));
	      Assert.assertEquals(word4.getSymbol(0), access4.getSymbol(0));
	      Assert.assertEquals(word4.getSymbol(1), access4.getSymbol(1));
	      
	      // test variable consistency after split 
	      boolean variableCorrectness = leaf3.checkVariableConsistency(dt, mto, new Constants());
	      Assert.assertFalse(variableCorrectness);
	      
	      Word<PSymbolInstance> prePopPop = Word.fromSymbols(
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT, 0)),
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));
	      SymbolicSuffix suffPopPop = new SymbolicSuffix(epsilon, prePopPop);
	      DTInnerNode leaf4parent = (DTInnerNode)leaf4.getParent();
	      Assert.assertTrue(suffPopPop.equals(leaf4parent.getSuffix()));
	      
	      // correct number of parameter assignments of push push leaf
	      Assert.assertEquals(leaf4.getPrimePrefix().getParsInVars().size(), 2);
	      // correct number of branches for the pop transition of push push leaf
	      Assert.assertEquals(leaf4.getBranching(I_POP).getBranches().size(), 2);
	      
	      Word<PSymbolInstance> prePush = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)));
	      
	      Word<PSymbolInstance> prePushPushPopNeq = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_POP, new DataValue(T_INT, 2)));
	      Word<PSymbolInstance> prePushPushPush = Word.fromSymbols(
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
	    		  new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));
	      
	      // test hypothesis construction from DT
	      dt = buildIncompleteTree(mto, false);
	      for (DTLeaf l : dt.getLeaves()) {
	    	  l.start(dt, mto, false, I_PUSH, I_POP);
	    	  l.updateBranching(mto, dt);
	      }
	      dt.split(prePushPush, suffix, dt.getLeaf(prePushPush));
	      dt.getLeaf(prePush).checkVariableConsistency(dt, mto, consts);
	      dt.sift(prePushPushPopNeq, true);
	      dt.sift(prePushPushPush, true);
	      AutomatonBuilder ab = new AutomatonBuilder(dt.getComponents(), consts);
	      Hypothesis hyp = ab.toRegisterAutomaton();
//	      System.out.println(hyp.toString());
	      
	      Assert.assertEquals(hyp.getStates().size(), 4);
	      Assert.assertEquals(hyp.getTransitions().size(), 10);
	}
}
