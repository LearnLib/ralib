package de.learnlib.ralib.data;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
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
        ValueCanonizer canonizer = ValueCanonizer.buildNew(theories, new Constants());
        DataValue[] rcvd = canonizer.canonize(dv(0,1,2), true);
        test(rcvd, 0,1,2);
        rcvd = canonizer.canonize(dv(50,100,1), false);
        test(rcvd, 3, 4, 1);
        rcvd = canonizer.canonize(dv( 5, 6, 1, 4), true);
        test(rcvd, 5, 6, 1, 100 );
	}
	
	private DataValue [] dv(int ... nums) {
		DataValue[] dvs = Arrays.stream(nums).mapToObj(num -> new DataValue(T_INT, new Integer(num))).toArray(DataValue []::new);
		return dvs;
	}
	
	private void test(DataValue [] rcvd, int ... expected) {
		Assert.assertEquals(rcvd.length, expected.length);
		for (int i = 0; i < rcvd.length; i ++) {
			Assert.assertEquals(((Integer)rcvd[i].id).intValue(), expected[i]);
		}
	}

}
