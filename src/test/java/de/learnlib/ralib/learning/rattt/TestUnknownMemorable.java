package de.learnlib.ralib.learning.rattt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
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
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class TestUnknownMemorable extends RaLibTestSuite {

	private static final DataType T_INT = new DataType("int", Integer.class);

	private static final InputSymbol IPUT =
			new InputSymbol("put", new DataType[] {T_INT});
	private static final InputSymbol IQUERY =
			new InputSymbol("query", new DataType[] {});
	private static final InputSymbol IHELLO =
			new InputSymbol("hello", new DataType[] {});

	private static final OutputSymbol OECHO =
			new OutputSymbol("echo", new DataType[] {T_INT});
	private static final OutputSymbol OYES =
			new OutputSymbol("yes", new DataType[] {T_INT});
	private static final OutputSymbol ONO =
			new OutputSymbol("no", new DataType[] {T_INT});
	private static final OutputSymbol OHELLO =
			new OutputSymbol("hello", new DataType[] {});
	private static final OutputSymbol ONOREPLY =
			new OutputSymbol("noreply", new DataType[] {});
	private static final OutputSymbol ONOK =
			new OutputSymbol("nok", new DataType[] {});

	private RegisterAutomaton buildAutomaton() {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();

		// locations
		RALocation l0 = ra.addInitialState();
		RALocation l0_nok = ra.addState();
		RALocation l0_put = ra.addState();
		RALocation l1 = ra.addState();
		RALocation l1_query = ra.addState();
		RALocation l1_hello = ra.addState();
		RALocation l1_put = ra.addState();
		RALocation l2 = ra.addState();
		RALocation l2_query = ra.addState();
		RALocation l2_hello = ra.addState();
		RALocation l3 = ra.addState();
		RALocation l3_hello = ra.addState();
		RALocation l3_nok = ra.addState();
		RALocation l4 = ra.addState();
		RALocation l4_hello = ra.addState();
		RALocation l4_nok = ra.addState();

		// registers and parameters
		SymbolicDataValueGenerator.RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
		SymbolicDataValue.Register r1 = rgen.next(T_INT);
		SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
		SymbolicDataValue.Parameter p1 = pgen.next(T_INT);

		// guards
		GuardExpression equal = new AtomicGuardExpression(r1, Relation.EQUALS, p1);
		GuardExpression notEqual = new AtomicGuardExpression(r1, Relation.NOT_EQUALS, p1);

		TransitionGuard equalGuard = new TransitionGuard(equal);
		TransitionGuard notEqualGuard = new TransitionGuard(notEqual);
		TransitionGuard trueGuard = new TransitionGuard();

		// assignments
		VarMapping<SymbolicDataValue.Register, SymbolicDataValue> store = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
        store.put(r1, p1);
        VarMapping<Register, SymbolicDataValue> copy = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();
        copy.put(r1, r1);
        VarMapping<Register, SymbolicDataValue> noMapping = new VarMapping<SymbolicDataValue.Register, SymbolicDataValue>();

        Assignment storeAssign = new Assignment(store);
        Assignment copyAssign = new Assignment(copy);
        Assignment noAssign = new Assignment(noMapping);

        OutputMapping omEqual = new OutputMapping(p1, r1);
        OutputMapping omTrue = new OutputMapping();

        // initial location
        ra.addTransition(l0, IPUT, new InputTransition(trueGuard, IPUT, l0, l0_put, storeAssign));
        ra.addTransition(l0, IHELLO, new InputTransition(trueGuard, IHELLO, l0, l0_nok, noAssign));
        ra.addTransition(l0, IQUERY, new InputTransition(trueGuard, IQUERY, l0, l0_nok, noAssign));

        ra.addTransition(l0_put, OECHO, new OutputTransition(omEqual, OECHO, l0_put, l1, copyAssign));
        ra.addTransition(l0_nok, ONOK, new OutputTransition(omTrue, ONOK, l0_nok, l0, noAssign));

        // put same value
        ra.addTransition(l1, IPUT, new InputTransition(equalGuard, IPUT, l1, l0_put, copyAssign));
        ra.addTransition(l1, IPUT, new InputTransition(notEqualGuard, IPUT, l1, l1_put, storeAssign));
        ra.addTransition(l1, IQUERY, new InputTransition(trueGuard, IQUERY, l1, l1_query, copyAssign));
        ra.addTransition(l1, IHELLO, new InputTransition(trueGuard, IHELLO, l1, l1_hello, copyAssign));

        ra.addTransition(l1_put, OECHO, new OutputTransition(omEqual, OECHO, l1_put, l2, copyAssign));
        ra.addTransition(l1_query, ONO, new OutputTransition(omEqual, ONO, l1_query, l1, copyAssign));
        ra.addTransition(l1_hello, OHELLO, new OutputTransition(omTrue, OHELLO, l1_hello, l3, copyAssign));

        // put different value
        ra.addTransition(l2, IPUT, new InputTransition(trueGuard, IPUT, l2, l1_put, storeAssign));
        ra.addTransition(l2, IQUERY, new InputTransition(trueGuard, IQUERY, l2, l2_query, copyAssign));
        ra.addTransition(l2, IHELLO, new InputTransition(trueGuard, IHELLO, l2, l2_hello, copyAssign));

        ra.addTransition(l2_query, OYES, new OutputTransition(omEqual, OYES, l2_query, l2, copyAssign));
        ra.addTransition(l2_hello, OHELLO, new OutputTransition(omTrue, OHELLO, l2_hello, l4, copyAssign));

        // hello, same value
        ra.addTransition(l3, IPUT, new InputTransition(equalGuard, IPUT, l3, l0_put, copyAssign));
        ra.addTransition(l3, IPUT, new InputTransition(notEqualGuard, IPUT, l3, l1_put, storeAssign));
        ra.addTransition(l3, IHELLO, new InputTransition(trueGuard, IHELLO, l3, l3_hello, copyAssign));
        ra.addTransition(l3, IQUERY, new InputTransition(trueGuard, IQUERY, l3, l3_nok, copyAssign));

        ra.addTransition(l3_hello, ONOREPLY, new OutputTransition(omTrue, ONOREPLY, l3_hello, l3, copyAssign));
        ra.addTransition(l3_nok, ONOK, new OutputTransition(omTrue, ONOK, l3_nok, l3, copyAssign));

        // hello, different value
        ra.addTransition(l4, IPUT, new InputTransition(trueGuard, IPUT, l4, l1_put, storeAssign));
        ra.addTransition(l4, IHELLO, new InputTransition(trueGuard, IHELLO, l4, l4_hello, noAssign));
        ra.addTransition(l4, IQUERY, new InputTransition(trueGuard, IQUERY, l4, l4_nok, noAssign));

        ra.addTransition(l4_hello, ONOREPLY, new OutputTransition(omTrue, ONOREPLY, l4_hello, l4, noAssign));
        ra.addTransition(l4_nok, ONOK, new OutputTransition(omTrue, ONOK, l4_nok, l4, noAssign));

        return ra;
	}

	@Test
	public void testUnknownMemorable() {
		// this learning experiment tests an edge case where two suffixes can both be used to resolve
		// a lost memorable variable, but only one of them will allow learning to continue

		RegisterAutomaton ra = buildAutomaton();

		ParameterizedSymbol[] inputSymbols = { IPUT, IQUERY, IHELLO };
		ParameterizedSymbol[] actions = { IPUT, IQUERY, IHELLO, OECHO, ONO, OYES, OHELLO, ONOREPLY };
		Constants consts = new Constants();

	    final Map<DataType, Theory> teachers = new LinkedHashMap<>();
	    teachers.put(T_INT, new IntegerEqualityTheory(T_INT));

	    ConstraintSolver solver = new SimpleConstraintSolver();

	    DataWordSUL sul = new SimulatorSUL(ra, teachers, consts);
        final ParameterizedSymbol ERROR = new OutputSymbol("_io_err", new DataType[]{});

        IOOracle ioOracle = new SULOracle(sul, ERROR);
	    IOCache ioCache = new IOCache(ioOracle);
	    IOFilter oracle = new IOFilter(ioCache, inputSymbols);

	    MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(oracle, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);
        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaTTT rattt = new RaTTT(mto, hypFactory, mlo, consts, true, actions);

        rattt.learn();

        Word<PSymbolInstance> ce = Word.fromSymbols(
        		new PSymbolInstance(IPUT, new DataValue(T_INT, 0)),
        		new PSymbolInstance(OECHO, new DataValue(T_INT, 0)),
        		new PSymbolInstance(IPUT, new DataValue(T_INT, 1)),
        		new PSymbolInstance(OECHO, new DataValue(T_INT, 1)),
        		new PSymbolInstance(IQUERY),
        		new PSymbolInstance(OYES, new DataValue(T_INT, 1)));
        rattt.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, true));

        rattt.learn();

        ce = Word.fromSymbols(
        		new PSymbolInstance(IPUT, new DataValue(T_INT, 0)),
        		new PSymbolInstance(OECHO, new DataValue(T_INT, 0)),
        		new PSymbolInstance(IHELLO),
        		new PSymbolInstance(OHELLO),
        		new PSymbolInstance(IPUT, new DataValue(T_INT, 0)),
        		new PSymbolInstance(OECHO, new DataValue(T_INT, 0)),
        		new PSymbolInstance(IQUERY),
        		new PSymbolInstance(ONO, new DataValue(T_INT, 0)));
        boolean acc = rattt.getHypothesis().accepts(ce);
        if (!acc) {
        	rattt.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, true));
        	rattt.learn();
        }

        // if learning reaches this point without assertion violations, the test is passed
		Assert.assertTrue(true);
	}

	@Test
	public void testSkippingMemorable() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();


        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        ConstraintSolver solver = new SimpleConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo =
                new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

        RaTTT rattt = new RaTTT(mto, hypFactory, mlo, consts, true, actions);
        rattt.learn();

        String[] ces = {"IPRACK[0[int]] Otimeout[] IINVITE[1[int]] Otimeout[] / true",
        		        "Inil[] Otimeout[] IINVITE[0[int]] O100[0[int]] / true",
        		        "IINVITE[0[int]] O100[0[int]] IPRACK[0[int]] O200[0[int]] / true",
        		        "IINVITE[0[int]] O100[0[int]] IACK[2[int]] Otimeout[] IPRACK[0[int]] O200[0[int]] / true",
        		        "IINVITE[0[int]] O100[0[int]] Inil[] O183[0[int]] IINVITE[3[int]] O100[3[int]] Inil[] O486[3[int]] / true",
        		        "IINVITE[0[int]] O100[0[int]] IPRACK[0[int]] O200[0[int]] Inil[] O180[0[int]] IINVITE[0[int]] O100[0[int]] Inil[] O486[0[int]] / true"};

        Deque<DefaultQuery<PSymbolInstance, Boolean>> ceQueue = buildSIPCEs(ces, actions);

        while (!ceQueue.isEmpty()) {
        	rattt.addCounterexample(ceQueue.pop());
        	rattt.learn();
        }

        Assert.assertTrue(true);
	}

	private Deque<DefaultQuery<PSymbolInstance, Boolean>> buildSIPCEs(String[] ceStrings, ParameterizedSymbol[] actionSymbols) {
		Deque<DefaultQuery<PSymbolInstance, Boolean>> ces = new LinkedList<>();

		for (String ceString : ceStrings) {
			Pattern dvPattern = Pattern.compile("\\[(.?)\\[int\\]\\]");
			Matcher m = dvPattern.matcher(ceString);
			Collection<Integer> params = new ArrayList<Integer>();
			while (m.find()) {
				String s = m.group(1);
				params.add(Integer.parseInt(m.group(1)));
			}

			Pattern psPattern = Pattern.compile("(IACK|Inil|IPRACK|IINVITE|Otimeout|O486|O481|O200|O180|O183|O100)");
			m = psPattern.matcher(ceString);
			Collection<String> as = new ArrayList<String>();
			while (m.find()) {
				as.add(m.group());
			}

			Pattern outPattern = Pattern.compile("(true|false)");
			m = outPattern.matcher(ceString);
			m.find();
			boolean outcome = Boolean.parseBoolean(m.group());

			String[] actions = as.toArray(new String[as.size()]);
			int[] dvs = new int[as.size()];
			Iterator<Integer> paramIt = params.iterator();
			for (int i = 0; i < as.size(); i++) {
				if (!actions[i].equals("Otimeout") && !actions[i].equals("Inil")) {
					dvs[i] = paramIt.next();
				}
			}

			ces.add(buildSIPCounterExample(actions, dvs, outcome, actionSymbols));
		}

		return ces;
	}

	private DefaultQuery<PSymbolInstance, Boolean> buildSIPCounterExample(String[] actions, int[] dv, boolean outcome, ParameterizedSymbol[] actionSymbols) {
		Word<PSymbolInstance> ce = Word.epsilon();
		for (int i = 0; i < actions.length; i++) {
			String action = actions[i];
			int idx = findMatchingSymbol(action, actionSymbols);
			if (idx < 0)
				return null;
			DataType[] dt = actionSymbols[idx].getPtypes();
			PSymbolInstance psi = dt.length > 0 ?
				new PSymbolInstance(actionSymbols[idx], new DataValue(dt[0], dv[i])) :
				new PSymbolInstance(actionSymbols[idx]);
			ce = ce.append(psi);
		}
		return new DefaultQuery<PSymbolInstance, Boolean>(ce, outcome);
	}

	private int findMatchingSymbol(String action, ParameterizedSymbol[] actionSymbols) {
		for (int i = 0; i < actionSymbols.length; i++ ) {
			if (actionSymbols[i].getName().contains(action))
				return i;
		}
		return -1;
	}
}
