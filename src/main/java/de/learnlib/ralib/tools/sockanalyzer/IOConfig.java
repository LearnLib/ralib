/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.tools.sockanalyzer;

import java.util.Map;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.commons.util.Pair;

/**
 *
 * @author falk
 */
public class IOConfig {
    
    private final ParameterizedSymbol symbol;
    
    
    public IOConfig(String config, boolean isInput, Map<String, DataType> types) 
            throws ClassNotFoundException, NoSuchMethodException {
        
        String actionName = config.substring(0, config.indexOf("(")).trim();
        String paramString = config.substring(config.indexOf("(") +1, config.indexOf(")")).trim();        
        String[] paramConfig = (paramString.length() < 1) ? 
                new String[0] : config.substring(config.indexOf("(") +1, config.indexOf(")")).trim().split(",");
        
        Class<?>[] pTypes = new Class<?>[paramConfig.length];
        DataType[] cTypes = new DataType[paramConfig.length];
        int idx = 0;
        for (String pc : paramConfig) {

            Pair<Class<?>, String> parsed = parseParamConfig(pc);
            pTypes[idx] = parsed.getFirst();
            cTypes[idx] = getOrCreate(parsed.getSecond(), parsed.getFirst(), types);
            idx++;
        }
        
        if (isInput) {
        	symbol = new InputSymbol(actionName, cTypes);
        } else {
        	symbol = new OutputSymbol(actionName, cTypes);
        }
        
    }

    /**
     * @return the the parameterized symbol
     */
    public ParameterizedSymbol  getSymbol() {
        return symbol;
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
