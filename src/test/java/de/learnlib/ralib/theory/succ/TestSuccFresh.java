package de.learnlib.ralib.theory.succ;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.succ.ModerateTCPSUL;
import de.learnlib.ralib.example.succ.OneWayFreshTCPSUL;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TestSuccFresh extends RaLibTestSuite{
    @Test
    public void testModerateTCPTree() {
    	
    	Double win = 100.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(OneWayFreshTCPSUL.DOUBLE_TYPE,
                		Arrays.asList(
                				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
                				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)), // for window size
                		Collections.emptyList()));

        DataWordSUL sul = new OneWayFreshTCPSUL(win);
        sul = new DeterminedDataWordSUL(() -> new ValueCanonizer(teachers), sul);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        

        ValueCanonizer canonizer = new ValueCanonizer(teachers);
        
        final Word<PSymbolInstance> testWord = Word.fromSymbols(
        		new PSymbolInstance(ModerateTCPSUL.ICONNECT,
                        new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 1.0)),
                new PSymbolInstance(ModerateTCPSUL.OK,
                		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 101.0)),
        		new PSymbolInstance(ModerateTCPSUL.ISYN, 
                		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 1.0),
                		new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(ModerateTCPSUL.OK),
                new PSymbolInstance(ModerateTCPSUL.ISYNACK,
                        new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 1.0),
                        new DataValue(ModerateTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(ModerateTCPSUL.OK));
        
    }
}
