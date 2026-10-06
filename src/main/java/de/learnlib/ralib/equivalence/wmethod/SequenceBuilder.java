package de.learnlib.ralib.equivalence.wmethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class SequenceBuilder {

    public static Word<PSymbolInstance> getDataWord(Word<PSymbolInstance> inputSequence, Word<PSymbolInstance> outputSequence) {
        Word<PSymbolInstance> word = Word.epsilon();
        if (inputSequence.length() != outputSequence.length()) {
            throw new IllegalArgumentException("Input and output sequences do not match");
        }
        Iterator<PSymbolInstance> in = inputSequence.iterator();
        Iterator<PSymbolInstance> out = outputSequence.iterator();
        while (in.hasNext()) {
            word = word.append(in.next()).append(out.next());
        }
        return word;
    }

    public static Word<PSymbolInstance> getInputSequence(Word<PSymbolInstance> word) {
        Word<PSymbolInstance> inputSeq = Word.epsilon();
        for (PSymbolInstance psi : word) {
            if (!(psi.getBaseSymbol() instanceof OutputSymbol)) {
                inputSeq = inputSeq.append(psi);
            }
        }
        return inputSeq;
    }

    public static Word<PSymbolInstance> getOutputSequence(Word<PSymbolInstance> word) {
        Word<PSymbolInstance> outSeq = Word.epsilon();
        for (PSymbolInstance psi : word) {
            if (psi.getBaseSymbol() instanceof OutputSymbol) {
                outSeq = outSeq.append(psi);
            }
        }
        return outSeq;
    }

    public static Word<PSymbolInstance> getOutputSequence(Word<PSymbolInstance> inputSequence, RegisterAutomaton model, Constants consts) {
        Word<PSymbolInstance> outSeq = Word.epsilon();
        RegisterValuation vars = new RegisterValuation();
        RALocation l = model.getInitialState();
        Set<DataValue> vals = new LinkedHashSet<>();

        for (PSymbolInstance psi : inputSequence) {
            vals.addAll(Set.of(psi.getParameterValues()));
            ParameterValuation pars = ParameterValuation.fromPSymbolInstance(psi);
            boolean found = false;
            for (Transition t : l.getOut(psi.getBaseSymbol())) {
                if (t.isEnabled(vars, pars, consts)) {
                    vars = t.valuation(vars, pars, consts);
                    l = t.getDestination();
                    if (l.getOut().size() == 1 &&
                            l.getOut().iterator().next() instanceof OutputTransition) {
                        OutputTransition ot = (OutputTransition) l.getOut().iterator().next();
                        OutputMapping outmap = ot.getOutput();
                        OutputSymbol out = ot.getLabel();
                        DataValue[] outVals = new DataValue[out.getArity()];
                        DataType[] outTypes = out.getPtypes();
                        for (int i = 0; i < out.getArity(); i++) {
                            Parameter p = new Parameter(outTypes[i], i + 1);
                            if (outmap.getFreshParameters().contains(p)) {
                                outVals[i] = getFresh(vals, outTypes[i]);
                            } else {
                                SymbolicDataValue s = outmap.getOutput().get(p);
                                if (s.isConstant()) {
                                    outVals[i] = consts.get((Constant) s);
                                } else if (s.isParameter()) {
                                    outVals[i] = outVals[s.getId() - 1];
                                } else if (s.isRegister()) {
                                    outVals[i] = vars.get((Register) s);
                                } else {
                                    throw new IllegalStateException("Unexpected parameter type");
                                }
                            }
                            vals.add(outVals[i]);
                        }
                        outSeq = outSeq.append(new PSymbolInstance(out, outVals));
                        vars = ot.valuation(vars, pars, consts);
                        l = ot.getDestination();
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("No valid transition found");
            }
        }
        return outSeq;
    }

    private static DataValue getFresh(Collection<DataValue> dataValues, DataType t) {
        if (dataValues.isEmpty()) {
            return new DataValue(t, BigDecimal.ZERO);
        }
        BigDecimal highest = dataValues.iterator().next().getValue();
        for (DataValue dv : dataValues) {
            if (dv.getDataType().equals(t)) {
                BigDecimal dvbd = dv.getValue();
                highest = highest.compareTo(dvbd) > 0 ? highest : dvbd;
            }
        }
        return new DataValue(t, highest.add(BigDecimal.ONE));
    }

    public static Set<Word<PSymbolInstance>> buildInfixSet(Word<PSymbolInstance> prefix, int size, ParameterizedSymbol ... acts) {
        if (acts.length == 0) {
            return Set.of(Word.epsilon());
        }
        Set<DataValue> vals = DataWords.valSet(prefix);
        return appendInfixSymbols(size, Set.of(Word.epsilon()), vals, acts);
    }

    private static Set<Word<PSymbolInstance>> appendInfixSymbols(int size, Set<Word<PSymbolInstance>> infixes, Set<DataValue> prefixVals, ParameterizedSymbol[] acts) {
        if (size <= 0) {
            return infixes;
        }
        Set<Word<PSymbolInstance>> appended = new LinkedHashSet<>();
        for (Word<PSymbolInstance> w : infixes) {
            Set<DataValue> vals = new LinkedHashSet<>(prefixVals);
            vals.addAll(DataWords.valSet(w));
            for (ParameterizedSymbol act : acts) {
                if (act.getArity() == 0) {
                    appended.add(w.append(new PSymbolInstance(act)));
                } else {
                    for (PSymbolInstance psi : buildSymbols(0, new ArrayList<>(), vals, act)) {
                        appended.add(w.append(psi));
                    }
                }
            }
        }
        return appendInfixSymbols(size - 1, appended, prefixVals, acts);
    }

    private static Set<PSymbolInstance> buildSymbols(int param, List<DataValue> params, Set<DataValue> vals, ParameterizedSymbol act) {
        if (param >= act.getArity()) {
            return Set.of(new PSymbolInstance(act, params.toArray(new DataValue[act.getArity()])));
        }
        Set<PSymbolInstance> ret = new LinkedHashSet<>();
        Set<DataValue> valsWithFresh = new LinkedHashSet<>(vals);
        valsWithFresh.add(getFresh(vals, act.getPtypes()[param]));
        for (DataValue d : valsWithFresh) {
            List<DataValue> nextParams = new ArrayList<>(params);
            nextParams.add(d);
            ret.addAll(buildSymbols(param + 1, nextParams, valsWithFresh, act));
        }
        return ret;
    }

}
