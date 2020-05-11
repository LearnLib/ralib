package de.learnlib.ralib.example.sumc.inequality;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.sumc.inequality.TCPExample.Option;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class OneWayFreshTCPSUL  extends DataWordSUL {

    public static final DataType DOUBLE_TYPE = 
            new DataType("DOUBLE", Double.class);    
    
    public static final ParameterizedSymbol ICONNECT = 
            new InputSymbol("IConnect", new DataType[]{});
    public static final ParameterizedSymbol ISYN = 
            new InputSymbol("ISYN", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol ISYNACK = 
            new InputSymbol("ISYNACK", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol IACK = 
            new InputSymbol("IACK", new DataType[]{DOUBLE_TYPE});
    
    public static final ParameterizedSymbol IFINACK = 
            new InputSymbol("IFINACK", new DataType[]{DOUBLE_TYPE});
    
    
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { ICONNECT, ISYN, ISYNACK, IACK};
    }
        
    public static final ParameterizedSymbol OCONNECT = 
            new OutputSymbol("OConnect", new DataType[]{
            		DOUBLE_TYPE
            });
    
    public static final ParameterizedSymbol OK = 
            new OutputSymbol("_ok", new DataType[]{});
    
    public static final ParameterizedSymbol NOK = 
            new OutputSymbol("_not_ok", new DataType[]{});

    public final ParameterizedSymbol[] getActionSymbols() {
        return new ParameterizedSymbol[] { ICONNECT, ISYN, ISYNACK, IACK, OCONNECT, OK, NOK, ERROR};
    }


    private OneWayFreshTCPExample tcpSut;

	private Option[] options ;

	private Double window;
    
    public OneWayFreshTCPSUL(Double window) {
    	this.window = window;
    }

    @Override
    public void pre() {
        countResets(1);
        this.tcpSut = new OneWayFreshTCPExample(window);
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
        } else if (x instanceof Double) {
        	return new PSymbolInstance(OCONNECT, new DataValue<Integer>(DOUBLE_TYPE, (Integer) x));
        } else {
        	throw new IllegalStateException("Output not supported");
        }
     }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        if (i.getBaseSymbol().equals(ICONNECT)) {
            Object x = tcpSut.IConnect();
            return new PSymbolInstance(OCONNECT, new DataValue<Double>(DOUBLE_TYPE, (Double)x));
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
        } else if (i.getBaseSymbol().equals(IFINACK)) {
            Object x = tcpSut.IFINACK(
            		(Double)i.getParameterValues()[0].getId());
            return createOutputSymbol(x); 
        }  
        
        else {
            throw new IllegalStateException("i must be instance of connect or flag config");
        }
    }
    
}
