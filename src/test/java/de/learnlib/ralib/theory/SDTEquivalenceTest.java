package de.learnlib.ralib.theory;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.smt.ConstraintSolver;

public class SDTEquivalenceTest extends RaLibTestSuite {

	private static DataType DT = new DataType("int");

	@Test
	public void testEquivalence() {
		ConstraintSolver solver = new ConstraintSolver();

		SuffixValue s1 = new SuffixValue(DT, 1);
		SuffixValue s2 = new SuffixValue(DT, 2);
		Register r1 = new Register(DT, 1);
		Register r2 = new Register(DT, 2);

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

		boolean equiv1 = sdt1.isEquivalent(sdt2, solver);
		boolean equiv2 = sdt1.isEquivalent(sdt3, solver);
		boolean equiv3 = sdt2.isEquivalent(sdt3, solver);

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

		boolean equiv4 = sdt4.isEquivalent(sdt5, solver);
		Assert.assertFalse(equiv4);
	}

	@Test
	public void testEquivalenceUnderBijection() {
		ConstraintSolver solver = new ConstraintSolver();

		SuffixValue s1 = new SuffixValue(DT, 1);
		SuffixValue s2 = new SuffixValue(DT, 2);
		Register r1 = new Register(DT, 1);
		Register r2 = new Register(DT, 2);
		Register r3 = new Register(DT, 3);
		Register r4 = new Register(DT, 4);

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

		Bijection bijection1 = new Bijection();
		bijection1.put(r1, r3);
		bijection1.put(r2, r4);
		Bijection bijection2 = new Bijection();
		bijection2.put(r1, r3);

		Bijection bi1 = SDT.equivalentUnderBijection(sdt1, sdt2, solver);
		Bijection bi2 = SDT.equivalentUnderBijection(sdt1, sdt2, bijection1, solver);
		Bijection bi3 = SDT.equivalentUnderBijection(sdt1, sdt2, bijection2, solver);

		Assert.assertEquals(bi1.size(), 2);
		Assert.assertEquals(bi1.size(), 2);
		Assert.assertTrue(bi1.get(r1).equals(r3));

		Assert.assertEquals(bi2.size(), 2);
		Assert.assertEquals(bi2.size(), 2);
		Assert.assertTrue(bi2.get(r1).equals(r3));

		Assert.assertEquals(bi3.size(), 2);
		Assert.assertEquals(bi3.size(), 2);
		Assert.assertTrue(bi3.get(r1).equals(r3));
	}
}
