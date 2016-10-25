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
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.TraceFixer;
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
        
        ValueCanonizer canonizer = ValueCanonizer.buildNew(teachers);
        
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
	}
	
	
	@Test
	public void testSulWithCanonizer() {
		Double win = 100.0;
		DataValueConstructor<Double> b = new DataValueConstructor<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE);
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        SumCDoubleInequalityTheory theory = new SumCDoubleInequalityTheory(OneWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)), // for window size
        		Collections.emptyList());
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, theory);
        
        ValueCanonizer canonizer = ValueCanonizer.buildNew(teachers);
        
        OneWayFreshTCPSUL sul = new OneWayFreshTCPSUL(100.0);
        DeterminedDataWordSUL dsul = new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers), sul);
        
        final Word<PSymbolInstance> testWord = Word.fromSymbols(
        		new PSymbolInstance(OneWayFreshTCPSUL.ICONNECT),
                new PSymbolInstance(OneWayFreshTCPSUL.OK,
                		b.fv(1.0)),
        		new PSymbolInstance(OneWayFreshTCPSUL.ISYN, 
                		b.intv(1.5, b.sumcv(1.0, 1.0), b.sumcv(1.0, 100.0))),
                new PSymbolInstance(OneWayFreshTCPSUL.NOK),
                new PSymbolInstance(OneWayFreshTCPSUL.ISYN,
                		b.dv(1.0)),
                new PSymbolInstance(OneWayFreshTCPSUL.OK));
        
        BasicSULOracle oracle = new BasicSULOracle(dsul, OneWayFreshTCPSUL.ERROR);
        Word<PSymbolInstance> result = oracle.trace(testWord);
        Assert.assertEquals(testWord.getSymbol(5).getBaseSymbol(), OneWayFreshTCPSUL.OK);
        Assert.assertEquals(testWord.getSymbol(3).getBaseSymbol(), OneWayFreshTCPSUL.NOK);
	}
	
	@Test
	public void testTraceFixer() {
		Double win = 100.0;
		DataValueConstructor<Double> b = new DataValueConstructor<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE);
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        SumCDoubleInequalityTheory theory = new SumCDoubleInequalityTheory(OneWayFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
        				new DataValue<Double>(OneWayFreshTCPSUL.DOUBLE_TYPE, win)), // for window size
        		Collections.emptyList());
        teachers.put(OneWayFreshTCPSUL.DOUBLE_TYPE, theory);
        
        ValueCanonizer canonizer = ValueCanonizer.buildNew(teachers);
        TraceFixer fixer = new TraceFixer(teachers);
        
        final Word<PSymbolInstance> testWord = Word.fromSymbols(
        		new PSymbolInstance(OneWayFreshTCPSUL.ISYN, 
                		b.intv(1.5, b.sumcv(1.0, 1.0), b.sumcv(1.0, 100.0))),
                new PSymbolInstance(OneWayFreshTCPSUL.ISYN,
                		b.intv(2.0, b.dv(1.5), b.sumcv(1.5, 1.0)), b.sumcv(101.0, 1.0)));
        Word<PSymbolInstance> fixedTrace = fixer.fixTrace(testWord);
        Assert.assertEquals(fixedTrace.lastSymbol().getParameterValues()[1].getClass(), FreshValue.class);
        Assert.assertEquals(fixedTrace.lastSymbol().getParameterValues()[0].getClass(), IntervalDataValue.class);
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
        sul = new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers), sul);
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
