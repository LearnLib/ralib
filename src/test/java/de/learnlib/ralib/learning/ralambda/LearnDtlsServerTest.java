package de.learnlib.ralib.learning.ralambda;

import java.util.LinkedHashMap;
import java.util.Map;

import net.automatalib.serialization.xml.ra.RegisterAutomatonImporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibLearningExperimentRunner;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import net.automatalib.automaton.ra.impl.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaLearningAlgorithmName;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.symbol.ParameterizedSymbol;

public class LearnDtlsServerTest extends RaLibTestSuite {

    @Test
    public void learnDtlsServerTest() {
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/dtls-server.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

//        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
//                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();

        DataType<Integer> epoch = loader.getDataTypes(Integer.class).iterator().next();

        final Map<DataType<?>, Theory<?>> teachers = new LinkedHashMap<>();
        teachers.put(epoch, new IntegerEqualityTheory(epoch));
        SimulatorOracle dwOracle = new SimulatorOracle(model);

        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();

        RaLibLearningExperimentRunner runner = new RaLibLearningExperimentRunner(logger);
        IOEquivalenceTest eqOracle = new IOEquivalenceTest(model, teachers, consts, false, actions);
        runner.setEqOracle(eqOracle);
        runner.setIoMode(true);
        Hypothesis result = runner.run(RaLearningAlgorithmName.RALAMBDA, dwOracle, teachers, consts, jsolv, actions);
        DefaultQuery<PSymbolInstance, Boolean> ce = eqOracle.findCounterExample(result, null);
        Assert.assertNull(ce);
    }

}
