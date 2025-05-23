/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class ClassAnalyzerTest extends RaLibTestSuite {

    @Test
    public void testClassAnalyzerWithMultilogin() {
        final String[] options = new String[] {
            "class-analyzer",
            "target=de.learnlib.ralib.example.login.FreshMultiLogin;" +
            "methods=" +
            "IRegister(java.math.BigDecimal:uid)java.math.BigDecimal:pwd+" +
            "ILogin(java.math.BigDecimal:uid,java.math.BigDecimal:pwd)boolean:boolean;" +
            //"ILogout(java.lang.Integer:int)boolean:boolean+" +
            //"IChangePassword(java.lang.Integer:int,java.lang.Integer:int)boolean:boolean;" +
            "random.seed=6521023071547789;" +
            "logging.level=WARNING;" +
            "max.time.millis=600000;" +
            "use.ceopt=false;" +
            "use.suffixopt=true;" +
            "use.fresh=true;" +
            "use.rwalk=true;" +
            "export.model=false;" +
            "rwalk.prob.fresh=0.3;" +
            "rwalk.prob.reset=0.1;" +
            "rwalk.max.depth=6;" +
            "rwalk.max.runs=10000;" +
            "rwalk.reset.count=false;" +
            "rwalk.draw.uniform=false;" +
            "teachers=uid:de.learnlib.ralib.tools.theories.IntegerEqualityTheory+" +
	    "pwd:de.learnlib.ralib.tools.theories.IntegerEqualityTheory;"};

        try {
            ConsoleClient cl = new ConsoleClient(options);
            int ret = cl.run();
            Assert.assertEquals(ret, 0);
        } catch (Throwable t) {
            t.printStackTrace();
            Assert.fail(t.getClass().getName());
        }
    }

    @Test
    public void testClassAnalyzerWithFreshValues() {
        final String[] options = new String[] {
            "class-analyzer",
            "target=de.learnlib.ralib.example.keygen.KeyGenMap;" +
            "methods=put(java.math.BigDecimal:int)java.math.BigDecimal:int+"
                + "get(java.math.BigDecimal:int)java.math.BigDecimal:int;" +
            "random.seed=652102309071547789;" +
            "logging.level=WARNING;" +
            "max.time.millis=600000;" +
            "use.ceopt=false;" +
            "use.suffixopt=true;" +
            "use.fresh=true;" +
            "use.rwalk=true;" +
            "export.model=false;" +
            "rwalk.prob.fresh=0.8;" +
            "rwalk.prob.reset=0.1;" +
            "rwalk.max.depth=6;" +
            "rwalk.max.runs=1000;" +
            "rwalk.reset.count=false;" +
            "rwalk.draw.uniform=false;" +
            "teachers=int:de.learnlib.ralib.tools.theories.IntegerEqualityTheory;"};

        try {
            ConsoleClient cl = new ConsoleClient(options);
            int ret = cl.run();
            Assert.assertEquals(ret, 0);

            RegisterAutomaton hyp = ((ClassAnalyzer) cl.getTool()).getHypothesis();

            RegisterAutomatonImporter loader = TestUtil.getLoader(
                    "/de/learnlib/ralib/automata/xml/classanalyzer1.xml");

            ParameterizedSymbol[] actions = loader.getActions().toArray(
                    new ParameterizedSymbol[]{});

            final Map<DataType, Theory> teachers = new LinkedHashMap<>();
            loader.getDataTypes().stream().forEach((t) -> {
                IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
                theory.setUseSuffixOpt(true);
                teachers.put(t, theory);
            });

            IOEquivalenceTest check = new IOEquivalenceTest(
                    loader.getRegisterAutomaton(), teachers, loader.getConstants(), true, actions);

            DefaultQuery<PSymbolInstance, Boolean> ce = check.findCounterExample(hyp, null);

            Assert.assertTrue(ce.toString().contains("__ERR"));
            Assert.assertEquals(hyp.getInputStates().size(), 3);
            Assert.assertEquals(hyp.getInputTransitions().size(), 7);

        } catch (Throwable t) {
            t.printStackTrace();
            Assert.fail(t.getClass().getName());
        }
    }

    @Test
    public void testClassAnalyzerInequalities() {
        final String[] options = new String[] {
            "class-analyzer",
            "target=java.util.PriorityQueue;" +
            "methods=offer(java.lang.Object:double)boolean:boolean+poll()java.lang.Object:double;" +
            "random.seed=3364703215613917377;" +
            "logging.level=WARNING;" +
            "max.time.millis=600000;" +
            "use.ceopt=true;" +
            "use.suffixopt=true;" +
            "use.fresh=false;" +
            "use.rwalk=true;" +
            "export.model=false;" +
            "rwalk.prob.fresh=0.1;" +
            "rwalk.prob.reset=0.0;" +
            "rwalk.max.depth=6;" +
            "rwalk.max.runs=100;" +
            "rwalk.reset.count=false;" +
            "rwalk.draw.uniform=false;" +
            "rwalk.seed.transitions=true;" +
            "teachers=double:de.learnlib.ralib.tools.theories.DoubleInequalityTheory"};

        try {
            ConsoleClient cl = new ConsoleClient(options);
            int ret = cl.run();
            Assert.assertEquals(ret, 0);

            RegisterAutomaton hyp = ((ClassAnalyzer) cl.getTool()).getHypothesis();

            RegisterAutomatonImporter loader = TestUtil.getLoader(
                    "/de/learnlib/ralib/automata/xml/classanalyzer2.xml");

            ParameterizedSymbol[] actions = loader.getActions().toArray(
                    new ParameterizedSymbol[]{});

            final Map<DataType, Theory> teachers = new LinkedHashMap<>();
            loader.getDataTypes().stream().forEach((t) -> {
                DoubleInequalityTheory theory = new DoubleInequalityTheory(t);
                theory.setUseSuffixOpt(true);
                teachers.put(t, theory);
            });

            IOEquivalenceTest check = new IOEquivalenceTest(
                    loader.getRegisterAutomaton(), teachers, loader.getConstants(), true, actions);

            // System.out.println(check.findCounterExample(hyp, null) );
            boolean equiv = check.findCounterExample(hyp, null) == null;
            Assert.assertTrue(equiv);

        } catch (Throwable t) {
            t.printStackTrace();
            Assert.fail(t.getClass().getName());
        }
    }

    @Test
    public void testClassAnalyzerConstants() {
    	final String[] options = new String[] {
                "class-analyzer",
                "target=de.learnlib.ralib.example.container.ContainerSUL;" +
                "methods=put(java.math.BigDecimal:int)void+" +
                        "get()java.math.BigDecimal:int;" +
                "random.seed=652102309071547789;" +
                "logging.level=WARNING;" +
                "max.time.millis=600000;" +
                "learner=sllambda;" +
                "use.ceopt=false;" +
                "use.suffixopt=true;" +
                "use.fresh=true;" +
                "use.rwalk=true;" +
                "export.model=false;" +
                "rwalk.prob.fresh=0.8;" +
                "rwalk.prob.reset=0.1;" +
                "rwalk.max.depth=6;" +
                "rwalk.max.runs=1000;" +
                "rwalk.reset.count=false;" +
                "rwalk.draw.uniform=false;" +
                "teachers=int:de.learnlib.ralib.tools.theories.IntegerEqualityTheory;" +
                "constants=[{'type':'int','value':'0'}]"};

    	try {
            ConsoleClient cl = new ConsoleClient(options);
            int ret = cl.run();
            Assert.assertEquals(ret, 0);

            RegisterAutomaton hyp = ((ClassAnalyzer) cl.getTool()).getHypothesis();

            RegisterAutomatonImporter loader = TestUtil.getLoader(
                    "/de/learnlib/ralib/automata/xml/classanalyzer3.xml");

            ParameterizedSymbol[] actions = loader.getActions().toArray(
                    new ParameterizedSymbol[]{});

            final Map<DataType, Theory> teachers = new LinkedHashMap<>();
            loader.getDataTypes().stream().forEach((t) -> {
                IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
                theory.setUseSuffixOpt(true);
                teachers.put(t, theory);
            });

            IOEquivalenceTest check = new IOEquivalenceTest(
                    loader.getRegisterAutomaton(), teachers, loader.getConstants(), true, actions);

            boolean equiv = check.findCounterExample(hyp, null) == null;
            Assert.assertTrue(equiv);

    	} catch (Throwable t) {
            Assert.fail(t.getClass().getName());
    	}
    }

}
