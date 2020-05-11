package de.learnlib.ralib.theory.sumc.equality;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.equivalence.HypVerify;
import de.learnlib.ralib.equivalence.IOCounterExampleLoopRemover;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.example.sumc.equality.SumCFIFOSUL;
import de.learnlib.ralib.example.sumc.equality.SumCFreshFIFOSUL;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.CanonizingSULOracle;
import de.learnlib.ralib.sul.DeterminizerDataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerSumCEqualityTheory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;

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
