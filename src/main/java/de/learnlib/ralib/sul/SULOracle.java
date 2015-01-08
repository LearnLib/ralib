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

import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SULOracle extends IOOracle {

    private final DataWordSUL sul;
    
    private final PSymbolInstance error;

    public SULOracle(DataWordSUL sul, PSymbolInstance error) {
        this.sul = sul;
        this.error = error;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        sul.pre();
        Word<PSymbolInstance> trace = Word.epsilon();
        for (PSymbolInstance i : query) {
            PSymbolInstance o = sul.step(i);
            trace = trace.append(o);
            if (o.getBaseSymbol().equals(error)) {
                break;
            }
        }
        sul.post();
        return trace;        
    }
    
}
