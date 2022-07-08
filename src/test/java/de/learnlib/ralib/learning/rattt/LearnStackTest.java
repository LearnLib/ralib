package de.learnlib.ralib.learning.rattt;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.T_INT;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class LearnStackTest extends RaLibTestSuite {
	@Test
	public void learnStackExample() {

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
        
        RaTTT rattt = new RaTTT(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);
        
        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));
        

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
    
        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);
        
        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));
        

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
    
        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
	}
	
	@Test
	public void learnStackExampleSwitchedCE() {
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
        
        RaTTT rattt = new RaTTT(mto, hypFactory, slo, consts, false, false, I_PUSH, I_POP);
        
        rattt.learn();
        RegisterAutomaton hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP0: {0}", hyp);
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP1: {0}", hyp);
        
        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 2)));
        

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
    
        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);

        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 0)));
        

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
    
        rattt.learn();
        hyp = rattt.getHypothesis();
        logger.log(Level.FINE, "HYP2: {0}", hyp);
        
        ce = Word.fromSymbols(
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 0)),
        		new PSymbolInstance(I_PUSH, new DataValue(T_INT, 1)),
        		new PSymbolInstance(I_POP, new DataValue(T_INT, 1)));

        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
        rattt.learn();
        hyp = rattt.getHypothesis();
        
        Collection<SymbolicSuffix> suffixes = rattt.getDT().getSuffixes();
        Set<Word<ParameterizedSymbol>> suffixActions = suffixes.stream().map(s -> s.getActions()).collect(Collectors.toSet());
        Set<Word<ParameterizedSymbol>> expectedSuffixActions = ImmutableSet.of(
            Word.fromSymbols(),
            Word.fromSymbols(I_PUSH),
            Word.fromSymbols(I_PUSH, I_PUSH),
            Word.fromSymbols(I_POP),
            Word.fromSymbols(I_POP, I_POP));

        Assert.assertEquals(hyp.getStates().size(), 4);
        Assert.assertEquals(hyp.getTransitions().size(), 10);
        Assert.assertEquals(suffixActions, expectedSuffixActions);
	}
}
