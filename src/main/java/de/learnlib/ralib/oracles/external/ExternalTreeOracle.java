/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class ExternalTreeOracle extends MultiTheoryTreeOracle {
    
    public ExternalTreeOracle(Map<DataType, Theory> teachers, Constants constants, ConstraintSolver solver) {
        super(null, teachers, constants, solver);
    }
    
    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values, PIV pir,
            Constants constants,
            SuffixValuation suffixValues) {
        
        System.out.println("prefix = " + prefix);
        System.out.println("suffix = " + suffix);
        System.out.println("values = " + values);
        System.out.println("pir = " + pir);
        System.out.println("constants = " + constants);
        System.out.println("suffixValues = " + suffixValues);
        
        throw new UnsupportedOperationException("Not implemented yet.");
    }    
}
