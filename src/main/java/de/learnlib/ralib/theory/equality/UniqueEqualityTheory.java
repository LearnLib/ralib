package de.learnlib.ralib.theory.equality;

import java.util.*;

import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.ParValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.SuffixValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.symbol.ParameterizedSymbol;
import net.automatalib.word.Word;

public abstract class UniqueEqualityTheory<T> implements Theory<T> {

    // protected boolean useNonFreeOptimization;

    // protected boolean freshValues = false;

    // protected IOOracle ioOracle;

    private static final Logger LOGGER = LoggerFactory.getLogger(EqualityTheory.class);

    public UniqueEqualityTheory(boolean useNonFreeOptimization) {
        // this.useNonFreeOptimization = useNonFreeOptimization;
    }

    public void setFreshValues(boolean freshValues, IOOracle ioOracle) {
        // this.ioOracle = ioOracle;
        // this.freshValues = freshValues;
    }

    public UniqueEqualityTheory() {
        this(false);
    }

    public List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
        return vals;
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, PIV pir,
                         Constants constants, SuffixValue<T> sv, SuffixValuation suffixValues, SDTConstructor oracle) {

        int pId = values.size() + 1;

        DataType<T> type = sv.getType();

        Collection<DataValue<T>> potSet = DataWords.joinValsToSet(constants.values(type), DataWords.valSet(prefix, type), suffixValues.values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);

        SDT sdt;
        Map<SDTGuard, SDT> merged = new HashMap<>();

        DataValue<T> d = getFreshValue(potential);
        values.put(pId, d);
        WordValuation trueValues = new WordValuation();
        trueValues.putAll(values);
        SuffixValuation trueSuffixValues = new SuffixValuation();
        trueSuffixValues.putAll(suffixValues);
        trueSuffixValues.put(sv, d);
        sdt = oracle.treeQuery(prefix, suffix, trueValues, pir, constants, trueSuffixValues);
        LOGGER.trace(Category.QUERY, " single deq SDT : {}", sdt.toString());

        merged.put(new SDTTrueGuard(sv), sdt);

        SDT returnSDT = new SDT(merged);
        return returnSDT;
    }

    @Override
    // instantiate a parameter with a data value
    public DataValue<T> instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
                                 Constants constants, SDTGuard guard, Parameter<T> param, Set<DataValue<T>> oldDvs) {

        List<DataValue<?>> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        LOGGER.trace(Category.QUERY, "prefix values : {}", prefixValues.toString());
        DataType<T> type = param.getType();
        Collection<DataValue<T>> potSet = DataWords.joinValsToSet(constants.values(type), DataWords.valSet(prefix, type), pval.values(type));

        if (!potSet.isEmpty()) {
            LOGGER.trace(Category.DATASTRUCTURE, "potSet = {}", potSet.toString());
        } else {
            LOGGER.trace(Category.DATASTRUCTURE, "potSet is empty");
        }
        DataValue<T> fresh = this.getFreshValue(new ArrayList<>(potSet));
        LOGGER.trace(Category.DATASTRUCTURE, "fresh = {}", fresh.toString());
        return fresh;

    }
}
