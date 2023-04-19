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


import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

import java.math.BigDecimal;

public class PriorityQueueSUL extends DataWordSUL {

    public static final DataType DOUBLE_TYPE = 
            new DataType("DOUBLE", BigDecimal.class);

    public static final ParameterizedSymbol POLL = 
            new InputSymbol("poll", new DataType[]{});
    
    public static final ParameterizedSymbol OFFER = 
            new InputSymbol("offer", new DataType[]{DOUBLE_TYPE});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { POLL, OFFER };
    }
        
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    public static final ParameterizedSymbol OUTPUT = 
            new OutputSymbol("_out", new DataType[]{DOUBLE_TYPE});
    
    public static final ParameterizedSymbol OK = 
            new OutputSymbol("_ok", new DataType[]{});
        
    public static final ParameterizedSymbol NOK = 
            new OutputSymbol("_not_ok", new DataType[]{});

    public final ParameterizedSymbol[] getActionSymbols() {
        return new ParameterizedSymbol[] { POLL, OFFER, OUTPUT, OK, NOK, ERROR };
    }


    private PQWrapper pqueue;
    private int capacity;
    
    public PriorityQueueSUL() {
        capacity = PQWrapper.CAPACITY;
    }
    
    public PriorityQueueSUL(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void pre() {
        countResets(1);
        this.pqueue = new PQWrapper(capacity);
    }

    @Override
    public void post() {
        this.pqueue = null;
    }

    private PSymbolInstance createOutputSymbol(Object x) {
        if (x instanceof Boolean) {
            return new PSymbolInstance( ((Boolean) x) ? OK : NOK);
        } else if (x instanceof java.lang.Exception) {
            return new PSymbolInstance(ERROR);
        } else if (x == null) {
            return new PSymbolInstance(NOK);
        } else {
            assert (null != x);
            return new PSymbolInstance(OUTPUT, new DataValue(DOUBLE_TYPE, x));
        }
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        if (i.getBaseSymbol().equals(OFFER)) {
            Object x = pqueue.offer(i.getParameterValues()[0].getId());
            return createOutputSymbol(x);
        } else if (i.getBaseSymbol().equals(POLL)) {
            Object x = pqueue.poll();
            return createOutputSymbol(x);
        } else {
            throw new IllegalStateException("i must be instance of poll or offer");
        }
    }

}
