package de.learnlib.ralib.example.succ;

import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.examples.AbstractTCPExample.Option;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class OneWayTCPSUL  extends DataWordSUL {

    public static final DataType<Double> DOUBLE_TYPE = 
            new DataType<>("DOUBLE", Double.class);    
    
    public static final ParameterizedSymbol ICONNECT = 
            new InputSymbol("IConnect", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol ISYN = 
            new InputSymbol("ISYN", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol ISYNACK = 
            new InputSymbol("ISYNACK", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol IACK = 
            new InputSymbol("IACK", new DataType[]{DOUBLE_TYPE});
    
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { ICONNECT, ISYN, ISYNACK, IACK};
    }
        
    public static final ParameterizedSymbol OK = 
            new OutputSymbol("_ok", new DataType[]{});
        
    public static final ParameterizedSymbol NOK = 
            new OutputSymbol("_not_ok", new DataType[]{});

    public final ParameterizedSymbol[] getActionSymbols() {
        return new ParameterizedSymbol[] { OK, NOK };
    }


    private OneWayTCPExample tcpSut;
    private Supplier<OneWayTCPExample> supplier;

	private Option[] options ;
    
    public OneWayTCPSUL() {
    	supplier = () -> new OneWayTCPExample();
    }
    
    public OneWayTCPSUL(Double window) {
    	supplier = () -> new OneWayTCPExample(window);
    }

    @Override
    public void pre() {
        countResets(1);
        this.tcpSut = supplier.get();
        if (options != null) {
        	this.tcpSut.configure(options);
        }
    }

    @Override
    public void post() {
        this.tcpSut = null;
    }
    
    public void configure(Option ... options ) {
    	this.options = options;
    }

    private PSymbolInstance createOutputSymbol(Object x) {
        if (x instanceof Boolean) {
            return new PSymbolInstance( ((Boolean) x) ? OK : NOK);
        } else {
        	throw new IllegalStateException("Output not supported");
        }
     }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        if (i.getBaseSymbol().equals(ICONNECT)) {
            Object x = tcpSut.IConnect(
            		(Double)i.getParameterValues()[0].getId());
            return createOutputSymbol(x);
        } else if (i.getBaseSymbol().equals(ISYN)) {
            Object x = tcpSut.ISYN(
            		(Double)i.getParameterValues()[0].getId());
            return createOutputSymbol(x); 
        } else if (i.getBaseSymbol().equals(ISYNACK)) {
            Object x = tcpSut.ISYNACK(
            		(Double)i.getParameterValues()[0].getId());
            return createOutputSymbol(x); 
        } else if (i.getBaseSymbol().equals(IACK)) {
            Object x = tcpSut.IACK(
            		(Double)i.getParameterValues()[0].getId());
            return createOutputSymbol(x); 
        } else {
            throw new IllegalStateException("i must be instance of connect or flag config");
        }
    }
    
}
