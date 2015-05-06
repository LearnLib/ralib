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

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import java.lang.reflect.Method;
import java.util.Map;
import net.automatalib.commons.util.Pair;

/**
 *
 * @author falk
 */
public class MethodConfig {
    
    private final Method method;
    
    private final InputSymbol input;
    
    private final OutputSymbol output;
    
    private final boolean isVoid;
       
    private final DataType retType;
    
    public MethodConfig(String config, Class<?> clazz, Map<String, DataType> types) 
            throws ClassNotFoundException, NoSuchMethodException {
        
        String methodName = config.substring(0, config.indexOf("(")).trim();
        String paramString = config.substring(config.indexOf("(") +1, config.indexOf(")")).trim();        
        String[] paramConfig = (paramString.length() < 1) ? 
                new String[0] : config.substring(config.indexOf("(") +1, config.indexOf(")")).trim().split(",");
        String returnConfig = config.substring(config.indexOf(")")+1).trim();
        
        Class<?>[] pTypes = new Class<?>[paramConfig.length];
        DataType[] cTypes = new DataType[paramConfig.length];
        int idx = 0;
        for (String pc : paramConfig) {

            Pair<Class<?>, String> parsed = parseParamConfig(pc);
            pTypes[idx] = parsed.getFirst();
            cTypes[idx] = getOrCreate(parsed.getSecond(), parsed.getFirst(), types);
            idx++;
        }
        
        method = clazz.getMethod(methodName, pTypes);        
        this.input = new InputSymbol("I_" + methodName, cTypes);
        
        Class<?> rType = method.getReturnType();
        if (rType.equals(Void.TYPE)) {
            this.output = SpecialSymbols.VOID;
            this.isVoid = true;
            this.retType = null;
        } 
        else {
            Pair<Class<?>, String> parsed = parseParamConfig(returnConfig); 
            assert rType.equals(parsed.getFirst());
            DataType rtc = getOrCreate(parsed.getSecond(), parsed.getFirst(), types);
            this.output = new OutputSymbol("O_" + methodName, rtc);
            this.isVoid = false;
            this.retType = rtc;
        }
        
    }

    /**
     * @return the method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @return the input
     */
    public InputSymbol getInput() {
        return input;
    }

    /**
     * @return the output
     */
    public OutputSymbol getOutput() {
        return output;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public DataType getRetType() {
        return retType;
    }
    
    
    private Pair<Class<?>, String> parseParamConfig(String config) throws ClassNotFoundException {
        System.out.println("param config: " + config);
        String[] parts = config.trim().split(":");
        Class<?> cl;
        if (parts[0].trim().equals("boolean")) {
            cl = boolean.class;
        }
        else {
            cl = Class.forName(parts[0].trim());
        }
        return new Pair<Class<?>, String>(cl, parts[1].trim());
    } 
    
    private DataType getOrCreate(String name, Class<?> base, Map<String, DataType> map) {
        DataType ret = map.get(name);
        if (ret == null) {
            ret = new DataType(name, base); 
            map.put(name, ret);
        }
        return ret;
    }
    
}
