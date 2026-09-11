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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 * A valuation of registers.
 *
 * @author falk
 */
public class RegisterValuation extends Mapping<SymbolicDataValue.Register, DataValue> {

    public static RegisterValuation copyOf(RegisterValuation other) {
        RegisterValuation copy = new RegisterValuation();
        if (other != null) {
            copy.putAll(other);
        }
        return copy;
    }

    public static RegisterValuation fromMemorable(Word<PSymbolInstance> prefix, Set<DataValue> memorable) {
		ArrayList<DataValue> vals = new ArrayList<>();
		vals.addAll(Arrays.asList(DataWords.valsOf(prefix)));

		RegisterValuation regs = new RegisterValuation();
		for (DataValue d : memorable) {
			int id = vals.indexOf(d) + 1;
			assert id > 0 : "Memorable is not in prefix";
			Register r = new Register(d.getDataType(), id);
			regs.put(r, d);
		}
		return regs;
    }

}
