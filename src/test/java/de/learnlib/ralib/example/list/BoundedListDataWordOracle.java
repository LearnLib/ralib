package de.learnlib.ralib.example.list;

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
    public static final DataType INT_TYPE= new DataType("int", Integer.class);

    public static final InputSymbol PUSH = new InputSymbol("push", new DataType[]{INT_TYPE});
    public static final InputSymbol INSERT = new InputSymbol("insert", new DataType[]{INT_TYPE, INT_TYPE});
    public static final InputSymbol POP = new InputSymbol("pop", new DataType[]{INT_TYPE});
    public static final InputSymbol CONTAINS = new InputSymbol("contains", new DataType[]{INT_TYPE});


    private Supplier<BoundedList> factory;

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
            list.push( (Integer) symInst.getParameterValues()[0].getId());
            return true;
        } else if (symInst.getBaseSymbol().equals(POP)) {
            Integer value = list.pop();
            return symInst.getParameterValues()[0].getId().equals(value);
        } else if (symInst.getBaseSymbol().equals(INSERT)) {
            list.insert((Integer) symInst.getParameterValues()[0].getId(), (Integer) symInst.getParameterValues()[1].getId());
            return true;
        } else if (symInst.getBaseSymbol().equals(CONTAINS)) {
            return list.contains((Integer) symInst.getParameterValues()[0].getId());
        }
        return false;
    }

    public static DataValue<Integer> dv(int val) {
        return new DataValue<Integer>(INT_TYPE, val);
    }
}
