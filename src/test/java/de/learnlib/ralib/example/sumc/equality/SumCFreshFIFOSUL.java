package de.learnlib.ralib.example.sumc.equality;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class SumCFreshFIFOSUL extends DataWordSUL{

    public static final DataType INT_TYPE = 
            new DataType("int", Integer.class);    

    public static final ParameterizedSymbol POLL = 
            new InputSymbol("poll", new DataType[]{INT_TYPE});
    
    public static final ParameterizedSymbol OFFER = 
            new InputSymbol("offer", new DataType[]{});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { POLL, OFFER };
    }
        
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    public static final ParameterizedSymbol OUTPUT = 
            new OutputSymbol("_out", new DataType[]{INT_TYPE});
    
    public static final ParameterizedSymbol OK = 
            new OutputSymbol("_ok", new DataType[]{});
        
    public static final ParameterizedSymbol NOK = 
            new OutputSymbol("_not_ok", new DataType[]{});

    public final ParameterizedSymbol[] getActionSymbols() {
        return new ParameterizedSymbol[] { POLL, OFFER, OUTPUT, OK, NOK, ERROR };
    }


    private SumCFreshFIFOExample fifo;
    private int capacity;

	private int sumc;
    
    public SumCFreshFIFOSUL(int capacity, int sumc) {
    	this.capacity = capacity;
    	this.sumc = sumc;
    }

    @Override
    public void pre() {
        countResets(1);
        fifo = new SumCFreshFIFOExample(capacity, sumc);
    }

    @Override
    public void post() {
        fifo = null;
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
            return new PSymbolInstance(OUTPUT, new DataValue(INT_TYPE, x));
        }
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        if (i.getBaseSymbol().equals(OFFER)) {
            Object x = fifo.offer();
            return createOutputSymbol(x);
        } else if (i.getBaseSymbol().equals(POLL)) {
            Object x = fifo.poll((Integer) i.getParameterValues()[0].getId());
            return createOutputSymbol(x);
        } else {
            throw new IllegalStateException("i must be instance of poll or offer");
        }
    }
}
