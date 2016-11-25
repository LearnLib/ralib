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
package de.learnlib.ralib.example.priority;

import java.util.Collection;
import java.util.PriorityQueue;

import de.learnlib.api.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author Sofia
 */
public final class PriorityQueueOracle implements DataWordOracle {

    public static final DataType doubleType = new DataType("DOUBLE", Double.class);

    public static final InputSymbol POLL = new InputSymbol("poll", new DataType[]{doubleType});
    public static final InputSymbol OFFER = new InputSymbol("offer", new DataType[]{doubleType});

    private long resets = 0;
    
    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
        
        for (Query<PSymbolInstance, Boolean> query : clctn) {
            resets++;
            if (query.getInput().length() < 1) {
                query.answer(true);
            } else {
                PriorityQueue<Double> queue = new PriorityQueue<Double>();
                PSymbolInstance[] trace = new PSymbolInstance[query.getInput().length()];
                Boolean[] answer = new Boolean[query.getInput().length()];

                for (int i = 0; i < query.getInput().length(); i++) {
                    answer[i] = false;
                    try {
                        PSymbolInstance psi = query.getInput().getSymbol(i);
                        DataValue<Double> d = psi.getParameterValues()[0];
                        if (psi.getBaseSymbol().equals(OFFER) && queue.size() < 3) {
                            queue.offer(d.getId());
                            answer[i] = true;
                        } else if (psi.getBaseSymbol().equals(POLL)) {
                            Double val = queue.poll();
                            if (val != null) {
                                if (val.equals(d.getId())) {

                                    answer[i] = true;
                                }
                            }
                        }
                    } catch (Exception e) {

                    }
                }
                query.answer(isArrayTrue(answer));
            }
        }
    }

    private boolean isArrayTrue(Boolean[] maybeArr) {
        boolean maybe = true;
        for (int c = 0; c < (maybeArr.length); c++) {
            if (!maybeArr[c]) {
                maybe = false;
                break;
            }
        }
        return maybe;
    }
    
    public long getResets() {
        return resets;
    }
}
