package de.learnlib.ralib.automata;

import java.math.BigDecimal;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class RaRunTest extends RaLibTestSuite {

	final DataType DT = new DataType("int");
	final InputSymbol A = new InputSymbol("a", new DataType[] {DT});

	@Test
	public void testMutableRARun() {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		RALocation l0 = ra.addInitialState();
		RALocation l1 = ra.addState();
		RALocation l2 = ra.addState();

		ParameterGenerator pgen = new ParameterGenerator();
		RegisterGenerator rgen = new RegisterGenerator();
		Parameter p1 = pgen.next(DT);
		Register r1 = rgen.next(DT);

		Expression<Boolean> gT = ExpressionUtil.TRUE;
		Expression<Boolean> gEq = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
		Expression<Boolean> gNe = new NumericBooleanExpression(r1, NumericComparator.NE, p1);

		VarMapping<Register, SymbolicDataValue> storeP1 = new VarMapping<>();
		storeP1.put(r1, p1);

		Assignment assP1 = new Assignment(storeP1);
		Assignment assNo = new Assignment(new VarMapping<>());

		ra.addTransition(l0, A, new InputTransition(gT, A, l0, l1, assP1));
		ra.addTransition(l1, A, new InputTransition(gEq, A, l1, l0, assNo));
		ra.addTransition(l1, A, new InputTransition(gNe, A, l1, l2, assP1));
		ra.addTransition(l2, A, new InputTransition(gEq, A, l2, l1, assP1));
		ra.addTransition(l2, A, new InputTransition(gNe, A, l2, l0, assNo));

		DataValue dv1 = new DataValue(DT, BigDecimal.ONE);
		DataValue dv2 = new DataValue(DT, BigDecimal.valueOf(2));
		PSymbolInstance psi1 = new PSymbolInstance(A, dv1);
		PSymbolInstance psi2 = new PSymbolInstance(A, dv2);
		Word<PSymbolInstance> word1 = Word.fromSymbols(psi1, psi1, psi2);

		RARun run = ra.getRun(word1);

		Assert.assertEquals(run.getLocation(0), l0);
		Assert.assertEquals(run.getLocation(1), l1);
		Assert.assertEquals(run.getLocation(2), l0);
		Assert.assertEquals(run.getLocation(3), l1);

		Assert.assertTrue(run.getValuation(0).isEmpty());
		Assert.assertEquals(run.getValuation(1).get(r1), dv1);
		Assert.assertEquals(run.getValuation(3).get(r1), dv2);

		Assert.assertEquals(run.getTransitionSymbol(1), psi1);
		Assert.assertEquals(run.getTransitionSymbol(2), psi1);
		Assert.assertEquals(run.getTransitionSymbol(3), psi2);
	}
}
