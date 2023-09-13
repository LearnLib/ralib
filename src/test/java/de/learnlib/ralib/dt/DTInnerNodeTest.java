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
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class DTInnerNodeTest {

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

      Word<PSymbolInstance> p1 = Word.fromSymbols(
              new PSymbolInstance(I_PUSH,
                      new DataValue(T_INT, 1)),
              new PSymbolInstance(I_POP,
                      new DataValue(T_INT, 1)));

      Word<PSymbolInstance> p2 = Word.fromSymbols(
              new PSymbolInstance(I_PUSH,
                      new DataValue(T_INT, 1)),
              new PSymbolInstance(I_PUSH,
                      new DataValue(T_INT, 2)));

      Word<PSymbolInstance> epsilon = Word.epsilon();
      Word<PSymbolInstance> push = Word.fromSymbols(
    		  new PSymbolInstance(I_PUSH,
    				  new DataValue(T_INT, 1)));

      Word<PSymbolInstance> suffix = Word.fromSymbols(
    		  new PSymbolInstance(I_POP,
    				  new DataValue(T_INT, 1)));

      SymbolicSuffix symbSuffix = new SymbolicSuffix(epsilon, suffix);

      DTInnerNode node = new DTInnerNode(symbSuffix);
      DTLeaf child1 = new DTLeaf(new MappedPrefix(epsilon, new PIV()), mto);
      DTLeaf child2 = new DTLeaf(new MappedPrefix(push, new PIV()), mto);

      TreeQueryResult tqr1 = mto.treeQuery(epsilon, symbSuffix);
      PathResult r1 = PathResult.computePathResult(mto, epsilon, node.getSuffixes(), false);
      TreeQueryResult tqr2 = mto.treeQuery(push, symbSuffix);
      PathResult r2 = PathResult.computePathResult(mto, push, node.getSuffixes(), false);

      node.addBranch(new DTBranch(child1, r1));
      node.addBranch(new DTBranch(child2, r2));

      DTNode test1 = node.sift(p1, mto, false).getKey();
      DTNode test2 = node.sift(p2, mto, false).getKey();

      Assert.assertEquals(test1, child1);
      Assert.assertEquals(test2, child2);
    }
}
