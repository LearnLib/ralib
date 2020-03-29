package de.learnlib.ralib.example.fresh;

import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class SessionManagerSUL extends DataWordSUL {

    public static final DataType INT_TYPE = 
            new DataType("INT", Integer.class);    
    
    public static final ParameterizedSymbol ISESSION = 
            new InputSymbol("IConnect", new DataType[]{INT_TYPE});
    public static final ParameterizedSymbol ILOGIN = 
            new InputSymbol("ILogin", new DataType[]{INT_TYPE, INT_TYPE});
    public static final ParameterizedSymbol ILOGOUT = 
            new InputSymbol("ILogout", new DataType[]{INT_TYPE});
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { ISESSION, ILOGIN, ILOGOUT};
    }
        
    
    public static final ParameterizedSymbol OSESSION = 
            new OutputSymbol("OSession", new DataType[]{INT_TYPE});
    
    public static final ParameterizedSymbol OK = 
            new OutputSymbol("_ok", new DataType[]{});
        
    public static final ParameterizedSymbol NOK = 
            new OutputSymbol("_not_ok", new DataType[]{});

    public final ParameterizedSymbol[] getActionSymbols() {
        return new ParameterizedSymbol[] { OK, NOK, OSESSION };
    }


    private SessionManager sessionManager;

    public SessionManagerSUL() {
    }
    

    @Override
    public void pre() {
        countResets(1);
        sessionManager = new SessionManager();
    }

    @Override
    public void post() {
        this.sessionManager = null;
    }
    
    private PSymbolInstance createOutputSymbol(Object x) {
        if (x instanceof Boolean) {
        	return new PSymbolInstance( ((Boolean) x) ? OK : NOK);
        } else if (x instanceof Integer){
        	return new PSymbolInstance( OSESSION, new DataValue<>(INT_TYPE, (Integer) x));
        } else {
        	throw new IllegalStateException("Output not supported");
        }
     }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        if (i.getBaseSymbol().equals(ISESSION)) {
            Object x = sessionManager.ISession(
            		(Integer)i.getParameterValues()[0].getId());
            return createOutputSymbol(x);
        } else if (i.getBaseSymbol().equals(ILOGIN)) {
            Object x = sessionManager.ILogin(
            		(Integer)i.getParameterValues()[0].getId(),
            		(Integer)i.getParameterValues()[1].getId());
            return createOutputSymbol(x); 
        } else if (i.getBaseSymbol().equals(ILOGOUT)) {
            Object x = sessionManager.ILogout(
            		(Integer)i.getParameterValues()[0].getId());
            return createOutputSymbol(x); 
        } else {
            throw new IllegalStateException("i must be instance of connect or flag config");
        }
    }
    
}
