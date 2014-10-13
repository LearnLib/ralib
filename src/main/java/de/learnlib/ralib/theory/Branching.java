/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.ralib.trees.SDTLeaf;
import de.learnlib.ralib.trees.SymbolicDecisionTree;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class Branching {
    
    private Word<PSymbolInstance> prefix;
    
    private ParameterizedSymbol act;
            
    private Map<Guard, Word<PSymbolInstance>> branches;

    
    
}
