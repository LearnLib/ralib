package de.learnlib.ralib.theory.inequality;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class TestIneqGuardLogic  extends RaLibTestSuite {
	private SymbolicDataValue a;
	private SymbolicDataValue b;
	private SymbolicDataValue c;
	private SymbolicDataValue d;
	private SuffixValue p;
	private InequalityGuardLogic logic;
	
	public TestIneqGuardLogic() {
		SymbolicDataValueGenerator gen = new SymbolicDataValueGenerator.SuffixValueGenerator();
		DataType type = new DataType("test", Integer.class);
		a = gen.next(type);
		b = gen.next(type);
		c = gen.next(type);
		d = gen.next(type);
		p = (SuffixValue) gen.next(type);
		logic = new InequalityGuardLogic();
		
	}
	
	@Test
	public void testConjunctionIntIntEq() {
		IntervalGuard int1 = new IntervalGuard(p, d, true, a, false);
		IntervalGuard int2 = new IntervalGuard(p, a, false, null, null);
		SDTGuard res = logic.conjunction(int1, int2);
		Assert.assertEquals(res, new EqualityGuard(p, a));
		res = logic.conjunction(int2, int1);
		Assert.assertEquals(res, new EqualityGuard(p, a));
	}
	
	@Test
	public void testConjunctionGrIntInt() {
		IntervalGuard int1 = new IntervalGuard(p, a, false, null, null);
		IntervalGuard int2 = new IntervalGuard(p, a, true, b, true);
		SDTGuard res = logic.conjunction(int1, int2);
		Assert.assertEquals(res, new IntervalGuard(p, a, false, b, true));
		res = logic.conjunction(int2, int1);
		Assert.assertEquals(res, new IntervalGuard(p, a, false, b, true));
	}
	
	@Test
	public void testConjunctionSmGrInt() {
		IntervalGuard int1 = new IntervalGuard(p, null, null, a, false);
		IntervalGuard int2 = new IntervalGuard(p, b, false, null, null);
		SDTGuard res = logic.conjunction(int1, int2);
		Assert.assertEquals(res, new IntervalGuard(p, b, false, a, false));
		res = logic.conjunction(int2, int1);
		Assert.assertEquals(res, new IntervalGuard(p, b, false, a, false));
		
	}
	
	@Test
	public void testDisjunctionIntIntTrue() {
		IntervalGuard int1 = new IntervalGuard(p, null, null, a, false);
		IntervalGuard int2 = new IntervalGuard(p, a, true, null, null);
		SDTGuard res = logic.disjunction(int1, int2);
		Assert.assertEquals(res, new SDTTrueGuard(p));
		res = logic.disjunction(int2, int1);
		Assert.assertEquals(res, new SDTTrueGuard(p));
	}
	
	@Test
	public void testDisjunctionIntIntDiseq() {
		IntervalGuard int1 = new IntervalGuard(p, null, null, a, true);
		IntervalGuard int2 = new IntervalGuard(p, a, true, null, null);
		SDTGuard res = logic.disjunction(int1, int2);
		Assert.assertEquals(res, new DisequalityGuard(p, a));
		res = logic.disjunction(int2, int1);
		Assert.assertEquals(res, new DisequalityGuard(p, a));
	}
	
	@Test
	public void testDisjunctionIntIntInt() {
		IntervalGuard int1 = new IntervalGuard(p, null, null, a, false);
		IntervalGuard int2 = new IntervalGuard(p, a, true, b, true);
		SDTGuard res = logic.disjunction(int1, int2);
		Assert.assertEquals(res, new IntervalGuard(p, null, null, b, true));
		res = logic.disjunction(int2, int1);
		Assert.assertEquals(res, new IntervalGuard(p, null, null, b, true));
		
		IntervalGuard int3 = new IntervalGuard(p, d, true, a, false);
		IntervalGuard int4 = new IntervalGuard(p, a, true, b, true);
		res = logic.disjunction(int3, int4);
		Assert.assertEquals(res, new IntervalGuard(p, d, true, b, true));
		res = logic.disjunction(int3, int4);
		Assert.assertEquals(res, new IntervalGuard(p, d, true, b, true));
	}
}
