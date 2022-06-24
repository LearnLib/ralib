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
package de.learnlib.ralib.ceanalysis;


import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Essentializer<T> {

    private final EqualityTheory<T> theory;

    private final DataWordOracle oracle;

    public Essentializer(EqualityTheory<T> theory, DataWordOracle oracle) {
        this.theory = theory;
        this.oracle = oracle;
    }

    public Word<PSymbolInstance> essentialEq(Word<PSymbolInstance> in) {
        final boolean refOut = oracle.answerQuery(in);
        final Word<ParameterizedSymbol> acts = DataWords.actsOf(in);
        DataValue[] vals = DataWords.valsOf(in);

        IDX: for (int index=vals.length-1; index>=0; index--) {
            final DataValue v = vals[index];
            final LinkedList<Integer> indices = indexesOf(vals, v);
            // is index unique or first?
            if (indices.size() == 1 || indices.peek() == index) {
                continue;
            }
            // can we make index unique
            DataValue<T> fresh = theory.getFreshValue(Arrays.asList(vals));
            vals[index] = fresh;
            if(refOut == oracle.answerQuery(DataWords.instantiate(acts, vals))) {
                continue;
            }
            // is it the last of its kind?
            // or can we break up the rest?
            if (indices.peekLast() > index) {
                Integer[] sublist = subListFrom(indices, index);
                for (int c=0; c<(1<<sublist.length)-1; c++) {
                    for (int i=0; i<sublist.length; i++) {
                        vals[sublist[i]] = (c & (1<<i)) == 0 ? fresh : v;
                    }
                    if (refOut == oracle.answerQuery(DataWords.instantiate(acts, vals))) {
                        continue IDX;
                    }
                }
                for (int i=0; i<sublist.length; i++) {
                    vals[sublist[i]] = v;
                }
            }
            vals[index] = v;
        }
        return DataWords.instantiate(acts, vals);
    }

    private Integer[] subListFrom(LinkedList<Integer> list, Integer i) {
        LinkedList<Integer> sublist = new LinkedList<>();
        for (Integer index : list) {
            if (index > i) {
                sublist.add(index);
            }
        }
        return sublist.toArray(new Integer[] {});
    }

    private LinkedList<Integer> indexesOf(DataValue[] vals, DataValue v) {
        LinkedList<Integer> list = new LinkedList<>();
        for (int i=0; i< vals.length; i++) {
            if (vals[i].equals(v)) {
                list.add(i);
            }
        }
        return list;
    }
}