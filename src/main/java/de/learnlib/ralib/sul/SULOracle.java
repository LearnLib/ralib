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

package de.learnlib.ralib.sul;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SULOracle extends IOOracle {

    private final DataWordSUL sul;
    
    private final ParameterizedSymbol error;

    private static LearnLogger log = LearnLogger.getLogger(SULOracle.class);
    
    public SULOracle(DataWordSUL sul, ParameterizedSymbol error) {
        this.sul = sul;
        this.error = error;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        // FIXME: this has to be checking a mapping is needed after every step!
        Word<PSymbolInstance> act = query;
        log.log(Level.FINEST, "MQ: {0}", query);                    
        sul.pre();
        Word<PSymbolInstance> trace = Word.epsilon();
        for (int i=0; i<query.length(); i+=2) {
            PSymbolInstance in = act.getSymbol(i);
            PSymbolInstance out = sul.step(in);
            
            trace = trace.append(in).append(out);

            if (out.getBaseSymbol().equals(error)) {
                break;
            }
        }
        sul.post();
        return trace;        
    }
    
}
