package de.learnlib.ralib.learning.ralambda;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class TestOutputSuffixes extends RaLibTestSuite {

	@Test
	public void testSIPOutputSuffixesPresent() {

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

        ConstraintSolver solver = new ConstraintSolver();

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo =
                new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
                new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solver);

//        RaDT radt = new RaDT(mto, hypFactory, mlo, consts, true, actions);
        SLCT slct = new SLCT(mto, hypFactory, mlo, consts, true, solver, actions);
        slct.learn();

        String[] ces = {"IINVITE[1[int]] O100[1[int]] / true",
        		"IINVITE[0[int]] O100[0[int]] IPRACK[0[int]] O200[0[int]] / true"};

        Deque<DefaultQuery<PSymbolInstance, Boolean>> ceQueue = TestUnknownMemorable.buildSIPCEs(ces, actions);

        while (!ceQueue.isEmpty()) {
        	slct.addCounterexample(ceQueue.pop());
        	slct.learn();
        }

        Set<ParameterizedSymbol> outputs = Sets.difference(Set.of(actions), Set.of(inputs));

        String[] wordStr = {"IINVITE[0[int]] O100[0[int]] IPRACK[0[int]] / true"};
        ceQueue = TestUnknownMemorable.buildSIPCEs(wordStr, actions);
        Word<PSymbolInstance> word = ceQueue.getFirst().getInput();

        Set<SymbolicSuffix> suffixes = slct.getCT().getLeaf(word).getPrefix(word).getPath().getSDTs().keySet();
        Set<ParameterizedSymbol> suffixActions = suffixes.stream()
        		                                         .filter(s -> s.length() == 1)
        		                                         .map(s -> s.getActions().firstSymbol())
        		                                         .collect(Collectors.toSet());
        for (ParameterizedSymbol ps : outputs) {
        	Assert.assertTrue(suffixActions.contains(ps));
        }
	}
}
