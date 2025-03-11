package de.learnlib.ralib.theory.equality;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public abstract class UniqueEqualityTheory implements Theory {

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

    public List<DataValue> getPotential(List<DataValue> vals) {
        return vals;
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, PIV pir,
                         Constants constants, SuffixValuation suffixValues, SDTConstructor oracle) {

        int pId = values.size() + 1;

        SymbolicDataValue.SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getDataType();

        Collection<DataValue> potSet = DataWords.joinValsToSet(constants.values(type),
                DataWords.valSet(prefix, type), suffixValues.values(type));

        List<DataValue> potList = new ArrayList<>(potSet);
        List<DataValue> potential = getPotential(potList);

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
        LOGGER.trace(Category.QUERY, " single deq SDT : {}", sdt.toString());

        merged.put(new SDTGuard.SDTTrueGuard(sv), sdt);

        SDT returnSDT = new SDT(merged);
        return returnSDT;
    }

    @Override
    // instantiate a parameter with a data value
    public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParameterValuation pval,
                                 Constants constants, SDTGuard guard, SymbolicDataValue.Parameter param, Set<DataValue> oldDvs) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        LOGGER.trace(Category.QUERY, "prefix values : {}", prefixValues);
        DataType type = param.getDataType();
        Collection potSet = DataWords.joinValsToSet(constants.values(type), DataWords.valSet(prefix, type),
                pval.values(type));

        if (!potSet.isEmpty()) {
            LOGGER.trace(Category.DATASTRUCTURE, "potSet = {}", potSet);
        } else {
            LOGGER.trace(Category.DATASTRUCTURE, "potSet is empty");
        }
        DataValue fresh = this.getFreshValue(new ArrayList<DataValue>(potSet));
        LOGGER.trace(Category.DATASTRUCTURE, "fresh = {}", fresh.toString());
        return fresh;

    }
}
