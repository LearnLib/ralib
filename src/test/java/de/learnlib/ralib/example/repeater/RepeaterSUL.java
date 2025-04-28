package de.learnlib.ralib.example.repeater;

import java.math.BigDecimal;

import de.learnlib.exception.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class RepeaterSUL extends DataWordSUL {
    public static final DataType TINT = new DataType("int");

    public static final ParameterizedSymbol IPUT = new InputSymbol("put", TINT);
    public static final ParameterizedSymbol OECHO = new OutputSymbol("echo", TINT);
    public static final ParameterizedSymbol ONOK = new OutputSymbol("nok");

    public static final ParameterizedSymbol ERROR = new OutputSymbol("_io_err");

    public final ParameterizedSymbol[] getInputSymbols() {
    	return new ParameterizedSymbol[] { IPUT };
    }

    public final ParameterizedSymbol[] getActionSymbols() {
    	return new ParameterizedSymbol[] { IPUT, OECHO, ONOK };
    }

    private Repeater repeater;
    private final int max_repeats;
    private final int capacity;

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
        return new PSymbolInstance(OECHO, new DataValue(TINT, new BigDecimal(x)));
    }

    @Override
    public PSymbolInstance step(PSymbolInstance in) throws SULException {
        countInputs(1);
        if (in.getBaseSymbol().equals(IPUT)) {
            Integer p = in.getParameterValues()[0].getValue().intValue();
            Integer x = repeater.repeat(p);
            return createOutputSymbol(x);
        } else {
            throw new IllegalStateException("in must be instance of IPUT");
        }
    }
}
