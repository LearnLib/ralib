package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class InstantiateSymbolicWordTest {
	@Test
	public void testInstantiateStack() {
		Constants consts = new Constants();
	    RegisterAutomaton sul = AUTOMATON;
	    DataWordOracle dwOracle = new SimulatorOracle(sul);

	    final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	    teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	    ConstraintSolver solver = new SimpleConstraintSolver();

	    MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
	              dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                        new Constants(), solver);

        Word<PSymbolInstance> prefix = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)));
        Word<PSymbolInstance> suffix = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));
        SymbolicSuffix symbSuffix = new SymbolicSuffix(prefix, suffix);

        TreeQueryResult tqr = mto.treeQuery(prefix, symbSuffix);

        Map<Word<PSymbolInstance>, Boolean> words = mto.instantiate(prefix, symbSuffix, tqr.getSdt(), tqr.getPiv());

        Word<PSymbolInstance> p1 = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));
        Word<PSymbolInstance> p2 = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 2)));
        Word<PSymbolInstance> p3 = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 2)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 3)));

        Assert.assertEquals(words.size(), 3);
        Assert.assertTrue(words.containsKey(p1) &&
        		words.containsKey(p2) &&
        		words.containsKey(p3));
        Assert.assertTrue(words.get(p1).booleanValue());
	}
}
