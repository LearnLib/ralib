package de.learnlib.ralib.example.repeater;

import de.learnlib.exception.SULException;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.symbol.ParameterizedSymbol;

public class RepeaterSUL extends DataWordSUL {
	public static final DataType<Integer> TINT =
			new DataType<>("int", Integer.class);

	public static final ParameterizedSymbol IPUT =
			new InputSymbol("put", new DataType[] {TINT});
	public static final ParameterizedSymbol OECHO =
			new OutputSymbol("echo", new DataType[] {TINT});
	public static final ParameterizedSymbol ONOK =
			new OutputSymbol("nok", new DataType[] {});

    public static final ParameterizedSymbol ERROR =
            new OutputSymbol("_io_err", new DataType[]{});

    public final ParameterizedSymbol[] getInputSymbols() {
    	return new ParameterizedSymbol[] { IPUT };
    }

    public final ParameterizedSymbol[] getActionSymbols() {
    	return new ParameterizedSymbol[] { IPUT, OECHO, ONOK };
    }

    private Repeater repeater;
    private int max_repeats;
    private int capacity;

    public RepeaterSUL() {
    	max_repeats = Repeater.MAX_REPEATS;
    	capacity = Repeater.CAPACITY;
    }

    public RepeaterSUL(int max_repeats) {
    	this.max_repeats = max_repeats;
    	capacity = Repeater.CAPACITY;
    }

    public RepeaterSUL(int max_repeats, int capacity) {
    	this.max_repeats = max_repeats;
    	this.capacity = capacity;
    }

	@Override
	public void pre() {
        countResets(1);
		repeater = new Repeater(max_repeats, capacity);
	}

	@Override
	public void post() {
		repeater = null;
	}

	private PSymbolInstance createOutputSymbol(Integer x) {
		if (x == null)
			return new PSymbolInstance(ONOK);
		return new PSymbolInstance(OECHO, new DataValue<Integer>(TINT, x.intValue()));
	}

	@Override
	public PSymbolInstance step(PSymbolInstance in) throws SULException {
        countInputs(1);
        if (in.getBaseSymbol().equals(IPUT)) {
        	Integer p = (Integer)in.getParameterValues()[0].getValue();
        	Integer x = repeater.repeat(p);
        	return createOutputSymbol(x);
        } else {
        	throw new IllegalStateException("in must be instance of IPUT");
        }
	}
}
