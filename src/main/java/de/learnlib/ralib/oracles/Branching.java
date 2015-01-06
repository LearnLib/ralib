/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.oracles;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public interface Branching {
    
    public Map<Word<PSymbolInstance>, TransitionGuard> getBranches();

}
