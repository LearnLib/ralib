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


import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 * A valuation of parameters.
 *
 * @author falk
 */
public class ParameterValuation extends Mapping<SymbolicDataValue.Parameter, DataValue> {

    public static ParameterValuation fromPSymbolInstance(PSymbolInstance psi) {
        ParameterValuation val = new ParameterValuation();
        ParameterGenerator pgen = new ParameterGenerator();
        for (DataValue dv : psi.getParameterValues()) {
            val.put(pgen.next(dv.getDataType()), dv);
        }
        return val;
    }

    public static ParameterValuation fromPSymbolWord(Word<PSymbolInstance> dw) {
        ParameterValuation val = new ParameterValuation();
        ParameterGenerator pgen = new ParameterGenerator();
        for (PSymbolInstance psi : dw) {
            for (DataValue dv : psi.getParameterValues()) {
                val.put(pgen.next(dv.getDataType()), dv);
            }
        }
        return val;
    }
}
