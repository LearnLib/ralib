package de.learnlib.ralib.automata;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
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
import net.automatalib.word.Word;

public class RaRunTest extends RaLibTestSuite {

	@Test
	public void testMutableRARun() {

		final DataType DT = new DataType("int", Integer.class);
		final InputSymbol A = new InputSymbol("a", new DataType[] {DT});

		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		RALocation l0 = ra.addInitialState();
		RALocation l1 = ra.addState();
		RALocation l2 = ra.addState();

		ParameterGenerator pgen = new ParameterGenerator();
		RegisterGenerator rgen = new RegisterGenerator();
		Parameter p1 = pgen.next(DT);
		Register r1 = rgen.next(DT);

		GuardExpression geeq = new AtomicGuardExpression<Parameter, Register>(p1, Relation.EQUALS, r1);
		GuardExpression geneq = new AtomicGuardExpression<Parameter, Register>(p1, Relation.NOT_EQUALS, r1);

		TransitionGuard gt = new TransitionGuard();
		TransitionGuard ge = new TransitionGuard(geeq);
		TransitionGuard gn = new TransitionGuard(geneq);

		VarMapping<Register, SymbolicDataValue> noStore = new VarMapping<>();
		VarMapping<Register, SymbolicDataValue> storeR1 = new VarMapping<>();
		storeR1.put(r1, p1);

		Assignment noAss = new Assignment(noStore);
		Assignment assR1 = new Assignment(storeR1);

		ra.addTransition(l0, A, new InputTransition(gt, A, l0, l1, assR1));
		ra.addTransition(l1, A, new InputTransition(ge, A, l1, l0, noAss));
		ra.addTransition(l1, A, new InputTransition(gn, A, l1, l2, assR1));
		ra.addTransition(l2, A, new InputTransition(ge, A, l2, l1, assR1));
		ra.addTransition(l2, A, new InputTransition(gn, A, l2, l0, noAss));


		DataValue<Integer> dv1 = new DataValue<>(DT, 1);
		DataValue<Integer> dv2 = new DataValue<>(DT, 2);
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

		Assert.assertEquals(run.getTransition(1), psi1);
		Assert.assertEquals(run.getTransition(2), psi1);
		Assert.assertEquals(run.getTransition(3), psi2);
	}

}
