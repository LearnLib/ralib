package de.learnlib.ralib.automata;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class RaRunTest extends RaLibTestSuite {

	final DataType DT = new DataType("int");
	final InputSymbol A = new InputSymbol("a", new DataType[] {DT});
	final OutputSymbol B = new OutputSymbol("b", new DataType[] {DT, DT, DT, DT});

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

	@Test
	public void testGetGuard() {
		Parameter p1 = new Parameter(DT, 1);
		Parameter p2 = new Parameter(DT, 2);
		Parameter p3 = new Parameter(DT, 3);
		Parameter p4 = new Parameter(DT, 4);
		Register r1 = new Register(DT, 1);
		Constant c1 = new Constant(DT, 1);
		Constants consts = new Constants();
		consts.put(c1, new DataValue(DT, BigDecimal.valueOf(-1)));

		MutableRegisterAutomaton ra = new MutableRegisterAutomaton(consts);
		RALocation l0 = ra.addInitialState();
		RALocation l1 = ra.addState();
		RALocation l2 = ra.addState();

		VarMapping<Register, Parameter> mapping = new VarMapping<>();
		mapping.put(r1, p1);
		Assignment assign = new Assignment(mapping);
		Collection<Parameter> fresh = new ArrayList<>();
		fresh.add(p2);
		fresh.add(p1);
		VarMapping<Parameter, SymbolicDataValue> outmap = new VarMapping<>();
		outmap.put(p3, r1);
		outmap.put(p4, c1);
		OutputMapping out = new OutputMapping(fresh, outmap);

		ra.addTransition(l0, A, new InputTransition(ExpressionUtil.TRUE, A, l0, l1, assign));
		ra.addTransition(l1, B, new OutputTransition(ExpressionUtil.TRUE, out, B, l1, l2, new Assignment(new VarMapping<>())));

		Word<PSymbolInstance> word = Word.fromSymbols(
				new PSymbolInstance(A, new DataValue(DT, BigDecimal.ZERO)),
				new PSymbolInstance(B, new DataValue(DT, BigDecimal.ONE),
						new DataValue(DT, BigDecimal.valueOf(2)),
						new DataValue(DT, BigDecimal.ZERO),
						new DataValue(DT, consts.get(c1).getValue())));

		RARun run = ra.getRun(word);
		Expression<Boolean> guard = run.getGuard(2, consts);

		Assert.assertEquals(guard.toString(),
				"((((('p1' != 0) && ('p1' != -1)) && "
				+ "((('p2' != 'p1') && ('p2' != 0)) && ('p2' != -1))) && "
				+ "('p3' == 0)) && ('p4' == -1))");
	}
}
