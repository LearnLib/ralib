package de.learnlib.ralib.theory.sumc.equality;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.sumc.equality.SumCFIFOSUL;
import de.learnlib.ralib.example.sumc.equality.SumCFreshFIFOSUL;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerSumCEqualityTheory;

public class TestSumCFIFOLearning extends RaLibLearningTestSuite {
	@Test
	public void learnSumCFIFO() {

		int capacity = 3;
		int sumc = 1;
		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		DataValue[] sumConsts = new DataValue[] { new DataValue<Integer>(SumCFIFOSUL.INT_TYPE, sumc), };

		IntegerSumCEqualityTheory theory = new IntegerSumCEqualityTheory();
		teachers.put(SumCFIFOSUL.INT_TYPE, theory);
		theory.setCheckForFreshOutputs(false);
		theory.setUseSuffixOpt(true);
		theory.setType(SumCFIFOSUL.INT_TYPE);
		SumCFIFOSUL sul = new SumCFIFOSUL(capacity, sumc);

		JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
		Constants consts = new Constants(new SumConstants(sumConsts));
		theory.setConstants(consts);
		
		super.runIOLearningExperiments(sul, teachers, consts, false, jsolv, sul.getActionSymbols(), SumCFIFOSUL.ERROR);
	}
	
	
	@Test
	public void learnSumCFreshFIFO() {

		int capacity = 3;
		int sumc = 1;
		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		DataValue[] sumConsts = new DataValue[] { new DataValue<Integer>(SumCFreshFIFOSUL.INT_TYPE, sumc), };

		IntegerSumCEqualityTheory theory = new IntegerSumCEqualityTheory();
		teachers.put(SumCFreshFIFOSUL.INT_TYPE, theory);
		theory.setCheckForFreshOutputs(true);
		theory.setUseSuffixOpt(true);
		theory.setType(SumCFreshFIFOSUL.INT_TYPE);
		SumCFreshFIFOSUL sul = new SumCFreshFIFOSUL(capacity, sumc);

		JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
		Constants consts = new Constants(new SumConstants(sumConsts));
		theory.setConstants(consts);
		
		super.runIOLearningExperiments(sul, teachers, consts, true, jsolv, sul.getActionSymbols(), SumCFreshFIFOSUL.ERROR);
	}
}
