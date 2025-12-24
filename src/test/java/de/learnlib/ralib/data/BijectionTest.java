package de.learnlib.ralib.data;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.SymbolicDataValue.Register;

public class BijectionTest extends RaLibTestSuite {

	private static final DataType DT = new DataType("type");

	@Test
	public void testBijection() {
		Register r1 = new Register(DT, 1);
		Register r2 = new Register(DT, 2);
		Register r3 = new Register(DT, 3);

		Bijection bi1 = new Bijection();
		bi1.put(r1, r2);
		bi1.put(r2, r1);

		Assert.assertTrue(bi1.get(r1).equals(r2));
		Assert.assertTrue(bi1.get(r2).equals(r1));

		bi1.put(r2, r3);
		Assert.assertTrue(bi1.get(r1).equals(r2));
		Assert.assertTrue(bi1.get(r2).equals(r3));

		Bijection bi2 = bi1.inverse();
		Assert.assertTrue(bi2.get(r2).equals(r1));
		Assert.assertTrue(bi2.get(r3).equals(r2));

		Bijection bi3 = new Bijection();
		bi3.putAll(bi2);
		Assert.assertTrue(bi3.get(r2).equals(r1));
		Assert.assertTrue(bi3.get(r3).equals(r2));
	}

	@Test
	public void testCompose() {
		Register r1 = new Register(DT, 1);
		Register r2 = new Register(DT, 2);
		Register r3 = new Register(DT, 3);
		Register r4 = new Register(DT, 4);

		Bijection bi1 = new Bijection();
		bi1.put(r1, r2);
		bi1.put(r2, r3);

		Bijection bi2 = new Bijection();
		bi2.put(r2, r3);
		bi2.put(r3, r4);

		Bijection composition = bi1.compose(bi2);
		Assert.assertEquals(composition.size(), 2);
		Assert.assertTrue(composition.get(r1).equals(r3));
		Assert.assertTrue(composition.get(r2).equals(r4));
	}

	@Test
	public void testException() {
		Register r1 = new Register(DT, 1);
		Register r2 = new Register(DT, 2);
		Register r3 = new Register(DT, 3);

		Map<Register, Register> map = new LinkedHashMap<>();
		map.put(r1, r3);
		map.put(r2, r3);

		Bijection bi = new Bijection();
		boolean exceptionCaught = false;
		try {
			bi.putAll(map);
		} catch(Exception e) {
			exceptionCaught = true;
		}

		Assert.assertTrue(exceptionCaught);
		Assert.assertEquals(bi.size(), 0);
	}
}
