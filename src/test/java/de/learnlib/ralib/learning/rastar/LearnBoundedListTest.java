package de.learnlib.ralib.learning.rastar;

import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INSERT;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INT_TYPE;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.POP;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.PUSH;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.dv;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.example.list.BoundedList;
import de.learnlib.ralib.example.list.BoundedListDataWordOracle;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.ConstraintSolverFactory;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author Paul Fiterau
 *
 */
public class LearnBoundedListTest {

    @Test(enabled=true)
    public void learnBoundedListOracleTest() {
        @SuppressWarnings("unchecked")
        Word<PSymbolInstance>[] ces = new Word[] {
                Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(POP, dv(1))),
                Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(INSERT, dv(1), dv(2)),
                        new PSymbolInstance(INSERT, dv(3), dv(4)), new PSymbolInstance(POP, dv(0)),
                        new PSymbolInstance(POP, dv(2))),
                Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(PUSH, dv(0)),
                        new PSymbolInstance(INSERT, dv(0), dv(1)), new PSymbolInstance(POP, dv(1))) };

        Hypothesis result = learnBoundedListDWOracle(3, false, ces);
        Assert.assertEquals(result.getStates().size(), 5);
    }

    private Hypothesis learnBoundedListDWOracle(int size, boolean useNull, Word<PSymbolInstance>[] ces) {
        Constants consts = new Constants();
        if (useNull) {
            consts.put(new Constant(INT_TYPE, 1), new DataValue<>(INT_TYPE, BoundedList.NULL_VALUE));
        }

        BoundedListDataWordOracle dwOracle = new BoundedListDataWordOracle(() -> new BoundedList(size, useNull));

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        IntegerEqualityTheory dit = new IntegerEqualityTheory(INT_TYPE);
        teachers.put(INT_TYPE, dit);

        ConstraintSolver solver = ConstraintSolverFactory.createZ3ConstraintSolver();
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, consts, solver);

        SDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp),
                teachers, consts, solver);

        List<ParameterizedSymbol> alphabet = Arrays.asList(INSERT, PUSH, POP);
        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, false,
                alphabet.toArray(new ParameterizedSymbol[alphabet.size()]));

        rastar.learn();
        Hypothesis hypothesis = rastar.getHypothesis();

        for (Word<PSymbolInstance> ce : ces) {
            rastar.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, dwOracle.answerQuery(ce)));
            rastar.learn();
            hypothesis = rastar.getHypothesis();
        }
        return hypothesis;
    }
}
