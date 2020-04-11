/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.theory;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestIneqOutputTreeOpt extends RaLibTestSuite {


    
    @Test
    public void testIneqEqTree() {

        RegisterAutomatonImporter loaderModel = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/pq3.xml");

        RegisterAutomatonImporter loaderHyp = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/pq3_hyp.xml");
        
        RegisterAutomaton model = loaderModel.getRegisterAutomaton();
        RegisterAutomaton hypModel = loaderHyp.getRegisterAutomaton();
        
        ParameterizedSymbol[] inputs = loaderModel.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loaderHyp.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loaderModel.getConstants();
        
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loaderModel.getDataTypes().stream().forEach((t) -> {
            DoubleInequalityTheory theory = new DoubleInequalityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        IOOracle sulOracle = new BasicSULOracle(sul, ERROR);
        IOCacheOracle sulCache = new IOCacheOracle(sulOracle);
        IOFilter sulFilter = new IOFilter(sulCache, inputs);

        DataWordSUL hyp = new SimulatorSUL(hypModel, teachers, consts);
        IOOracle hypOracle = new BasicSULOracle(hyp, ERROR);
        IOCacheOracle hypCache = new IOCacheOracle(hypOracle);
        IOFilter hypFilter = new IOFilter(hypCache, inputs);
        
        ConstraintSolver solver = new SimpleConstraintSolver();
        
        MultiTheoryTreeOracle sulMTO = new MultiTheoryTreeOracle(
                sulFilter, sulOracle, teachers, consts, solver);

        MultiTheoryTreeOracle hypMTO = new MultiTheoryTreeOracle(
                hypFilter, hypOracle, teachers, consts, solver);
        
        DataType doube_t = new DataType("DOUBLE", Double.class);
        
        ParameterizedSymbol offer = new InputSymbol("offer", doube_t);
        ParameterizedSymbol poll = new InputSymbol("poll");
        ParameterizedSymbol out = new OutputSymbol("_out", doube_t);
        ParameterizedSymbol ok = new OutputSymbol("_ok");
        ParameterizedSymbol nok = new OutputSymbol("_nok");
        
        // CE: offer[1.0[DOUBLE]] _ok[] offer[0.5[DOUBLE]] _ok[] offer[1.75[DOUBLE]] <> _ok[] poll[] _out[0.5[DOUBLE]] poll[] _out[1.0[DOUBLE]]
        // CE: offer[1.0[DOUBLE]] _ok[] <> offer[0.5[DOUBLE]] _ok[] offer[1.75[DOUBLE]] <> _ok[] poll[] _out[0.5[DOUBLE]] poll[] _out[1.0[DOUBLE]]
        // CE: offer[1.0[DOUBLE]] _ok[] offer[0.5[DOUBLE]] _ok[] <> offer[1.75[DOUBLE]] <> _ok[] poll[] _out[0.5[DOUBLE]] poll[] _out[1.0[DOUBLE]]
        // binary search: 5, 2, 4
        
        final Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(offer, new DataValue(doube_t, 1.0)),
                new PSymbolInstance(ok),
                new PSymbolInstance(offer, new DataValue(doube_t, 0.5)),
                new PSymbolInstance(ok),
                new PSymbolInstance(offer, new DataValue(doube_t, 1.0)),
                new PSymbolInstance(ok),
                new PSymbolInstance(poll),
                new PSymbolInstance(out, new DataValue(doube_t, 0.5)),
                new PSymbolInstance(poll),
                new PSymbolInstance(out, new DataValue(doube_t, 1.0)));
                

        Word<PSymbolInstance> prefix = ce.prefix(2);
        Word<PSymbolInstance> suffix = ce.suffix(8);
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final GeneralizedSymbolicSuffix symSuffix = 
                new GeneralizedSymbolicSuffix(prefix, suffix,
                        new Constants(), teachers);        
            
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + symSuffix);
        
        TreeQueryResult resSul = sulMTO.treeQuery(prefix, symSuffix);
        TreeQueryResult resHyp = hypMTO.treeQuery(prefix, symSuffix);
        
        System.out.println("SUL SDT\n" + resSul.getSdt());
        System.out.println("HYP SDT\n" + resHyp.getSdt());

        System.out.println("SUL PIV\n" + resSul.getPiv());
        System.out.println("HYP PIV\n" + resHyp.getPiv());
        
        boolean sulHasMoreRegs = !resHyp.getPiv().keySet().containsAll(resSul.getPiv().keySet()); 
        
        System.out.println("More regs: " + sulHasMoreRegs);
    }

}
