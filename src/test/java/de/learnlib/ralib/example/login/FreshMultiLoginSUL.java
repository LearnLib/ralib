package de.learnlib.ralib.example.login;

import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class FreshMultiLoginSUL extends DataWordSUL  {
	public static final DataType INT_TYPE = new DataType("INTEGER", Integer.class);

	public static final ParameterizedSymbol ILOGIN = new InputSymbol("ILogin", new DataType[] { INT_TYPE, INT_TYPE });
	public static final ParameterizedSymbol IREGISTER = new InputSymbol("IRegister", new DataType[] { INT_TYPE});
	public static final ParameterizedSymbol ILOGOUT = new InputSymbol("ILogout", new DataType[] { INT_TYPE, INT_TYPE });

	public static final ParameterizedSymbol ICHANGEPASSWORD = new InputSymbol("IChangePassword", new DataType[] { INT_TYPE, INT_TYPE });

	public static final ParameterizedSymbol ERROR = new OutputSymbol("_io_err", new DataType[] {});

	public final ParameterizedSymbol[] getInputSymbols() {
		return new ParameterizedSymbol[] { ILOGIN, IREGISTER, ILOGOUT, ICHANGEPASSWORD};
	}

	public static final ParameterizedSymbol OREGISTER = new OutputSymbol("OPwd",
			new DataType[] { INT_TYPE });
	public static final ParameterizedSymbol OK = 
	            new OutputSymbol("OK", new DataType[]{});
	        
	public static final ParameterizedSymbol NOK = new OutputSymbol("NOK", new DataType[]{});

	public final ParameterizedSymbol[] getOutputSymbols() {
		return new ParameterizedSymbol[] { OREGISTER, OK, NOK};
	}

	private FreshMultiLogin tcpSut;
	private Supplier<FreshMultiLogin> supplier;

	public FreshMultiLoginSUL() {
		supplier = () -> new FreshMultiLogin();
	}
	
	public FreshMultiLoginSUL(int maxRegUsers) {
		supplier = () -> {
			FreshMultiLogin sul = new FreshMultiLogin();
			sul.setMaxRegUsers(maxRegUsers);
			return sul;
		};
	}


	@Override
	public void pre() {
		countResets(1);
		this.tcpSut = supplier.get();
	}

	@Override
	public void post() {
		this.tcpSut = null;
	}


	private PSymbolInstance createOutputSymbol(Object x) {
		if (x instanceof Integer) {
			return new PSymbolInstance(OREGISTER, new DataValue[]{ dv(x)});
		} else {
			if (x instanceof Boolean) {
				if (((boolean) x)) {
					return new PSymbolInstance(OK);
				} else {
					return new PSymbolInstance(NOK);
				}
			} else {
	        	throw new IllegalStateException("Output not supported");
	        }
		}
	}

	public DataValue<Integer> dv(Object val) {
		return new DataValue<Integer>(INT_TYPE, (Integer) val);
	}

	@Override
	public PSymbolInstance step(PSymbolInstance i) throws SULException {
		countInputs(1);
		if (i.getBaseSymbol().equals(ILOGIN)) {
			Object x = tcpSut.ILogin((Integer) i.getParameterValues()[0].getId(),
					(Integer) i.getParameterValues()[1].getId());
			return createOutputSymbol(x);
		} else if (i.getBaseSymbol().equals(ILOGOUT)) {
			Object x = tcpSut.ILogout((Integer) i.getParameterValues()[0].getId());
			return createOutputSymbol(x);
		} else if (i.getBaseSymbol().equals(IREGISTER)) {
			Object x = tcpSut.IRegister((Integer) i.getParameterValues()[0].getId());
			return createOutputSymbol(x);
		} 

		else {
			throw new IllegalStateException("must be instance of connect or flag config");
		}
	}
	
	public static void main(String args[]) {
		
	}
}
