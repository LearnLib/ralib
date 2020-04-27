package de.learnlib.ralib.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.example.succ.IntHardFreshTCPSUL;
import de.learnlib.ralib.mapper.SymbolicDeterminizer;
import de.learnlib.ralib.mapper.MultiTheoryDeterminizer;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.tools.theories.SumCIntegerInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.InputSymbol;

public class ValueCanonizerTest  extends RaLibTestSuite {
    public static final DataType T_INT = new DataType("T_uid", Integer.class);
    public static final InputSymbol IN = 
            new InputSymbol("in", new DataType[] {T_INT}); 
    
    public static final InputSymbol OUT = 
            new InputSymbol("out", new DataType[] {T_INT}); 
    

    @Test
	public void testCanonizerEqu() {
        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(T_INT, new IntegerEqualityTheory(T_INT));
        MultiTheoryDeterminizer canonizer = new MultiTheoryDeterminizer(theories, new Constants());
        DataValue[] rcvd = canonizer.canonize(dv(T_INT, 0,1,2), true);
        test(rcvd, 0,1,2);
        rcvd = canonizer.canonize(dv(T_INT, 50,100,1), false);
        test(rcvd, 3, 4, 1);
        rcvd = canonizer.canonize(dv(T_INT, 5, 6, 1, 4), true);
        test(rcvd, 5, 6, 1, 100 );
	}
    
    @Test
	public void testConcreteCanonizerIneq() {
		Integer win = 1000;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Integer>(IntHardFreshTCPSUL.INT_TYPE, 1), // for successor
				new DataValue<Integer>(IntHardFreshTCPSUL.INT_TYPE, win)};
        SumCIntegerInequalityTheory sumCTheory = new SumCIntegerInequalityTheory(IntHardFreshTCPSUL.INT_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(IntHardFreshTCPSUL.INT_TYPE, 
               sumCTheory);

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
    	DataValueConstructor<Integer> b = new DataValueConstructor<>(IntHardFreshTCPSUL.INT_TYPE);
        MultiTheoryDeterminizer canonizer = new MultiTheoryDeterminizer(teachers, consts);
        DataValue[] dvs = dv(IntHardFreshTCPSUL.INT_TYPE, 0, 1, 2, 10000, 20000, 30000, 11000); 
        DataValue[] result = canonizer.canonize(dvs, false);
        Assert.assertEquals(Arrays.stream(result).filter(d -> d instanceof FreshValue).count(), 4);
	}
    
    @Test
	public void testSymbolicCanonizerIneq() {
		Integer win = 1000;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Integer>(IntHardFreshTCPSUL.INT_TYPE, 1), // for successor
				new DataValue<Integer>(IntHardFreshTCPSUL.INT_TYPE, win)};
        SumCIntegerInequalityTheory sumCTheory = new SumCIntegerInequalityTheory(IntHardFreshTCPSUL.INT_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(IntHardFreshTCPSUL.INT_TYPE, 
               sumCTheory);

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
    	DataValueConstructor<Integer> b = new DataValueConstructor<>(IntHardFreshTCPSUL.INT_TYPE);
    	SymbolicDeterminizer<Integer> valueMapper = new SymbolicDeterminizer<Integer>(sumCTheory, IntHardFreshTCPSUL.INT_TYPE);
    	MultiTheoryDeterminizer valueCanonizer = MultiTheoryDeterminizer.newCustom(Collections.singletonMap(IntHardFreshTCPSUL.INT_TYPE, valueMapper), consts);
        DataValue[] dvs = new DataValue[]{
        		b.fv(0), b.sumcv(0, 1), b.sumcv(1, 1), b.fv(10000), b.fv(20000), 
        		b.fv(30000), //b.intv(15000, 10000, 20000),
        		b.intv(17500, 15000, 20000), b.sumcv(17500, 1), b.sumcv(17500, 1000), 
        		b.intv(18000, b.sumcv(17500, 1), b.sumcv(17500, 1000))  
        };
        DataValue[] result = valueCanonizer.canonize(dvs, false);
        
        Assert.assertEquals(Arrays.stream(result).filter(d -> d instanceof IntervalDataValue).count(), 1);
        Assert.assertEquals(Arrays.stream(result).filter(d -> d instanceof SumCDataValue).count(), 4);
	}
    

	private DataValue [] dv(DataType dType, int ... nums) {
		DataValue[] dvs = Arrays.stream(nums).mapToObj(num -> new DataValue<Integer>(dType, new Integer(num))).toArray(DataValue []::new);
		return dvs;
	}
	
	private void test(DataValue [] rcvd, int ... expected) {
		Assert.assertEquals(rcvd.length, expected.length);
		for (int i = 0; i < rcvd.length; i ++) {
			Assert.assertEquals(((Integer)rcvd[i].id).intValue(), expected[i]);
		}
	}

}
