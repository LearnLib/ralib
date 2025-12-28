package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;

public class SDTEquivalenceTest extends RaLibTestSuite {

	private static DataType DT = new DataType("int");

	@Test
	public void testEquivalence() {

		SuffixValue s1 = new SuffixValue(DT, 1);
		SuffixValue s2 = new SuffixValue(DT, 2);
		DataValue r1 = new DataValue(DT, BigDecimal.ONE);
        DataValue r2 = new DataValue(DT, BigDecimal.valueOf(2.0));

		SDT sdt1 = new SDT(Map.of(
				new SDTGuard.EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
				new SDTGuard.DisequalityGuard(s1, r1), SDTLeaf.REJECTING));
		SDT sdt2 = new SDT(Map.of(
				new SDTGuard.EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
				new SDTGuard.DisequalityGuard(s1, r1), SDTLeaf.REJECTING));
		SDT sdt3 = new SDT(Map.of(
				new SDTGuard.IntervalGuard(s1, null, r1), SDTLeaf.REJECTING,
				new SDTGuard.EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
				new SDTGuard.IntervalGuard(s1, r1, null), SDTLeaf.REJECTING));

		boolean equiv1 = sdt1.isEquivalent(sdt2, new Bijection<>());
		boolean equiv2 = sdt1.isEquivalent(sdt3, new Bijection<>());
		boolean equiv3 = sdt2.isEquivalent(sdt3, new Bijection<>());

		Assert.assertTrue(equiv1);
		Assert.assertTrue(equiv2);
		Assert.assertTrue(equiv3);

		SDT sdt4 = new SDT(Map.of(
				new SDTGuard.EqualityGuard(s1, r1), new SDT(Map.of(
						new SDTGuard.EqualityGuard(s2, r2), SDTLeaf.ACCEPTING,
						new SDTGuard.DisequalityGuard(s2, r2), SDTLeaf.REJECTING)),
				new SDTGuard.DisequalityGuard(s1, r1), new SDT(Map.of(
						new SDTGuard.EqualityGuard(s2, r2), SDTLeaf.REJECTING,
						new SDTGuard.DisequalityGuard(s2, r2), SDTLeaf.ACCEPTING))));
		SDT sdt5 = new SDT(Map.of(
				new SDTGuard.EqualityGuard(s1, r1), new SDT(Map.of(
						new SDTGuard.SDTTrueGuard(s2), SDTLeaf.REJECTING)),
				new SDTGuard.DisequalityGuard(s1, r1), new SDT(Map.of(
						new SDTGuard.EqualityGuard(s2, r2), SDTLeaf.REJECTING,
						new SDTGuard.DisequalityGuard(s2, r2), SDTLeaf.ACCEPTING))));

		boolean equiv4 = sdt4.isEquivalent(sdt5, new Bijection<>());
		Assert.assertFalse(equiv4);
	}

	@Test
	public void testEquivalenceUnderBijection() {

		SuffixValue s1 = new SuffixValue(DT, 1);
		SuffixValue s2 = new SuffixValue(DT, 2);
        DataValue r1 = new DataValue(DT, BigDecimal.valueOf(1.0));
        DataValue r2 = new DataValue(DT, BigDecimal.valueOf(2.0));
        DataValue r3 = new DataValue(DT, BigDecimal.valueOf(3.0));
        DataValue r4 = new DataValue(DT, BigDecimal.valueOf(4.0));

		SDT sdt1 = new SDT(Map.of(
				new SDTGuard.EqualityGuard(s1, r1), new SDT(Map.of(
						new SDTGuard.EqualityGuard(s2, r2), SDTLeaf.ACCEPTING,
						new SDTGuard.DisequalityGuard(s2, r2), SDTLeaf.REJECTING)),
				new SDTGuard.DisequalityGuard(s1, r1), new SDT(Map.of(
						new SDTGuard.EqualityGuard(s2, r2), SDTLeaf.REJECTING,
						new SDTGuard.DisequalityGuard(s2, r2), SDTLeaf.ACCEPTING))));
		SDT sdt2 = new SDT(Map.of(
				new SDTGuard.EqualityGuard(s1, r3), new SDT(Map.of(
						new SDTGuard.EqualityGuard(s2, r4), SDTLeaf.ACCEPTING,
						new SDTGuard.DisequalityGuard(s2, r4), SDTLeaf.REJECTING)),
				new SDTGuard.DisequalityGuard(s1, r3), new SDT(Map.of(
						new SDTGuard.EqualityGuard(s2, r4), SDTLeaf.REJECTING,
						new SDTGuard.DisequalityGuard(s2, r4), SDTLeaf.ACCEPTING))));

		Bijection<DataValue> bijection1 = new Bijection<>();
		bijection1.put(r1, r3);
		bijection1.put(r2, r4);
		Bijection<DataValue> bijection2 = new Bijection<>();
		bijection2.put(r1, r3);

		Bijection<DataValue> bi1 = SDT.equivalentUnderBijection(sdt1, sdt2, new Bijection<>());
		Bijection<DataValue> bi2 = SDT.equivalentUnderBijection(sdt1, sdt2, bijection1);
		Bijection<DataValue> bi3 = SDT.equivalentUnderBijection(sdt1, sdt2, bijection2);

		Assert.assertEquals(bi1.size(), 2);
		Assert.assertEquals(bi1.size(), 2);
        Assert.assertEquals(r3, bi1.get(r1));

        assert bi2 != null;
        Assert.assertEquals(bi2.size(), 2);
		Assert.assertEquals(bi2.size(), 2);
        Assert.assertEquals(r3, bi2.get(r1));

        assert bi3 != null;
        Assert.assertEquals(bi3.size(), 2);
		Assert.assertEquals(bi3.size(), 2);
        Assert.assertEquals(r3, bi3.get(r1));
	}

}
