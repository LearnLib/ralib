package de.learnlib.ralib.learning.rattt;

import static de.learnlib.ralib.example.llambda.LLambdaAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.llambda.LLambdaAutomatonExample.A;
import static de.learnlib.ralib.example.llambda.LLambdaAutomatonExample.B;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class LearnLLambdaTest extends RaLibTestSuite {
	@Test
	public void learnLLambdaAutomaton() {

		Constants consts = new Constants();        
	    RegisterAutomaton sul = AUTOMATON;
	    DataWordOracle dwOracle = new SimulatorOracle(sul);

	    final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	    ConstraintSolver solver = new SimpleConstraintSolver();

	    MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
	              dwOracle, teachers, new Constants(), solver);

        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> 
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, 
                        new Constants(), solver);
        
        RaTTT rattt = new RaTTT(mto, hypFactory, slo, consts, false, false, true, A, B);
        
        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(B),
        		new PSymbolInstance(B),
        		new PSymbolInstance(B),
        		new PSymbolInstance(A),
        		new PSymbolInstance(B),
        		new PSymbolInstance(B),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A),
        		new PSymbolInstance(A));
        
        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
        rattt.setIndicesToSearch(0);

        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);
        
        Assert.assertEquals(hyp.getStates().size(), 6);
        Assert.assertEquals(hyp.getTransitions().size(), 12);
	}
}
