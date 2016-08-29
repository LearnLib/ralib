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

import java.util.Map;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class PriorityQueueSUL extends DataWordSUL {

    public enum Actions {

        OFFER,
        POLL,
        OUTPUT,
        ERROR,
        OK,
        NOK
    }

    private PQWrapper pqueue;
    private final Map<DataType, Theory> teachers;
    private final Constants consts;
    
    private Map<Actions, ParameterizedSymbol> inputs;
    private Map<Actions, ParameterizedSymbol> outputs;

    public PriorityQueueSUL(Map<DataType, Theory> teachers,
            Constants consts, Map<Actions, ParameterizedSymbol> inputs, Map<Actions, ParameterizedSymbol> outputs) {
        this.pqueue = new PQWrapper();
        this.teachers = teachers;
        this.consts = consts;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public void pre() {
        countResets(1);
        this.pqueue = new PQWrapper();
    }

    @Override
    public void post() {
        this.pqueue = null;
    }

    private PSymbolInstance createOutputSymbol(Object x) {
        if (x instanceof Boolean) {
            if ((Boolean) x) {
//                System.out.println("returns OK");

                return new PSymbolInstance(outputs.get(Actions.OK));
            } else {
//                System.out.println("returns NOK");

                return new PSymbolInstance(outputs.get(Actions.NOK));
            }
        } else if (x instanceof java.lang.Exception) {
//            System.out.println("returns ERR");

            return new PSymbolInstance(outputs.get(Actions.ERROR));
        } else if (x == null) {
//            System.out.println("returns NOK");

            return new PSymbolInstance(outputs.get(Actions.NOK));
        } else {
            assert !(x == null);
//            System.out.println("returns OUTPUT " + x.toString());
            ParameterizedSymbol op = outputs.get(Actions.OUTPUT);
            return new PSymbolInstance(op, new DataValue(op.getPtypes()[0], x));
        }
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
//        System.out.println("executing:  " + i.toString() + " on " + pqueue.toString());
        if (i.getBaseSymbol().equals(inputs.get(Actions.OFFER))) {
            //DataValue<Double> d = i.getParameterValues()[0];
                Object x = pqueue.offer(i.getParameterValues()[0].getId());

                return createOutputSymbol(x);
        } else if (i.getBaseSymbol().equals(inputs.get(Actions.POLL))) {
            Object x = pqueue.poll();
            return createOutputSymbol(x);
        } else {
            throw new IllegalStateException("i must be instance of poll or offer");
        }

    }

}
