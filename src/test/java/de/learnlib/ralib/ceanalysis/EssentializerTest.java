package de.learnlib.ralib.ceanalysis;

import de.learnlib.api.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

public class EssentializerTest {

    @Test
    public void testEssentializerLogin() {

        final DataType _t = new DataType("T", Integer.class);
        final InputSymbol act = new InputSymbol("act", _t);

        final EqualityTheory<Integer> theory = new EqualityTheory<Integer>() {
            @Override
            public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
                int max = 0;
                for (DataValue<Integer> i : vals) {
                    max = Math.max(max, i.getId());
                }
                return new DataValue<Integer>(_t, max +1);
            }

            @Override
            public Collection<DataValue<Integer>> getAllNextValues(List<DataValue<Integer>> vals) {
                throw new RuntimeException("not implemented for test.");
            }
        };

        final DataWordOracle oracle = new DataWordOracle() {
            @Override
            public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> collection) {
                for (Query<PSymbolInstance, Boolean> q : collection) {
                    DataValue[] vals = DataWords.valsOf(q.getInput());
                    System.out.println("MQ: " + q.getInput());
                    q.answer(vals.length == 4 && vals[0].equals(vals[2]) && vals[1].equals(vals[3]));
                }
            }
        };

        Essentializer e = new Essentializer(theory, oracle);

        DataValue dv1 = new DataValue<Integer>(_t, 1);
        DataValue dv2 = new DataValue<Integer>(_t, 2);
        DataValue dv3 = new DataValue<Integer>(_t, 3);
        DataValue dv4 = new DataValue<Integer>(_t, 4);

        Word<PSymbolInstance> w1 = DataWords.instantiate(
                Word.fromSymbols(act, act, act, act),
                new DataValue[] {dv1, dv1, dv1, dv1});

        Word<PSymbolInstance> w2 = DataWords.instantiate(
                Word.fromSymbols(act, act, act, act),
                new DataValue[] {dv1, dv2, dv1, dv2});

        Word<PSymbolInstance> w3 = DataWords.instantiate(
                Word.fromSymbols(act, act, act, act),
                new DataValue[] {dv1, dv1, dv1, dv2});

        Word<PSymbolInstance> w4 = DataWords.instantiate(
                Word.fromSymbols(act, act, act, act),
                new DataValue[] {dv1, dv4, dv3, dv2});

        Word<PSymbolInstance> _w1 = e.essentialEq(w1);
        assert _w1.equals(w2);

        Word<PSymbolInstance> _w2 = e.essentialEq(w2);
        assert _w2.equals(w2);

        Word<PSymbolInstance> _w3 = e.essentialEq(w3);
        assert _w3.equals(w4);
    }


}
