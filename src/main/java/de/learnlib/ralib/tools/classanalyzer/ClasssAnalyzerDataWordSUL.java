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

package de.learnlib.ralib.tools.classanalyzer;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;


/**
 *
 * @author falk
 */
public class ClasssAnalyzerDataWordSUL extends DataWordSUL {

    
    private final Class<?> sulClass;
    
    private final Map<ParameterizedSymbol, MethodConfig> methods;
    
    private Object sul = null;
    
    private final int maxDepth;

    private int depth = 0;
    
    public ClasssAnalyzerDataWordSUL(Class<?> sulClass, Map<ParameterizedSymbol, 
            MethodConfig> methods, int d) {
        this.sulClass = sulClass;
        this.methods = methods;
        this.maxDepth = d;
    }
    
    
    @Override
    public void pre() {
        countResets(1);
        depth = 0;
        try {
            sul = sulClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void post() {
        sul = null;
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        
        if (depth > maxDepth && (maxDepth > 0)) {
            return new PSymbolInstance(SpecialSymbols.DEPTH);
        }
        depth++;
        
        MethodConfig in = methods.get(i.getBaseSymbol());
        Method act = in.getMethod();
        
        DataValue[] dvs = i.getParameterValues();
        Object[] params = new Object[dvs.length];
        for (int j = 0; j < dvs.length; j++) {
            params[j] = dvs[j].getId();
        }

        Object ret = null;
        try {
            ret = act.invoke(sul, params);
        } catch (Throwable ex) {
            if (ex instanceof IllegalAccessException || 
                    ex instanceof IllegalArgumentException || 
                    ex instanceof InvocationTargetException) {
                throw new RuntimeException(ex);
            }
            else {
                return new PSymbolInstance(new SpecialSymbols.ErrorSymbol(ex));
            }
        }
        
        if (in.isVoid()) {
            return new PSymbolInstance(SpecialSymbols.VOID);
        }
        
        if (ret == null) {
            return new PSymbolInstance(SpecialSymbols.NULL);
        }
        
        if (in.getRetType().equals(SpecialSymbols.BOOLEAN_TYPE)) {
            return new PSymbolInstance( (Boolean) ret ? SpecialSymbols.TRUE : SpecialSymbols.FALSE );
        }
        
        return new PSymbolInstance(in.getOutput(), new DataValue(in.getRetType(), ret));
        
    }    
    
}
