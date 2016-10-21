package de.learnlib.ralib.theory.succ;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.example.succ.ModerateTCPSUL;
import de.learnlib.ralib.example.succ.OneWayFreshTCPSUL;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.automata.concepts.Output;
import net.automatalib.words.Word;

public class TestSuccFresh extends RaLibTestSuite{
	
	@Test
	public void testCanonizer() {
		Double win = 10.0;
		DataValueConstructor<Double> b = new DataValueConstructor<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE);
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        SumCDoubleInequalityTheory theory = new SumCDoubleInequalityTheory(OneWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)), // for window size
        		Collections.emptyList());
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, theory);
        
        ValueCanonizer canonizer = new ValueCanonizer(teachers);
        FreshValue<Double> fv = new FreshValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 0.0);
        SumCDataValue<Double> sc = new SumCDataValue<Double> (fv, new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0));
        
        
        canonizer.canonize(new DataValue [] {
        		b.fv(0.0)
        }, true);
        
        DataValue<Double>[] outpt = canonizer.canonize(new DataValue [] {
        		b.fv(659.0)
        }, false);
        
        outpt = canonizer.canonize(new DataValue [] {
        		b.intv(4.0, 0.0 , 11.0),
        		b.sumcv(outpt[0].getId(), 1.0), 
        		b.sumcv(0.0, 1.0)
        }, true);
        
        Assert.assertEquals(outpt[0].getId(), 330.0); // the ValueMapper 
        Assert.assertEquals(outpt[1].getId(), 660.0);
        Assert.assertEquals(outpt[2].getId(), 2.0);
        
        
        System.out.println(outpt);
        
        
	}
	
    @Test
    public void testModerateTCPTree() {
    	Double win = 100.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        SumCDoubleInequalityTheory theory = new SumCDoubleInequalityTheory(OneWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)), // for window size
        		Collections.emptyList());
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, theory);

        DataWordSUL sul = new OneWayFreshTCPSUL(win);
        sul = new DeterminedDataWordSUL(() -> new ValueCanonizer(teachers), sul);
        SULOracle oracle = new SULOracle(sul, OneWayFreshTCPSUL.ERROR);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();       
        
        
//        canonizer.canonize(new IntervalDataGuard<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), inverse)
        
        final Word<PSymbolInstance> testWord = Word.fromSymbols(
        		new PSymbolInstance(OneWayFreshTCPSUL.ICONNECT,
                        new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 0.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.OK,
                		new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 101.0)),
        		new PSymbolInstance(OneWayFreshTCPSUL.ISYN, 
                		new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 0.0),
                		new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.OK),
                new PSymbolInstance(OneWayFreshTCPSUL.ISYNACK,
                        new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0),
                        new DataValue(OneWayFreshTCPSUL.DOUBLE_TYPE, 2.0)),
                new PSymbolInstance(ModerateTCPSUL.OK));
        
        Word<PSymbolInstance> canonizedTrace = oracle.trace(testWord);
        
        sul.pre();
        
        //Word<PSymbolInstance> obtainedWord = testWord.transform(sym -> canonizer.canonize(sym, false));
        
        
    }
    
//    private Word<PSymbolInstance> run(DataWordSUL sul, Word<PSymbolInstance> word) {
//    	
//    	
//    }
}