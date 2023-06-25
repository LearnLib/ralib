package de.learnlib.ralib.tools.theories;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.equality.UniqueEqualityTheory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UniqueIntegerEqualityTheory extends UniqueEqualityTheory<Integer> implements TypedTheory<Integer> {


    private DataType type = null;

    public UniqueIntegerEqualityTheory() {
    }

    public UniqueIntegerEqualityTheory(DataType t) {
        this.type = t;
    }

    @Override
    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
        int dv = -1;
        for (DataValue<Integer> d : vals) {
            dv = Math.max(dv, d.getId());
        }

        return new DataValue(type, dv + 1);
    }

    @Override
    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setUseSuffixOpt(boolean useit) {
        this.useNonFreeOptimization = useit;
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit, IOOracle oracle) {
        super.setFreshValues(doit, oracle);
    }

    @Override
    public Collection<DataValue<Integer>> getAllNextValues(
            List<DataValue<Integer>> vals) {

        // only fresh value is next value ...
        ArrayList<DataValue<Integer>> ret = new ArrayList<>();
        ret.add(getFreshValue(vals));
        return ret;
    }
}
