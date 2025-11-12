package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.example.list.ArrayListDataWordOracle.ADD;
import static de.learnlib.ralib.example.list.ArrayListDataWordOracle.REMOVE;
import static de.learnlib.ralib.example.list.ArrayListDataWordOracle.TYPE;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.list.ArrayListDataWordOracle;
import de.learnlib.ralib.example.list.ArrayListWrapper;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class LearnArrayList {

	@Test
	public void testLearnArrayList() {
		DataWordOracle oracle = new ArrayListDataWordOracle(() -> new ArrayListWrapper());

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(TYPE, new DoubleInequalityTheory(TYPE));

		ConstraintSolver solver = new ConstraintSolver();
		Constants consts = new Constants();
		MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(oracle, teachers, consts, solver);

        SDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, new Constants(), solver);

        SLLambda learner = new SLLambda(mto, hypFactory, mlo, teachers, consts, false, solver, ADD, REMOVE);
        learner.learn();

        // round 1: find all locations
        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.valueOf(2))),
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.valueOf(3))),
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.valueOf(4))));
        learner.addCounterexample(new DefaultQuery<>(ce, false));
        learner.learn();

        // round 2: find remove after three adds
        ce = Word.fromSymbols(
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.valueOf(2))),
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.valueOf(3))),
        		new PSymbolInstance(REMOVE, new DataValue(TYPE, BigDecimal.valueOf(3))));
        learner.addCounterexample(new DefaultQuery<>(ce, true));
        learner.learn();

        // possible round 3: ordering of data values
        ce = Word.fromSymbols(
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.valueOf(3))),
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.valueOf(2))),
        		new PSymbolInstance(ADD, new DataValue(TYPE, BigDecimal.ONE)),
        		new PSymbolInstance(REMOVE, new DataValue(TYPE, BigDecimal.valueOf(2))));
        Hypothesis hyp = learner.getHypothesis();
        if (!hyp.accepts(ce)) {
        	learner.addCounterexample(new DefaultQuery<>(ce, true));
        	learner.learn();
        	hyp = learner.getHypothesis();
        }

        Assert.assertTrue(hyp.accepts(ce));

        Assert.assertEquals(hyp.getStates().size(), 5);
	}
}
