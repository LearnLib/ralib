/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.sul;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Map;
import java.util.PriorityQueue;

public class PriorityQueueSUL extends DataWordSUL {

    public enum Actions {

        OFFER,
        POLL,
        OUTPUT,
        ERROR,
        OK,
        NOK
    }

    private PriorityQueue pqueue;
    private final Map<DataType, Theory> teachers;
    private final Constants consts;
    private final int limit;

    private Map<Actions, ParameterizedSymbol> inputs;
    private Map<Actions, ParameterizedSymbol> outputs;

    public PriorityQueueSUL(Map<DataType, Theory> teachers,
            Constants consts, Map<Actions, ParameterizedSymbol> inputs, Map<Actions, ParameterizedSymbol> outputs, int limit) {
        this.pqueue = new PriorityQueue<>();
        this.teachers = teachers;
        this.consts = consts;
        this.inputs = inputs;
        this.outputs = outputs;
        this.limit = limit;
    }

    @Override
    public void pre() {
        countResets(1);
        this.pqueue = new PriorityQueue<>();
    }

    @Override
    public void post() {
        this.pqueue = null;
    }

    private PSymbolInstance createOutputSymbol(Object x) {
        if (x instanceof Boolean) {
            if ((Boolean) x) {
                System.out.println("returns OK");

                return new PSymbolInstance(outputs.get(Actions.OK));
            } else {
                System.out.println("returns NOK");

                return new PSymbolInstance(outputs.get(Actions.NOK));
            }
        } else if (x instanceof java.lang.Exception) {
            System.out.println("returns ERR");

            return new PSymbolInstance(outputs.get(Actions.ERROR));
        } else if (x == null) {
            System.out.println("returns NOK");

            return new PSymbolInstance(outputs.get(Actions.NOK));
        } else {
            assert !(x == null);
            System.out.println("returns OUTPUT " + x.toString());
            ParameterizedSymbol op = outputs.get(Actions.OUTPUT);
            return new PSymbolInstance(op, new DataValue(op.getPtypes()[0], x));
        }
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        System.out.println("executing:  " + i.toString() + " on " + pqueue.toString());
        if (i.getBaseSymbol().equals(inputs.get(Actions.OFFER))) {
            //DataValue<Double> d = i.getParameterValues()[0];
            if (pqueue.size() < limit) {
                Object x = pqueue.offer(i.getParameterValues()[0].getId());

                return createOutputSymbol(x);
            } else {
                System.out.println("returns NOK");
                return new PSymbolInstance(outputs.get(Actions.NOK));
            }

        } else if (i.getBaseSymbol().equals(inputs.get(Actions.POLL))) {
            Object x = pqueue.poll();
            return createOutputSymbol(x);
        } else {
            throw new IllegalStateException("i must be instance of poll or offer");
        }

    }

}
