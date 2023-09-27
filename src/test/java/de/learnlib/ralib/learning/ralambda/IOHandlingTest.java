package de.learnlib.ralib.learning.ralambda;

import static de.learnlib.ralib.learning.ralambda.IOHandlingTest.IORAExamples.ID;
import static de.learnlib.ralib.learning.ralambda.IOHandlingTest.IORAExamples.IN;
import static de.learnlib.ralib.learning.ralambda.IOHandlingTest.IORAExamples.NOK;
import static de.learnlib.ralib.learning.ralambda.IOHandlingTest.IORAExamples.OK;
import static de.learnlib.ralib.learning.ralambda.IOHandlingTest.IORAExamples.OUT;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class IOHandlingTest extends RaLibTestSuite {
	static class IORAExamples {

		static final DataType ID = new DataType("id", Integer.class);

		static final OutputSymbol NOK = new OutputSymbol("NOK", new DataType[] {});

		static final OutputSymbol OK = new OutputSymbol("OK", new DataType[] {});

		static final InputSymbol IN = new InputSymbol("in", new DataType[] { ID });

		static final OutputSymbol ERROR = new OutputSymbol("ERROR", new DataType[] {});

		static final OutputSymbol OUT = new OutputSymbol("OUT", new DataType[] { ID });

		private IORAExamples() {
		}

		private static RegisterAutomaton buildAutomatonWithNoOutputParams() {
			MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

			// locations
			RALocation l0 = ra.addInitialState(true);
			RALocation l1 = ra.addState(true);
			RALocation l2 = ra.addState(true);
			RALocation l3 = ra.addState(true);
//	        RALocation ls = ra.addState(false);

			// registers and parameters
			RegisterGenerator rgen = new RegisterGenerator();
			Register rVal = rgen.next(ID);
			ParameterGenerator pgen = new ParameterGenerator();
			Parameter pVal = pgen.next(ID);

			// guards
			TransitionGuard okGuard = new TransitionGuard(
					new AtomicGuardExpression<Register, Parameter>(rVal, Relation.EQUALS, pVal));
			TransitionGuard nokGuard = new TransitionGuard(
					new AtomicGuardExpression<Register, Parameter>(rVal, Relation.NOT_EQUALS, pVal));
			TransitionGuard trueGuard = new TransitionGuard();

			// assignments
			VarMapping<Register, SymbolicDataValue> copyMapping = new VarMapping<Register, SymbolicDataValue>();
			copyMapping.put(rVal, rVal);

			VarMapping<Register, SymbolicDataValue> storeMapping = new VarMapping<Register, SymbolicDataValue>();
			storeMapping.put(rVal, pVal);

			Assignment copyAssign = new Assignment(copyMapping);
			Assignment storeAssign = new Assignment(storeMapping);
			OutputMapping outMapping = new OutputMapping();

			// initial location
			ra.addTransition(l0, IN, new InputTransition(trueGuard, IN, l0, l1, storeAssign));
//	        ra.addTransition(l0, OK, new OutputTransition(outMapping, OK, l0, ls, noAssign));
//	        ra.addTransition(l0, NOK, new OutputTransition(outMapping, NOK, l0, ls, noAssign));

			// IN 0 location
//	        ra.addTransition(l1, IN, new InputTransition(trueGuard, IN, l1, ls, noAssign));
			ra.addTransition(l1, OK, new OutputTransition(outMapping, OK, l1, l2, copyAssign));
//	        ra.addTransition(l1, NOK, new OutputTransition(outMapping, NOK, l1, ls, noAssign));

			// IN 0 OK location
			ra.addTransition(l2, IN, new InputTransition(okGuard, IN, l2, l1, copyAssign));
			ra.addTransition(l2, IN, new InputTransition(nokGuard, IN, l2, l3, copyAssign));
//	        ra.addTransition(l2, OK, new OutputTransition(outMapping, OK, l2, ls, noAssign));
//	        ra.addTransition(l2, NOK, new OutputTransition(outMapping, NOK, l2, ls, noAssign));

			// IN 0 OK in 1 location
//	        ra.addTransition(l3, IN, new InputTransition(trueGuard, IN, l3, ls, noAssign));
//	        ra.addTransition(l3, OK, new OutputTransition(outMapping, OK, l3, ls, noAssign));
			ra.addTransition(l3, NOK, new OutputTransition(outMapping, NOK, l3, l2, copyAssign));

			// sink location
//	        ra.addTransition(ls, IN, new InputTransition(trueGuard, IN, ls, ls, noAssign));
//	        ra.addTransition(ls, OK, new OutputTransition(outMapping, OK, ls, ls, noAssign));
//	        ra.addTransition(ls, NOK, new OutputTransition(outMapping, NOK, ls, ls, copyAssign));

			return ra;
		}

		private static RegisterAutomaton buildAutomatonOutputParam(boolean fresh) {
			MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

			// locations
			RALocation l0 = ra.addInitialState(true);
			RALocation l1 = ra.addState(true);
			RALocation l2 = ra.addState(true);
			RALocation l3 = ra.addState(true);

			// registers and parameters
			RegisterGenerator rgen = new RegisterGenerator();
			Register rVal = rgen.next(ID);
			ParameterGenerator pgen = new ParameterGenerator();
			Parameter pVal = pgen.next(ID);

			// guards
			TransitionGuard okGuard = new TransitionGuard(
					new AtomicGuardExpression<Register, Parameter>(rVal, Relation.EQUALS, pVal));
			TransitionGuard nokGuard = new TransitionGuard(
					new AtomicGuardExpression<Register, Parameter>(rVal, Relation.NOT_EQUALS, pVal));
			TransitionGuard trueGuard = new TransitionGuard();

			// assignments
			VarMapping<Register, SymbolicDataValue> copyMapping = new VarMapping<Register, SymbolicDataValue>();
			copyMapping.put(rVal, rVal);

			VarMapping<Register, SymbolicDataValue> storeMapping = new VarMapping<Register, SymbolicDataValue>();
			storeMapping.put(rVal, pVal);

			Assignment copyAssign = new Assignment(copyMapping);
			Assignment storeAssign = new Assignment(storeMapping);
			OutputMapping nokOutputMapping = new OutputMapping();
			OutputMapping outOutputMapping = null;
			if (fresh) {
				outOutputMapping = new OutputMapping(pVal);
			} else {
				outOutputMapping = new OutputMapping(pVal, rVal);
			}

			// initial location
			ra.addTransition(l0, IN, new InputTransition(trueGuard, IN, l0, l1, storeAssign));

			// IN 0 location
			ra.addTransition(l1, OUT, new OutputTransition(outOutputMapping, OUT, l1, l2, copyAssign));

			// IN 0 OUT fresh/0 location
			ra.addTransition(l2, IN, new InputTransition(okGuard, IN, l2, l1, copyAssign));
			ra.addTransition(l2, IN, new InputTransition(nokGuard, IN, l2, l3, copyAssign));

			// IN 0 OUT fresh/0 in 1 location
//		        ra.addTransition(l3, IN, new InputTransition(trueGuard, IN, l3, ls, noAssign));
//		        ra.addTransition(l3, OK, new OutputTransition(outMapping, OK, l3, ls, noAssign));
			ra.addTransition(l3, NOK, new OutputTransition(nokOutputMapping, NOK, l3, l2, copyAssign));

			return ra;
		}
	}

	@Test
	public void testLearnIORAWithNoOutputParams() {

		Constants consts = new Constants();
		RegisterAutomaton model = IORAExamples.buildAutomatonWithNoOutputParams();

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(ID, new IntegerEqualityTheory(ID));

		DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
		IOOracle ioOracle = new SULOracle(sul, IORAExamples.ERROR);
		IOCache ioCache = new IOCache(ioOracle);
		IOFilter ioFilter = new IOFilter(ioCache, IN);

		teachers.values().stream().forEach((t) -> {
			((EqualityTheory) t).setFreshValues(true, ioCache);
		});

		ConstraintSolver solver = new SimpleConstraintSolver();

		MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts, solver);
		MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp),
				teachers, consts, solver);

		RaLambda ralambda = new RaLambda(mto, hypFactory, mlo, consts, true, IN, OK, NOK);

		ralambda.learn();

		RegisterAutomaton hyp = ralambda.getHypothesis();
		logger.log(Level.FINE, "HYP1: {0}", hyp);

		Word<PSymbolInstance> ce = Word.fromSymbols(new PSymbolInstance(IN, new DataValue(ID, 0)),
				new PSymbolInstance(OK), new PSymbolInstance(IN, new DataValue(ID, 1)), new PSymbolInstance(NOK));

		ralambda.addCounterexample(new DefaultQuery<>(ce, model.accepts(ce)));

		ralambda.learn();
		hyp = ralambda.getHypothesis();
		logger.log(Level.FINE, "HYP2: {0}", hyp);
		Assert.assertTrue(hyp.accepts(ce));
		IOEquivalenceTest test = new IOEquivalenceTest(model, teachers, consts, true, IN, OK, NOK);
		DefaultQuery<PSymbolInstance, Boolean> ceQuery = test.findCounterExample(hyp, null);
		Assert.assertNull(ceQuery);
	}

	@Test
	public void testLearnIORAWithEqualOutputParam() {

		Constants consts = new Constants();
		RegisterAutomaton model = IORAExamples.buildAutomatonOutputParam(false);

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(ID, new IntegerEqualityTheory(ID));

		DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
		IOOracle ioOracle = new SULOracle(sul, IORAExamples.ERROR);
		IOCache ioCache = new IOCache(ioOracle);
		IOFilter ioFilter = new IOFilter(ioCache, IN);

		teachers.values().stream().forEach((t) -> {
			((EqualityTheory) t).setFreshValues(true, ioCache);
		});

		ConstraintSolver solver = new SimpleConstraintSolver();

		MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts, solver);
		MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp),
				teachers, consts, solver);

		RaLambda ralambda = new RaLambda(mto, hypFactory, mlo, consts, true, IN, NOK, OUT);

		ralambda.learn();

		RegisterAutomaton hyp = ralambda.getHypothesis();
		logger.log(Level.FINE, "HYP1: {0}", hyp);

