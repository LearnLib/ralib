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
package de.learnlib.ralib.data;

import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A mapping from parameters to registers.
 *
 * @author falk
 */
public class PIV extends VarMapping<Parameter, Register> {

    public PIV() {
    }

    public PIV relabel(VarMapping relabelling) {
        PIV ret = new PIV();
        for (Map.Entry<Parameter, Register> e : this) {
            Parameter p = (Parameter) relabelling.get(e.getKey());
            Register r = (Register) relabelling.get(e.getValue());
            ret.put(p == null ? e.getKey() : p, r == null ? e.getValue() : r);
        }
        return ret;
    }

    public Map<DataType, Integer> typedSize() {
        Map<DataType, Integer> ret = new LinkedHashMap<>();
        for (Parameter p : keySet()) {
            Integer i = ret.get(p.getType());
            i = (i == null) ? 1 : i+1;
            ret.put(p.getType(), i);
        }
        return ret;
    }

    public Map<DataType, Parameter[]> asTypedArrays() {
        Map<DataType, List<Parameter>> tmp = new LinkedHashMap<>();
        for (Parameter p : keySet()) {
            List<Parameter> list = tmp.get(p.getType());
            if (list == null) {
                list = new ArrayList<>();
                tmp.put(p.getType(), list);
            }
            list.add(p);
        }

        Map<DataType, Parameter[]> ret = new LinkedHashMap<>();
        for (Map.Entry<DataType, List<Parameter>> e : tmp.entrySet()) {
            ret.put(e.getKey(), e.getValue().toArray(new Parameter[] {}));
        }
        return ret;
    }

    //FIXME: this method is bogus. There may be more than one value.
    public Parameter getOneKey(Register value) {
        Parameter retKey = null;
        for (Parameter key : this.keySet()) {
//            System.out.println("key = " + key.toString());
//            System.out.println("value = " + this.get(key).toString());
            if (this.get(key).getId().equals(value.getId())){
//                System.out.println(this.get(key).toString() + " equals " + value.toString());
                retKey = key;
                break;
            }
        }
        return retKey;
    }

    /**
     * Creates a remapping from registers from this PIV to the supplied PIV.
     */
    public VarMapping<Register, Register> createRemapping(PIV to) {
        // there should not be any register with id > n
        for (Register r : to.values()) {
            if (r.getId() > to.size()) {
                throw new IllegalStateException("there should not be any register with id > n: " + to);
            }
        }

        VarMapping<Register, Register> map = new VarMapping<>();

        int id = to.size() + 1;
        for (Entry<Parameter, Register> e : this) {
            Register rep = to.get(e.getKey());
            if (rep == null) {
                rep = new Register(e.getValue().getType(), id++);
            }
            map.put(e.getValue(), rep);
        }

        return map;
    }
}
