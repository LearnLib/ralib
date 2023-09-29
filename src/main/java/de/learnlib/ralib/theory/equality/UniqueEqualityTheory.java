package de.learnlib.ralib.theory.equality;

import java.util.*;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public abstract class UniqueEqualityTheory<T> implements Theory<T> {

    protected boolean useNonFreeOptimization;

    protected boolean freshValues = false;

    protected IOOracle ioOracle;

    private static final LearnLogger log = LearnLogger.getLogger(EqualityTheory.class);

    public UniqueEqualityTheory(boolean useNonFreeOptimization) {
        this.useNonFreeOptimization = useNonFreeOptimization;
    }

    public void setFreshValues(boolean freshValues, IOOracle ioOracle) {
        this.ioOracle = ioOracle;
        this.freshValues = freshValues;
    }

    public UniqueEqualityTheory() {
        this(false);
    }

    public List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
        return vals;
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, PIV pir,
                         Constants constants, SuffixValuation suffixValues, SDTConstructor oracle) {

        int pId = values.size() + 1;

        SymbolicDataValue.SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
                DataWords.<T>valSet(prefix, type), suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);

        SDT sdt;
        Map<SDTGuard, SDT> merged = new HashMap<>();

        DataValue d = getFreshValue(potential);
        values.put(pId, d);
        WordValuation trueValues = new WordValuation();
        trueValues.putAll(values);
        SuffixValuation trueSuffixValues = new SuffixValuation();
        trueSuffixValues.putAll(suffixValues);
        trueSuffixValues.put(sv, d);
        sdt = oracle.treeQuery(prefix, suffix, trueValues, pir, constants, trueSuffixValues);
        log.trace(" single deq SDT : " + sdt.toString());

        merged.put(new SDTTrueGuard(sv), sdt);

        SDT returnSDT = new SDT(merged);
        return returnSDT;
    }

    @Override
    // instantiate a parameter with a data value
    public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
                                 Constants constants, SDTGuard guard, SymbolicDataValue.Parameter param, Set<DataValue<T>> oldDvs) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        log.trace("prefix values : " + prefixValues.toString());
        DataType type = param.getType();
        Collection potSet = DataWords.<T>joinValsToSet(constants.<T>values(type), DataWords.<T>valSet(prefix, type),
                pval.<T>values(type));

        if (!potSet.isEmpty()) {
            log.trace("potSet = " + potSet.toString());
        } else {
            log.trace("potSet is empty");
        }
        DataValue fresh = this.getFreshValue(new ArrayList<DataValue<T>>(potSet));
        log.trace("fresh = " + fresh.toString());
        return fresh;

    }
}
