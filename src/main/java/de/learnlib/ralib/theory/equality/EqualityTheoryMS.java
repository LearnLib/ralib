/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.theory.equality;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk and sofia
 * @param <T>
 */
public abstract class EqualityTheoryMS<T> implements Theory<T> {

    protected boolean useNonFreeOptimization;

    protected boolean freshValues = false;

    protected IOOracle ioOracle;

    private static final LearnLogger log
            = LearnLogger.getLogger(EqualityTheoryMS.class);

    public EqualityTheoryMS(boolean useNonFreeOptimization) {
        this.useNonFreeOptimization = useNonFreeOptimization;
    }

    public void setFreshValues(boolean freshValues, IOOracle ioOracle) {
        this.ioOracle = ioOracle;
        this.freshValues = freshValues;
    }

    public EqualityTheoryMS() {
        this(false);
    }

    public List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
        return vals;
    }

// given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT.  Base case: always add the 'else' guard first.
    private Map<SDTGuard, SDT>
            mergeGuards(Map<EqualityGuard, SDT> eqs,
                    SDTAndGuard deqGuard, SDT deqSdt) {

        Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
        List<DisequalityGuard> deqList = new ArrayList<>();
        List<EqualityGuard> eqList = new ArrayList<>();
        for (Map.Entry<EqualityGuard, SDT> e : eqs.entrySet()) {
            SDT eqSdt = e.getValue();
            EqualityGuard eqGuard = e.getKey();
            log.log(Level.FINEST, "comparing guards: " + eqGuard.toString()
                    + " to " + deqGuard.toString() + "\nSDT    : "
                    + eqSdt.toString() + "\nto SDT : " + deqSdt.toString());
            List<SDTIfGuard> ds = new ArrayList();
            ds.add(eqGuard);
            log.log(Level.FINEST, "remapping: " + ds.toString());
            if (!(eqSdt.isEquivalentUnder(deqSdt, ds))) {
                log.log(Level.FINEST, "--> not eq.");
                deqList.add(eqGuard.toDeqGuard());
                eqList.add(eqGuard);
            } else {
                log.log(Level.FINEST, "--> equivalent");
            }

        }
        if (eqList.isEmpty()) {
            retMap.put(new SDTTrueGuard(deqGuard.getParameter()), deqSdt);
        } else if (eqList.size() == 1) {
            EqualityGuard q = eqList.get(0);
            retMap.put(q, eqs.get(q));
            retMap.put(q.toDeqGuard(), deqSdt);
        } else if (eqList.size() > 1) {
            for (EqualityGuard q : eqList) {
                retMap.put(q, eqs.get(q));
            }
            retMap.put(new SDTAndGuard(deqGuard.getParameter(),
                    deqList.toArray(new SDTIfGuard[]{})), deqSdt);
        }
        assert !retMap.isEmpty();

        return retMap;
    }

    // given a set of registers and a set of guards, keep only the registers
    // that are mentioned in any guard
    private PIV keepMem(Map<SDTGuard, SDT> guardMap) {
        PIV ret = new PIV();
        for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
            SDTGuard mg = e.getKey();
            if (mg instanceof EqualityGuard) {
                log.log(Level.FINEST, mg.toString());
                SymbolicDataValue r = ((EqualityGuard) mg).getRegister();
                Parameter p = new Parameter(r.getType(), r.getId());
                if (r instanceof Register) {
                    ret.put(p, (Register) r);
                }
            }
        }
        return ret;
    }

    // process a tree query
    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix,
            WordValuation values,
            PIV pir,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle) {

        int pId = values.size() + 1;

        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();

        SuffixValue currentParam = new SuffixValue(type, pId);

        Map<EqualityGuard, SDT> tempKids = new LinkedHashMap<>();

        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(
                constants.<T>values(type),
                DataWords.<T>valSet(prefix, type),
                suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);

        boolean free = suffix.getFreeValues().contains(sv);
        if (!free && useNonFreeOptimization) {
            DataValue d = suffixValues.get(sv);
            if (d == null) {
                d = getFreshValue(potential);
            }
            values.put(pId, d);
            WordValuation trueValues = new WordValuation();
            trueValues.putAll(values);
            SuffixValuation trueSuffixValues = new SuffixValuation();
            trueSuffixValues.putAll(suffixValues);
            trueSuffixValues.put(sv, d);
            SDT sdt = oracle.treeQuery(
                    prefix, suffix, trueValues,
                    pir, constants, trueSuffixValues);

            log.log(Level.FINEST, " single deq SDT : " + sdt.toString());

            Map<SDTGuard, SDT> merged = mergeGuards(tempKids,
                    new SDTAndGuard(currentParam), sdt);

            log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
            //log.log(Level.FINEST,"temporary pivs = " + tempPiv.keySet());
            log.log(Level.FINEST, "merged guards = " + merged.keySet());
            log.log(Level.FINEST, "merged pivs = " + pir.toString());

            return new SDT(merged);
        }

        // special case: fresh values in outputs
        if (freshValues) {

            ParameterizedSymbol ps = computeSymbol(suffix, pId);

            if (ps instanceof OutputSymbol && ps.getArity() > 0) {

                int idx = computeLocalIndex(suffix, pId);
                Word<PSymbolInstance> query = buildQuery(
                        prefix, suffix, values);
                Word<PSymbolInstance> trace = ioOracle.trace(query);
                PSymbolInstance out = trace.lastSymbol();

                if (out.getBaseSymbol().equals(ps)) {

                    DataValue d = out.getParameterValues()[idx];

                    if (d instanceof FreshValue) {
                        d = getFreshValue(potential);
                        values.put(pId, d);
                        WordValuation trueValues = new WordValuation();
                        trueValues.putAll(values);
                        SuffixValuation trueSuffixValues
                                = new SuffixValuation();
                        trueSuffixValues.putAll(suffixValues);
                        trueSuffixValues.put(sv, d);
                        SDT sdt = oracle.treeQuery(
                                prefix, suffix, trueValues,
                                pir, constants, trueSuffixValues);

                        log.log(Level.FINEST,
                                " single deq SDT : " + sdt.toString());

                        Map<SDTGuard, SDT> merged = mergeGuards(tempKids,
                                new SDTAndGuard(currentParam), sdt);

                        log.log(Level.FINEST,
                                "temporary guards = " + tempKids.keySet());
                        log.log(Level.FINEST,
                                "merged guards = " + merged.keySet());
                        log.log(Level.FINEST,
                                "merged pivs = " + pir.toString());

                        return new SDT(merged);
                    }
                }
            }
        }

        log.log(Level.FINEST, "potential " + potential.toString());

        // process each 'if' case
        // prepare by picking up the prefix values
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        log.log(Level.FINEST, "prefix list    " + prefixValues.toString());

        DataValue fresh = getFreshValue(potential);

        List<DisequalityGuard> diseqList = new ArrayList<DisequalityGuard>();
        for (DataValue<T> newDv : potential) {
            log.log(Level.FINEST, newDv.toString());

            // this is the valuation of the suffixvalues in the suffix
            SuffixValuation ifSuffixValues = new SuffixValuation();
            ifSuffixValues.putAll(suffixValues);  // copy the suffix valuation

            EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues,
                    currentParam, values, constants);
            log.log(Level.FINEST, "eqGuard is: " + eqGuard.toString());
            diseqList.add(new DisequalityGuard(
                    currentParam, eqGuard.getRegister()));
            //construct the equality guard
            // find the data value in the prefix
            // this is the valuation of the positions in the suffix
            WordValuation ifValues = new WordValuation();
            ifValues.putAll(values);
            ifValues.put(pId, newDv);
            SDT eqOracleSdt = oracle.treeQuery(
                    prefix, suffix, ifValues, pir, constants, ifSuffixValues);

            tempKids.put(eqGuard, eqOracleSdt);
        }

        // process the 'else' case
        // this is the valuation of the positions in the suffix
        WordValuation elseValues = new WordValuation();
        elseValues.putAll(values);
        elseValues.put(pId, fresh);

        // this is the valuation of the suffixvalues in the suffix
        SuffixValuation elseSuffixValues = new SuffixValuation();
        elseSuffixValues.putAll(suffixValues);
        elseSuffixValues.put(sv, fresh);

        SDT elseOracleSdt = oracle.treeQuery(
                prefix, suffix, elseValues, pir, constants, elseSuffixValues);

        SDTAndGuard deqGuard = new SDTAndGuard(currentParam,
                (diseqList.toArray(new DisequalityGuard[]{})));
        log.log(Level.FINEST, "diseq guard = " + deqGuard.toString());

        // merge the guards
        Map<SDTGuard, SDT> merged = mergeGuards(
                tempKids, deqGuard, elseOracleSdt);

        // only keep registers that are referenced by the merged guards
        pir.putAll(keepMem(merged));

        log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
        log.log(Level.FINEST, "merged guards = " + merged.keySet());
        log.log(Level.FINEST, "merged pivs = " + pir.toString());

        // clear the temporary map of children
        tempKids.clear();

        for (SDTGuard g : merged.keySet()) {
            assert !(g == null);
        }

        SDT returnSDT = new SDT(merged);
        return returnSDT;

    }

    // construct equality guard by picking up a data value from the prefix
    private EqualityGuard pickupDataValue(DataValue<T> newDv,
            List<DataValue> prefixValues, SuffixValue currentParam,
            WordValuation ifValues, Constants constants) {
        DataType type = currentParam.getType();
        int newDv_i;
        for (Constant c : constants.keySet()) {
            if (constants.get(c).equals(newDv)) {
                return new EqualityGuard(currentParam, c);
            }
        }
        if (prefixValues.contains(newDv)) {
            // first index of the data value in the prefixvalues list
            newDv_i = prefixValues.indexOf(newDv) + 1;
            Register newDv_r = new Register(type, newDv_i);
            log.log(Level.FINEST, "current param = " + currentParam.toString());
            log.log(Level.FINEST, "New register = " + newDv_r.toString());
            return new EqualityGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix, 
        // it is somewhere earlier in the suffix
        else {

            int smallest = Collections.min(ifValues.getAllKeys(newDv));
            return new EqualityGuard(
                    currentParam, new SuffixValue(type, smallest));
        }
    }

    @Override
    // instantiate a parameter with a data value
    public DataValue instantiate(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv,
            ParValuation pval,
            Constants constants,
            SDTGuard guard,
            Parameter param
    ) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        log.log(Level.FINEST, "prefix values : " + prefixValues.toString());
        DataType type = param.getType();

        if (guard instanceof EqualityGuard) {
            log.log(Level.FINEST, "equality guard " + guard.toString());
            EqualityGuard eqGuard = (EqualityGuard) guard;
            SymbolicDataValue ereg = eqGuard.getRegister();
            if (ereg.isRegister()) {
                log.log(Level.FINEST, "piv: " + piv.toString()
                        + " " + ereg.toString() + " " + param.toString());
                Parameter p = piv.getOneKey((Register) ereg);
                log.log(Level.FINEST, "p: " + p.toString());
                int idx = p.getId();
                return prefixValues.get(idx - 1);
            } else if (ereg.isSuffixValue()) {
                Parameter p = new Parameter(type, ereg.getId());
                return pval.get(p);
            } else if (ereg.isConstant()) {
                return constants.get((Constant) ereg);
            }
        }

        Collection potSet = DataWords.<T>joinValsToSet(
                constants.<T>values(type),
                DataWords.<T>valSet(prefix, type),
                pval.<T>values(type));

        if (!potSet.isEmpty()) {
            log.log(Level.FINEST, "potSet = " + potSet.toString());
        } else {
            log.log(Level.FINEST, "potSet is empty");
        }
        DataValue fresh = this.getFreshValue(
                new ArrayList<DataValue<T>>(potSet));
        log.log(Level.FINEST, "fresh = " + fresh.toString());
        return fresh;

    }

    private ParameterizedSymbol computeSymbol(SymbolicSuffix suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return a;
            }
        }
        return suffix.getActions().size() > 0
                ? suffix.getActions().firstSymbol() : null;
    }

    private int computeLocalIndex(SymbolicSuffix suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return pId - (idx - a.getArity()) - 1;
            }
        }
        return pId - 1;
    }

    private Word<PSymbolInstance> buildQuery(Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix, WordValuation values) {

        Word<PSymbolInstance> query = prefix;
        int base = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            if (base + a.getArity() > values.size()) {
                break;
            }
            DataValue[] vals = new DataValue[a.getArity()];
            for (int i = 0; i < a.getArity(); i++) {
                vals[i] = values.get(base + i + 1);
            }
            query = query.append(new PSymbolInstance(a, vals));
            base += a.getArity();
        }
        return query;
    }

}
