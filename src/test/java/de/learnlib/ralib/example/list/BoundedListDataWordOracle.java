package de.learnlib.ralib.example.list;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Supplier;

import de.learnlib.query.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author Paul Fiterau
 *
 */
public class BoundedListDataWordOracle implements DataWordOracle {
    public static final DataType INT_TYPE= new DataType("int");

    public static final InputSymbol PUSH = new InputSymbol("push", INT_TYPE);
    public static final InputSymbol INSERT = new InputSymbol("insert", INT_TYPE, INT_TYPE);
    public static final InputSymbol POP = new InputSymbol("pop", INT_TYPE);
    public static final InputSymbol CONTAINS = new InputSymbol("contains", INT_TYPE);


    private final Supplier<BoundedList> factory;

    public BoundedListDataWordOracle(Supplier<BoundedList> factory) {
        this.factory = factory;
    }

    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
        for (Query<PSymbolInstance, Boolean> query : queries) {
            query.answer(answer(query.getInput()));
        }
    }

    private Boolean answer(Word<PSymbolInstance> input) {
        BoundedList list = factory.get();
        try {
            for (PSymbolInstance symInst : input) {
                boolean accepts = accepts(symInst, list);
                if (!accepts) {
                    return false;
                }
            }
        } catch(Exception ignored) {
            // any errors in the processing of an input should result in rejection
            return false;
        }

        return true;
    }

    private boolean accepts(PSymbolInstance symInst, BoundedList list) {
        if (symInst.getBaseSymbol().equals(PUSH)) {
            list.push(  symInst.getParameterValues()[0].getValue() );
            return true;
        } else if (symInst.getBaseSymbol().equals(POP)) {
            BigDecimal value = list.pop();
            return symInst.getParameterValues()[0].getValue().equals(value);
        } else if (symInst.getBaseSymbol().equals(INSERT)) {
            list.insert( symInst.getParameterValues()[0].getValue(), symInst.getParameterValues()[1].getValue());
            return true;
        } else if (symInst.getBaseSymbol().equals(CONTAINS)) {
            return list.contains( symInst.getParameterValues()[0].getValue() );
        }
        return false;
    }

    public static DataValue dv(int val) {
        return new DataValue(INT_TYPE, new BigDecimal(val));
    }
}
