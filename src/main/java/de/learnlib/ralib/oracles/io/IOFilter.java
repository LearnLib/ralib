/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.oracles.io;

import de.learnlib.api.Query;
import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * filters out queries that do not alternate input and output
 * 
 * @author falk
 */
public class IOFilter extends QueryCounter implements DataWordOracle {

    private final Collection<ParameterizedSymbol> inputs; 

    private final DataWordOracle back;
    
    private static LearnLogger log = LearnLogger.getLogger(IOFilter.class);
        
    public IOFilter(DataWordOracle back, ParameterizedSymbol ... inputs) {
        this.inputs = new HashSet<>(Arrays.asList(inputs));
        this.back = back;
    }        
    
    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
        countQueries(clctn.size());
        List<Query<PSymbolInstance, Boolean>> valid = new ArrayList<>();
        for (Query<PSymbolInstance, Boolean> q : clctn) {
            log.log(Level.FINEST, "MQ: {0}", q.getInput());
            if (isValid(q.getInput())) {
                valid.add(q);
            } else {
                q.answer(Boolean.FALSE);
            }
        }
       back.processQueries(valid);
    }    
    
    private boolean isValid(Word<PSymbolInstance> query) {        
        boolean inExp = true;
        for (PSymbolInstance psi : query) {            
            boolean isInput = (this.inputs.contains(psi.getBaseSymbol()));            
            if (inExp ^ isInput) {
                return false;
            }                        
            inExp = !inExp;
        }
               
        return true;
    }
    
}