//		Word<PSymbolInstance> ce = Word.fromSymbols(new PSymbolInstance(IN, new DataValue(ID, 0)),
//				new PSymbolInstance(OUT, new DataValue(ID, 0)), new PSymbolInstance(IN, new DataValue(ID, 1)),
//				new PSymbolInstance(NOK));
		Word<PSymbolInstance> ce = Word.fromSymbols(new PSymbolInstance(IN, new DataValue(ID, 0)),
				new PSymbolInstance(OUT, new DataValue(ID, 0)), new PSymbolInstance(IN, new DataValue(ID, 1)),
				new PSymbolInstance(NOK));

		ralambda.addCounterexample(new DefaultQuery<>(ce, model.accepts(ce)));

		ralambda.learn();
		hyp = ralambda.getHypothesis();
		logger.log(Level.FINE, "HYP2: {0}", hyp);
		Assert.assertTrue(hyp.accepts(ce));
		IOEquivalenceTest test = new IOEquivalenceTest(model, teachers, consts, true, IN, NOK, OUT);
		DefaultQuery<PSymbolInstance, Boolean> ceQuery = test.findCounterExample(hyp, null);
		Assert.assertNull(ceQuery);
	}

	@Test
	public void testLearnIORAWithFreshOutputParam() {

		Constants consts = new Constants();
		RegisterAutomaton model = IORAExamples.buildAutomatonOutputParam(true);

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		teachers.put(ID, new IntegerEqualityTheory(ID));

		DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
		IOOracle ioOracle = new SULOracle(sul, IORAExamples.ERROR);
		IOCache ioCache = new IOCache(ioOracle);
		IOFilter ioFilter = new IOFilter(ioCache, IN);

		teachers.values().stream().forEach((t) -> {
			((EqualityTheory) t).setFreshValues(true, ioCache);
		});

		ConstraintSolver solver = new SimpleConstraintSolver();

		MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts, solver);
		MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp),
				teachers, consts, solver);

		RaLambda ralambda = new RaLambda(mto, hypFactory, mlo, consts, true, IN, NOK, OUT);

		ralambda.learn();

		RegisterAutomaton hyp = ralambda.getHypothesis();
		logger.log(Level.FINE, "HYP1: {0}", hyp);

		Word<PSymbolInstance> ce = Word.fromSymbols(new PSymbolInstance(IN, new DataValue(ID, 0)),
				new PSymbolInstance(OUT, new DataValue(ID, 1)), new PSymbolInstance(IN, new DataValue(ID, 1)),
				new PSymbolInstance(NOK));

		ralambda.addCounterexample(new DefaultQuery<>(ce, model.accepts(ce)));

		ralambda.learn();
		hyp = ralambda.getHypothesis();
		logger.log(Level.FINE, "HYP2: {0}", hyp);
		Assert.assertTrue(hyp.accepts(ce));
		IOEquivalenceTest test = new IOEquivalenceTest(model, teachers, consts, true, IN, NOK, OUT);
		DefaultQuery<PSymbolInstance, Boolean> ceQuery = test.findCounterExample(hyp, null);
		Assert.assertNull(ceQuery);
	}
}
