/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.equivalence;

import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class IOCounterExampleRelationRemoverTest {
        
    @Test
    public void testRelationRemover() {
        
      
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();

        
        
        DataType type = loader.getDataTypes().iterator().next();
        Theory theory = new IntegerEqualityTheory(type);
        
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            IntegerEqualityTheory _theory = new IntegerEqualityTheory(t);
            _theory.setUseSuffixOpt(true);
            teachers.put(t, _theory);
        });
        
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
        
//        IOCounterExampleRelationRemover remover = 
//                new IOCounterExampleRelationRemover(
//                        teachers, consts, jsolv);
//        
//        ParameterizedSymbol a = new InputSymbol("a", type);
//        DataValue dv1 = theory.getFreshValue(new ArrayList());
//        DataValue dv2 = theory.getFreshValue(Collections.singletonList(dv1));
//        
//        Word<PSymbolInstance> ce = Word.fromSymbols(
//                new PSymbolInstance(a, dv1),
//                new PSymbolInstance(a, dv1),
//                new PSymbolInstance(a, dv1),
//                new PSymbolInstance(a, dv1),
//                new PSymbolInstance(a, dv2)
//        );
//        
//        remover.optimizeCE(ce, null);
        
    } 
}
