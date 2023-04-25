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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

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

    private final Map<DataType, Map<DataValue, Object>> buckets = new HashMap<>();

    public ClasssAnalyzerDataWordSUL(Class<?> sulClass, Map<ParameterizedSymbol, MethodConfig> methods, int d) {
        this.sulClass = sulClass;
        this.methods = methods;
        this.maxDepth = d;
    }

    @Override
    public void pre() {
        //System.out.println("----------");
        countResets(1);
        buckets.clear();
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
        updateSeen(i.getParameterValues());

        if (depth > maxDepth && (maxDepth > 0)) {
            return new PSymbolInstance(SpecialSymbols.DEPTH);
        }
        depth++;

        MethodConfig in = methods.get(i.getBaseSymbol());
        Method act = in.getMethod();

        DataValue[] dvs = i.getParameterValues();
        Object[] params = new Object[dvs.length];
        for (int j = 0; j < dvs.length; j++) {
            params[j] = resolve(dvs[j]);
        }

        Object ret = null;
        try {
            ret = act.invoke(sul, params);
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                InvocationTargetException iex = (InvocationTargetException) ex;
                return new PSymbolInstance(new SpecialSymbols.ErrorSymbol(
                        iex.getTargetException()));
            } else {
                throw new RuntimeException(ex);
            }
        }

        if (in.isVoid()) {
            return new PSymbolInstance(SpecialSymbols.VOID);
        }

        if (ret == null) {
            return new PSymbolInstance(SpecialSymbols.NULL);
        }

        if (in.getRetType().equals(SpecialSymbols.BOOLEAN_TYPE)) {
            return new PSymbolInstance((Boolean) ret ? SpecialSymbols.TRUE : SpecialSymbols.FALSE);
        }

        DataValue retVal = (isFresh(in.getRetType(), ret))
                ? registerFreshValue(in.getRetType(), ret)
                : new DataValue(in.getRetType(), ret);

        //updateSeen(retVal);
        return new PSymbolInstance(in.getOutput(), retVal);

    }

    private void updateSeen(DataValue... vals) {
        for (DataValue v : vals) {
            Map<DataValue, Object> map = this.buckets.get(v.getType());
            if (map == null) {
                map = new HashMap<>();
                this.buckets.put(v.getType(), map);
            }

            if (!map.containsKey(v)) {
                //System.out.println("Put: " + v + " : " + v.getId());
                map.put(v, v.getId());
            }
        }
    }

    private Object resolve(DataValue d) {
        Map<DataValue, Object> map = this.buckets.get(d.getType());
        if (map == null || !map.containsKey(d)) {
            //System.out.println(d);
            assert false;
            return d.getId();
        }
        //System.out.println("Get: " + d + " : " + map.get(d));
        return map.get(d);
    }

    private boolean isFresh(DataType t, Object id) {
        Map<DataValue, Object> map = this.buckets.get(t);
        return map == null || !map.containsValue(id);
    }

    private DataValue registerFreshValue(DataType retType, Object ret) {
        Map<DataValue, Object> map = this.buckets.get(retType);
        if (map == null) {
            map = new HashMap<>();
            this.buckets.put(retType, map);
        }

        DataValue v = new DataValue(retType, map.size());
        //System.out.println("Put (F): " + v + " : " + ret);
        map.put(v, ret);
        return new FreshValue(v.getType(), v.getId());
    }

}
