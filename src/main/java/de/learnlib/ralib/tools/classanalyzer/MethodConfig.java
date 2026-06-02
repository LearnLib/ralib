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
package de.learnlib.ralib.tools.classanalyzer;

import java.lang.reflect.Method;
import java.util.Map;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import net.automatalib.common.util.Pair;

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
            cTypes[idx] = getOrCreate(parsed.getSecond(), types);
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
            DataType rtc = getOrCreate(parsed.getSecond(), types);
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
        String[] parts = config.trim().split(":", -1);
        Class<?> cl;
        if (parts[0].trim().equals("boolean")) {
            cl = boolean.class;
        }
        else {
            cl = Class.forName(parts[0].trim());
        }
        return Pair.of(cl, parts[1].trim());
    }

    private DataType getOrCreate(String name, Map<String, DataType> map) {
        DataType ret = map.get(name);
        if (ret == null) {
            ret = new DataType(name);
            map.put(name, ret);
        }
        return ret;
    }

}
