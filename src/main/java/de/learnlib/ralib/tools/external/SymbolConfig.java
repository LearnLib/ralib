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
package de.learnlib.ralib.tools.external;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.InputSymbol;
import java.util.Map;

/**
 *
 * @author falk
 */
public class SymbolConfig {
    
    private final String name;
    
    private final  DataType[] cTypes;

    public SymbolConfig(String config, Map<String, DataType> types) {
        
        name = config.substring(0, config.indexOf("(")).trim();
        String paramString = config.substring(config.indexOf("(") +1, config.indexOf(")")).trim();        
        String[] paramConfig = (paramString.length() < 1) ? 
                new String[0] : config.substring(config.indexOf("(") +1, config.indexOf(")")).trim().split(",");
        
        cTypes = new DataType[paramConfig.length];
        int idx = 0;
        for (String pc : paramConfig) {

            cTypes[idx] = getOrCreate(pc, types);
            idx++;
        }
    }

    /**
     * @return the method
     */
    public String getName() {
        return name;
    }

    public DataType[] getcTypes() {
        return cTypes;
    }

    private DataType getOrCreate(String name, Map<String, DataType> map) {
        DataType ret = map.get(name);
        if (ret == null) {
            ret = new DataType(name, Integer.class); 
            map.put(name, ret);
        }
        return ret;
    }
    
}
