package de.learnlib.ralib.example.ineq;

import java.util.Map;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 * A simple class encoding which accepts a word "put(x,y,z)" iff
 * z > x and z != y. Output is "ok" if accepted, and "not_ok" otherwise.
 * 
 * @author Bharat
 */
public class BharatExampleSUL extends DataWordSUL {

    public enum Actions{
        PUT,
        OK,
        NOK
    }

    private Map<DataType, Theory> teachers;
    private Constants consts;
    private Map<Actions, ParameterizedSymbol> inputs;
    private Map<Actions, ParameterizedSymbol> outputs;
    private Boolean SULfull;

    public BharatExampleSUL(
            Map<DataType, Theory> teachers,
            Constants consts, Map<Actions, ParameterizedSymbol> inputs, Map<Actions, ParameterizedSymbol> outputs) {
        this.SULfull = false;
        this.teachers = teachers;
        this.consts = consts;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public void pre() {
        countResets(1);
        this.SULfull = false;
        return;
    }

    @Override
    public void post() {
       this.SULfull = null;
       return;
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        if (!i.getBaseSymbol().getName().equalsIgnoreCase(Actions.PUT.toString())) {
            throw new IllegalStateException("i must be instance of put");
        }

        return createOutputSymbol(i);
    }

    private PSymbolInstance createOutputSymbol(PSymbolInstance i) {
        if (this.SULfull) {
            return new PSymbolInstance(outputs.get(Actions.NOK));
        }
        if (!this.SULfull) {
            this.SULfull = true;
            if ( ( (double) i.getParameterValues()[2].getId() > (double) i.getParameterValues()[0].getId()) 
                && ((double) i.getParameterValues()[2].getId() !=  (double) i.getParameterValues()[1].getId())
                ) {
                    return new PSymbolInstance(outputs.get(Actions.OK));
            } else {
                return new PSymbolInstance(outputs.get(Actions.NOK));
            }
        }
        // System.out.println("Input symbol: " + i);
        assert false;
        return null;
    }
    
}