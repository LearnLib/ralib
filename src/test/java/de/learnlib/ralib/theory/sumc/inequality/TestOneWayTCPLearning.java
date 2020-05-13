package de.learnlib.ralib.theory.sumc.inequality;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

import de.learnlib.ralib.IOEquivalenceOracleBuilder;
import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.sumc.inequality.OneWayFreshTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.OneWayTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.TCPExample.Option;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleSumCInequalityTheory;

public class TestOneWayTCPLearning extends RaLibLearningTestSuite{
	
	@Test
	public void testOneWayTCPLearning() {
    	Double win = 100.0;
    	DoubleSumCInequalityTheory theory = new DoubleSumCInequalityTheory();
    	Constants consts = new Constants();
    	consts.setSumC(new SumConstants(new DataValue<Double>(OneWayTCPSUL.DOUBLE_TYPE, 1.0), 
    			new DataValue<Double>(OneWayTCPSUL.DOUBLE_TYPE, win)));
    	theory.setType(OneWayTCPSUL.DOUBLE_TYPE);
    	theory.setConstants(consts);
        		
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(OneWayTCPSUL.DOUBLE_TYPE, theory);

        OneWayTCPSUL sul = new OneWayTCPSUL(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();     
        
        super.getEquOracleBuilder()
        .setMaxRuns(10000);
        
        super.runIOLearningExperiments(sul, teachers, consts, false, jsolv, sul.getActionSymbols(), OneWayTCPSUL.ERROR);
	}
	
	@Test
	public void testOneWayFreshTCPLearning() {
    	Double win = 100.0;
    	DoubleSumCInequalityTheory theory = new DoubleSumCInequalityTheory();
    	Constants consts = new Constants();
    	consts.setSumC(new SumConstants(new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), 
    			new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)));
    	theory.setType(OneWayFreshTCPSUL.DOUBLE_TYPE);
    	theory.setConstants(consts);
    	theory.setCheckForFreshOutputs(true);
        		
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, theory);

        OneWayFreshTCPSUL sul = new OneWayFreshTCPSUL(win);
        sul.configure(Option.WIN_CONNECTING_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();    
        
        super.getEquOracleBuilder()
        .setMaxRuns(10000)
        .setMaxDepth(20)
        .setFreshProbability(0.1);
        
        super.runIOLearningExperiments(sul, teachers, consts, true, jsolv, sul.getActionSymbols(), OneWayFreshTCPSUL.ERROR);
	}
}
