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

package de.learnlib.ralib.sul;

import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.api.Query;
import de.learnlib.drivers.reflect.AbstractMethodInput;
import de.learnlib.drivers.reflect.AbstractMethodOutput;
import de.learnlib.drivers.reflect.Error;
import de.learnlib.drivers.reflect.SimplePOJOTestDriver;
import de.learnlib.oracles.SULOracle;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class DataWordSULConnector implements DataWordOracle {
    
    final SimplePOJOTestDriver driver;    
    
    final Map<ParameterizedSymbol, Method> inputs;
    
    final SULOracle<AbstractMethodInput, AbstractMethodOutput> oracle;
    
    public DataWordSULConnector(SimplePOJOTestDriver driver, Map<ParameterizedSymbol, Method> map) {
        this.driver = driver;
        this.inputs = map;
        this.oracle = new SULOracle<>(driver);
    }

    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
    
        for (Query<PSymbolInstance, Boolean> c : clctn) {
            Word<AbstractMethodInput> prefix = translate(c.getPrefix());
            Word<AbstractMethodInput> suffix = translate(c.getSuffix());
            
            Word<AbstractMethodOutput> trace = this.oracle.answerQuery(prefix, suffix);
            
            c.answer(!(trace.lastSymbol() instanceof Error));
        }    
    }
    
    private Word<AbstractMethodInput> translate(Word<PSymbolInstance> query) {
        
        Word<AbstractMethodInput> trans = Word.epsilon();
        for (PSymbolInstance psi : query) {
            AbstractMethodInput a = translate(psi);
            trans.append(a);
        }
        
        return trans;
    }
    
    private AbstractMethodInput translate(PSymbolInstance psi) {
        ParameterizedSymbol p = psi.getBaseSymbol();
        Method m = this.inputs.get(p);
        Object[] params = new Object[p.getArity()];
        
        int idx = 0;
        for (DataValue dv : psi.getParameterValues()) {
            params[idx++] = dv.getId();
        }
        
        AbstractMethodInput ret = new AbstractMethodInput(
                p.getName(), m, new HashMap<String, Integer>(), params);
        
        return ret;
    }
    
}
