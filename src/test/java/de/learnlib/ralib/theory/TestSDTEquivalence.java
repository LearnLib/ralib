package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.SDTGuard.EqualityGuard;
import de.learnlib.ralib.theory.SDTGuard.IntervalGuard;

public class TestSDTEquivalence {

	@Test
	public void testSelfEquivalenceIneqTheory() {
		DataType type = new DataType("double");
		SuffixValue s1 = new SuffixValue(type, 1);
		Register r1 = new Register(type, 1);
		Register r2 = new Register(type, 2);

		SDT sdt = new SDT(Map.of(
				IntervalGuard.greaterGuard(s1, r2), SDTLeaf.REJECTING,
				new EqualityGuard(s1, r2), SDTLeaf.ACCEPTING,
				new IntervalGuard(s1, r1, r2), SDTLeaf.REJECTING,
				new EqualityGuard(s1, r1), SDTLeaf.ACCEPTING,
				IntervalGuard.lessGuard(s1, r1), SDTLeaf.REJECTING));

		Assert.assertTrue(SDT.equivalentUnderId(sdt, sdt));

		DataValue d0 = new DataValue(type, BigDecimal.ZERO);
		DataValue d1 = new DataValue(type, BigDecimal.ONE);
		SDTRelabeling relabeling = new SDTRelabeling();
		relabeling.put(r1, d0);
		relabeling.put(r2, d1);
		Bijection<DataValue> expected = new Bijection<>();
		expected.put(d0, d0);
		expected.put(d1, d1);

		SDT sdtDv = sdt.relabel(relabeling);
		Bijection<DataValue> actual = SDT.equivalentUnderBijection(sdtDv, sdtDv);
		Assert.assertEquals(actual, expected);
	}
}
