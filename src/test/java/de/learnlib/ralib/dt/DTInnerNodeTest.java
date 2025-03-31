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
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class DTInnerNodeTest {

    @Test
    public void siftTest() {

        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
            dwOracle, teachers, new Constants(), solver);

        Word<PSymbolInstance> p1 = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))),
            new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(1))));

        Word<PSymbolInstance> p2 = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))),
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(2))));

        Word<PSymbolInstance> epsilon = Word.epsilon();
        Word<PSymbolInstance> push = Word.fromSymbols(
            new PSymbolInstance(I_PUSH, new DataValue(T_INT, new BigDecimal(1))));

        Word<PSymbolInstance> suffix = Word.fromSymbols(
            new PSymbolInstance(I_POP, new DataValue(T_INT, new BigDecimal(1))));

        SymbolicSuffix symbSuffix = new SymbolicSuffix(epsilon, suffix);

        DTInnerNode node = new DTInnerNode(symbSuffix);
        DTLeaf child1 = new DTLeaf(new MappedPrefix(epsilon, new Bijection<>()), mto);
        DTLeaf child2 = new DTLeaf(new MappedPrefix(push, new Bijection<>()), mto);

        PathResult r1 = PathResult.computePathResult(mto, new MappedPrefix(epsilon, new Bijection<>()), node.getSuffixes(), false);
        PathResult r2 = PathResult.computePathResult(mto, new MappedPrefix(push, new Bijection<>()), node.getSuffixes(), false);


        node.addBranch(new DTBranch(child1, r1));
        node.addBranch(new DTBranch(child2, r2));

        DTNode test1 = node.sift(new MappedPrefix(p1, new Bijection<>()), mto, false).getKey();
        DTNode test2 = node.sift(new MappedPrefix(p2, new Bijection<>()), mto, false).getKey();

        Assert.assertEquals(test1, child1);
        Assert.assertEquals(test2, child2);
    }
}
