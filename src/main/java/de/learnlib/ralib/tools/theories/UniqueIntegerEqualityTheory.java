package de.learnlib.ralib.tools.theories;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.UniqueEqualityTheory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class UniqueIntegerEqualityTheory extends UniqueEqualityTheory implements TypedTheory {

    private DataType type = null;

    public UniqueIntegerEqualityTheory() {
    }

    public UniqueIntegerEqualityTheory(DataType t) {
        this.type = t;
    }

    @Override
    public DataValue getFreshValue(List<DataValue> vals) {
        BigDecimal dv = new BigDecimal("-1");
        for (DataValue d : vals) {
            dv = dv.max(d.getValue());
        }
        return new DataValue(type, BigDecimal.ONE.add(dv));
    }

    @Override
    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setUseSuffixOpt(boolean unused) {
        // this.useNonFreeOptimization = unused;
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit, IOOracle oracle) {
        super.setFreshValues(doit, oracle);
    }

    @Override
    public Collection<DataValue> getAllNextValues(List<DataValue> vals) {
        // only fresh value is next value ...
        ArrayList<DataValue> ret = new ArrayList<>();
        ret.add(getFreshValue(vals));
        return ret;
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix,
			Word<PSymbolInstance> suffix, Constants consts) {
        return new UnrestrictedSuffixValue(suffixValue);
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
        return new UnrestrictedSuffixValue(guard.getParameter());
    }

    @Override
    public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
        // not yet implemented for inequality theory
        return false;
    }
}
