package de.learnlib.ralib.learning.rattt;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
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
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class LearnLoginTest extends RaLibTestSuite {

    @Test
    public void learnLoginExample() {
        
        Constants consts = new Constants();        
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();        
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));        
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));
        
        ConstraintSolver solver = new SimpleConstraintSolver();
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                dwOracle, teachers, new Constants(), solver);
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> 
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, 
                        new Constants(), solver);
        
        RaTTT rattt = new RaTTT(mto, hypFactory, slo, 
                consts, I_LOGIN, I_LOGOUT, I_REGISTER);
        
        rattt.learn();        
        RegisterAutomaton hyp = rattt.getHypothesis();        
        logger.log(Level.FINE, "HYP1: {0}", hyp);

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                        new DataValue(T_UID, 0), new DataValue(T_PWD, 0)),
                new PSymbolInstance(I_LOGIN, 
                        new DataValue(T_UID, 0), new DataValue(T_PWD, 0)));
    
        rattt.addCounterexample(new DefaultQuery<>(ce, sul.accepts(ce)));
    
        rattt.learn();        
        hyp = rattt.getHypothesis();        
        logger.log(Level.FINE, "HYP2: {0}", hyp);
        
        Assert.assertEquals(hyp.getStates().size(), 3);
        Assert.assertEquals(hyp.getTransitions().size(), 11);        

    }
}
