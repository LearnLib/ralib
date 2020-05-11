package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.example.priority.PrioritizedListSUL;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;

public class LearnPLIOTest extends RaLibLearningTestSuite  {
	   @Test
	    public void learnPrioritizedListIO() {
	        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	        DoubleInequalityTheory dit = 
	                new DoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE);
	        
	        dit.setUseSuffixOpt(false);
	        teachers.put(PriorityQueueSUL.DOUBLE_TYPE, dit);
	                
	        
	        final Constants consts = new Constants();

	        PrioritizedListSUL sul = new PrioritizedListSUL(3, new int [] {1, 2, 0});
	        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
	        
	        super.runIOLearningExperiments(sul, teachers, consts, false, jsolv, sul.getActionSymbols(), PrioritizedListSUL.ERROR);
	    }
}
