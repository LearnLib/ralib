package de.learnlib.ralib.example.ineq;

import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class BranchSUL  extends DataWordSUL {

    public static final DataType DOUBLE_TYPE = 
            new DataType("DOUBLE", Double.class);    
    
    public static final ParameterizedSymbol IINIT = 
            new InputSymbol("IConnect", new DataType[]{DOUBLE_TYPE, DOUBLE_TYPE, DOUBLE_TYPE});
    public static final ParameterizedSymbol ITEST_NO_MERGE = 
            new InputSymbol("ISYN", new DataType[]{DOUBLE_TYPE});
    
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { IINIT, ITEST_NO_MERGE};
    }
        
    public static final ParameterizedSymbol OK = 
            new OutputSymbol("_ok", new DataType[]{});
        
    public static final ParameterizedSymbol NOK = 
            new OutputSymbol("_not_ok", new DataType[]{});

    public final ParameterizedSymbol[] getActionSymbols() {
        return new ParameterizedSymbol[] { OK, NOK };
    }


    private BranchClass branchSut;
    private Supplier<BranchClass> supplier;

    
    public BranchSUL() {
    	supplier = () -> new BranchClass();
    }
    

    @Override
    public void pre() {
        countResets(1);
        this.branchSut = supplier.get();
    }

    @Override
    public void post() {
        this.branchSut = null;
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
        if (i.getBaseSymbol().equals(IINIT)) {
            Object x = branchSut.IInit(
            		(Double)i.getParameterValues()[0].getId(), 
            		(Double)i.getParameterValues()[1].getId(),
            		(Double)i.getParameterValues()[2].getId());
            return createOutputSymbol(x);
        } else if (i.getBaseSymbol().equals(ITEST_NO_MERGE)) {
            Object x = branchSut.ITestNoMerge(
            		(Double)i.getParameterValues()[0].getId());
            return createOutputSymbol(x); 
        } 
//        else if (i.getBaseSymbol().equals(ISYNACK)) {
//            Object x = tcpSut.ISYNACK(
//            		(Double)i.getParameterValues()[0].getId());
//            return createOutputSymbol(x); 
//        } else if (i.getBaseSymbol().equals(IACK)) {
//            Object x = tcpSut.IACK(
//            		(Double)i.getParameterValues()[0].getId());
//            return createOutputSymbol(x); 
//        } 
        else {
            throw new IllegalStateException("i must be instance of connect or flag config");
        }
    }
    
}
