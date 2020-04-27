package de.learnlib.ralib;

import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.Theory;

public class LearningExperiment {
	private Constants consts = new Constants();
	private ConstraintSolver solver = new SimpleConstraintSolver();
	private Map<DataType, Theory> teachers = null;
	
	

}
