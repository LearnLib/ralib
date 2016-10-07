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
            new InputSymbol("IInit", new DataType[]{DOUBLE_TYPE, DOUBLE_TYPE, DOUBLE_TYPE});
    public static final ParameterizedSymbol ITEST_NO_MERGE = 
            new InputSymbol("ITestNoMerge", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol ITEST_EQU_DISEQ = 
            new InputSymbol("ITestIneqDiseq", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol ITEST_FIRST_TWO = 
            new InputSymbol("ITestFirstTwo", new DataType[]{DOUBLE_TYPE});
    public static final ParameterizedSymbol ITEST_LAST_TWO = 
            new InputSymbol("ITestLastTwo", new DataType[]{DOUBLE_TYPE});
    
    
    public static final ParameterizedSymbol ITEST_IS_MAX = 
            new InputSymbol("ITestIsMax", new DataType[]{DOUBLE_TYPE});
    
    public static final ParameterizedSymbol ITEST_True = 
            new InputSymbol("ITestTrue", new DataType[]{DOUBLE_TYPE});
    
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { IINIT, ITEST_NO_MERGE, ITEST_FIRST_TWO, ITEST_LAST_TWO, ITEST_EQU_DISEQ};
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
        } else {
//        	branchSut.getClass().getMethod(i.getBaseSymbol().getName(), Double.class).invoke(branchSut, 
//        			(Double)i.getParameterValues()[0].getId());
        	if (i.getBaseSymbol().equals(ITEST_NO_MERGE)) {
            Object x = branchSut.ITestNoMerge(
            		(Double)i.getParameterValues()[0].getId());
            return createOutputSymbol(x); 
	        } else if (i.getBaseSymbol().equals(ITEST_EQU_DISEQ)) {
	            Object x = branchSut.ITestMergeEquDiseq(
	            		(Double)i.getParameterValues()[0].getId());
	            return createOutputSymbol(x); 
	        } else if (i.getBaseSymbol().equals(ITEST_FIRST_TWO)) {
	            Object x = branchSut.ITestMergeFirstTwo((Double)i.getParameterValues()[0].getId());
	            return createOutputSymbol(x); 
	        } else if (i.getBaseSymbol().equals(ITEST_LAST_TWO)) {
	            Object x = branchSut.ITestMergeLastTwo((Double)i.getParameterValues()[0].getId());
	            return createOutputSymbol(x); 
	        } else {
	            throw new IllegalStateException("i must be instance of init or a test command");
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
        } 
    }
    
}
