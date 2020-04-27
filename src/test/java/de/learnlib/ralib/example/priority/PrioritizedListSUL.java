package de.learnlib.ralib.example.priority;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class PrioritizedListSUL extends DataWordSUL {

    public static final DataType DOUBLE_TYPE = 
            new DataType("DOUBLE", Double.class);    

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


    private PrioritizedList pqueue;
    private int capacity;
	private int[] order;
    
    public PrioritizedListSUL(int capacity, int [] order) {
    	this.capacity = capacity;
    	this.order = order;
    }

    @Override
    public void pre() {
        countResets(1);
        pqueue = new PrioritizedList(capacity, order);
    }

    @Override
    public void post() {
        pqueue = null;
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
            return new PSymbolInstance(OUTPUT, new DataValue<Double>(DOUBLE_TYPE, (Double)x));
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
