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
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final Map<DataValue, Set<DataValue>> replacements = new HashMap<>();
  
    public SULOracle(DataWordSUL sul, ParameterizedSymbol error) {
        this.sul = sul;
        this.error = error;
    }

    @Override
    public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
        countQueries(1);
        Word<PSymbolInstance> act = query;
        log.log(Level.FINEST, "MQ: {0}", query);
        sul.pre();
        replacements.clear();
        Word<PSymbolInstance> trace = Word.epsilon();
        for (int i = 0; i < query.length(); i += 2) {
            PSymbolInstance in = applyReplacements(act.getSymbol(i));
            
            PSymbolInstance out = sul.step(in);
            updateReplacements(act.getSymbol(i + 1), out);

            trace = trace.append(in).append(out);

            if (out.getBaseSymbol().equals(error)) {
                break;
            }
        }
        sul.post();
        return trace;
    }

    private PSymbolInstance applyReplacements(PSymbolInstance symbol) {
        DataValue[] vals = new DataValue[symbol.getBaseSymbol().getArity()];
        for (int i = 0; i < symbol.getBaseSymbol().getArity(); i++) {
            Set<DataValue> set = getOrCreate(symbol.getParameterValues()[i]);
            if (set.size() < 1) {
                vals[i] = symbol.getParameterValues()[i];
            } else {
                vals[i] = set.iterator().next();
            }
        }
              
        return new PSymbolInstance(symbol.getBaseSymbol(), vals);
    }

    private void updateReplacements(
            PSymbolInstance outTest, PSymbolInstance outSys) {

        for (int i = 0; i < outSys.getBaseSymbol().getArity(); i++) {
            Set<DataValue> set = getOrCreate(outSys.getParameterValues()[i]);
            set.add(outSys.getParameterValues()[i]);
            assert set.size() <= 1;
        }
    }

    private Set<DataValue> getOrCreate(DataValue key) {
        Set<DataValue> ret = replacements.get(key);
        if (ret == null) {
            ret = new HashSet<>();
            replacements.put(key, ret);
        }
        return ret;
    }

}
