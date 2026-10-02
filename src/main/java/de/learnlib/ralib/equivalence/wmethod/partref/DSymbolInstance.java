package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class DSymbolInstance {

    private final ParameterizedSymbol baseSymbol;

    private final SymbolicDataVariable[] symbolicValues;

    public DSymbolInstance(ParameterizedSymbol baseSymbol, SymbolicDataVariable ... symbolicValues) {
        this.baseSymbol = baseSymbol;
        this.symbolicValues = symbolicValues;
    }

    public ParameterizedSymbol getBaseSymbol() {
        return baseSymbol;
    }

    public SymbolicDataVariable[] getSymbolicValues() {
        return symbolicValues;
    }

    public Mapping<Parameter, SymbolicDataVariable> getParamValuation() {
        Mapping<Parameter, SymbolicDataVariable> pars = new Mapping<>();
        ParameterGenerator pgen = new ParameterGenerator();
        for (SymbolicDataVariable sdv : getSymbolicValues()) {
            pars.put(pgen.next(sdv.getDataType()), sdv);
        }
        return pars;
    }

    public int maxIndex() {
        int max = 0;
        for (SymbolicDataVariable sdv : getSymbolicValues()) {
            if (sdv.getId() > max) {
                max = sdv.getId();
            }
        }
        return max;
    }

    @Override
    public String toString() {
        return this.baseSymbol.getName() + Arrays.toString(symbolicValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DSymbolInstance other = (DSymbolInstance) obj;
        if (!Objects.equals(this.baseSymbol, other.baseSymbol)) {
            return false;
        }
        return Arrays.deepEquals(this.symbolicValues, other.symbolicValues);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + (this.baseSymbol != null ? this.baseSymbol.hashCode() : 0);
        hash = 11 * hash + Arrays.deepHashCode(this.symbolicValues);
        return hash;
    }

    /**
     * Returns the number of symbolic data values in a sequence of symbols
     *
     * @param word
     * @return
     */
    public static int paramValLength(Word<DSymbolInstance> word) {
        int length = 0;
        for (DSymbolInstance dsi : word) {
            length += dsi.getSymbolicValues().length;
        }
        return length;
    }

    /**
     * returns sequence of actions in a symbolic data word.
     * @param word
     * @return
     */
    public static Word<ParameterizedSymbol> actsOf(
            Word<DSymbolInstance> word) {
        ParameterizedSymbol[] symbols = new ParameterizedSymbol[word.length()];
        int idx = 0;
        for (DSymbolInstance dsi : word) {
            symbols[idx++] = dsi.getBaseSymbol();
        }
        return Word.fromSymbols(symbols);
    }

    /**
     * returns sequence of all symbolic data values in a symbolic data word.
     *
     * @param word
     * @return
     */
    public static SymbolicDataVariable[] valsOf(Word<DSymbolInstance> word) {
        SymbolicDataVariable[] vals = new SymbolicDataVariable[paramValLength(word)];
        int i = 0;
        for (DSymbolInstance dsi : word) {
            for (SymbolicDataVariable p : dsi.getSymbolicValues()) {
                vals[i++] = p;
            }
        }
        return vals;
    }

    /**
     * returns set of all unique data values in a data word.
     *
     * @param word
     * @return
     */
    public static Set<SymbolicDataVariable> valSet(Word<DSymbolInstance> word) {
        Set<SymbolicDataVariable> valset = new LinkedHashSet<>();
        for (DSymbolInstance dsi : word) {
            valset.addAll(Arrays.asList(dsi.getSymbolicValues()));
        }
        return valset;
    }

    /**
     * returns set of unique data values of some type in a data word.
     *
     * @param word
     * @param t
     * @return
     */
    public static  Set<SymbolicDataVariable> valSet(Word<DSymbolInstance> word, DataType t) {
        Set<SymbolicDataVariable> vals = new LinkedHashSet<>();
        for (DSymbolInstance dsi : word) {
            for (SymbolicDataVariable d : dsi.getSymbolicValues()) {
                if (d.getDataType().equals(t)) {
                    vals.add(d);
                }
            }
        }
        return vals;
    }

    public static Word<DSymbolInstance> offset(Word<DSymbolInstance> word, int offset) {
        Word<DSymbolInstance> ret = Word.epsilon();
        for (DSymbolInstance dsi : word) {
            SymbolicDataVariable[] vals = dsi.getSymbolicValues();
            SymbolicDataVariable[] newVals = new SymbolicDataVariable[vals.length];
            for (int i = 0; i < vals.length; i++) {
                if (vals[i] instanceof SDV sdv) {
                    newVals[i] = new SDV(sdv.getDataType(), sdv.getId() + offset);
                } else {
                    newVals[i] = vals[i];
                }
            }
            ret = ret.append(new DSymbolInstance(dsi.getBaseSymbol(), newVals));
        }
        return ret;
    }

    public static Word<DSymbolInstance> append(Word<DSymbolInstance> w1, Word<DSymbolInstance> w2) {
        Word<DSymbolInstance> word = w1;
        for (DSymbolInstance dsi : w2) {
            word = word.append(dsi);
        }
        return word;
    }

    public static Word<PSymbolInstance> toDataWord(Word<DSymbolInstance> symbWord, Map<SymbolicDataVariable, DataValue> varmapping) {
        Word<PSymbolInstance> word = Word.epsilon();
        for (DSymbolInstance dsi : symbWord) {
            SymbolicDataVariable[] vars = dsi.getSymbolicValues();
            DataValue[] vals = new DataValue[vars.length];
            for (int i = 0; i < vars.length; i++) {
                vals[i] = varmapping.get(vars[i]);
            }
            PSymbolInstance psi = new PSymbolInstance(dsi.getBaseSymbol(), vals);
            word = word.append(psi);
        }
        return word;
    }
}
