/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory.equality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.EquivalenceClassFilter;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author falk and sofia
 * @param <T>
 */
public abstract class EqualityTheory<T> implements Theory<T> {

    protected boolean useNonFreeOptimization;

    protected boolean freshValues = false;

    protected IOOracle ioOracle;

    private static final Logger LOGGER = LoggerFactory.getLogger(EqualityTheory.class);

    public EqualityTheory(boolean useNonFreeOptimization) {
        this.useNonFreeOptimization = useNonFreeOptimization;
    }

    public void setFreshValues(boolean freshValues, IOOracle ioOracle) {
        this.ioOracle = ioOracle;
        this.freshValues = freshValues;
    }

    public EqualityTheory() {
        this(false);
    }

    public List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
        return vals;
    }

    // given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT. Base case: always add the 'else' guard first.
    private Map<SDTGuard, SDT> mergeGuards(Map<EqualityGuard, SDT> eqs, SDTAndGuard deqGuard, SDT deqSdt) {

        Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
        List<DisequalityGuard> deqList = new ArrayList<>();
        List<EqualityGuard> eqList = new ArrayList<>();
        for (Map.Entry<EqualityGuard, SDT> e : eqs.entrySet()) {
            SDT eqSdt = e.getValue();
            EqualityGuard eqGuard = e.getKey();
            LOGGER.trace("comparing guards: " + eqGuard.toString() + " to " + deqGuard.toString()
                    + "\nSDT    : " + eqSdt.toString() + "\nto SDT : " + deqSdt.toString());
            List<SDTIfGuard> ds = new ArrayList();
            ds.add(eqGuard);
            LOGGER.trace("remapping: " + ds.toString());
            if (!(eqSdt.isEquivalentUnder(deqSdt, ds))) {
                LOGGER.trace("--> not eq.");
                deqList.add(eqGuard.toDeqGuard());
                eqList.add(eqGuard);
            } else {
                LOGGER.trace("--> equivalent");
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
            retMap.put(new SDTAndGuard(deqGuard.getParameter(), deqList.toArray(new SDTIfGuard[] {})), deqSdt);
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
                LOGGER.trace(mg.toString());
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
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, PIV pir,
            Constants constants, SuffixValuation suffixValues, SDTConstructor oracle) {

        int pId = values.size() + 1;

        SuffixValue currentParam = suffix.getSuffixValue(pId);
        DataType type = currentParam.getType();

        Map<EqualityGuard, SDT> tempKids = new LinkedHashMap<>();

        Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
                DataWords.<T>valSet(prefix, type), suffixValues.<T>values(type));

        List<DataValue<T>> potList = new ArrayList<>(potSet);
        List<DataValue<T>> potential = getPotential(potList);

        DataValue fresh = getFreshValue(potential);

        List<DataValue<T>> equivClasses = new ArrayList<>(potSet);
        equivClasses.add(fresh);
        EquivalenceClassFilter<T> eqcFilter = new EquivalenceClassFilter<T>(equivClasses, useNonFreeOptimization);
        List<DataValue<T>> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(currentParam), prefix, suffix.getActions(), suffixValues, constants);
        assert filteredEquivClasses.size() > 0;

        // TODO: integrate fresh-value optimization with restrictions
        // special case: fresh values in outputs
        if (freshValues) {

            ParameterizedSymbol ps = computeSymbol(suffix, pId);

            if (ps instanceof OutputSymbol && ps.getArity() > 0) {

                int idx = computeLocalIndex(suffix, pId);
                Word<PSymbolInstance> query = buildQuery(prefix, suffix, values);
                Word<PSymbolInstance> trace = ioOracle.trace(query);

                if (!trace.isEmpty() && trace.lastSymbol().getBaseSymbol().equals(ps)) {

                    DataValue d = trace.lastSymbol().getParameterValues()[idx];

                    if (d instanceof FreshValue) {
                        d = getFreshValue(potential);
                        values.put(pId, d);
                        WordValuation trueValues = new WordValuation();
                        trueValues.putAll(values);
                        SuffixValuation trueSuffixValues = new SuffixValuation();
                        trueSuffixValues.putAll(suffixValues);
                        trueSuffixValues.put(currentParam, d);
                        SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, pir, constants, trueSuffixValues);

                        LOGGER.trace(" single deq SDT : " + sdt.toString());

                        Map<SDTGuard, SDT> merged = mergeGuards(tempKids, new SDTAndGuard(currentParam), sdt);

                        LOGGER.trace("temporary guards = " + tempKids.keySet());
                        LOGGER.trace("merged guards = " + merged.keySet());
                        LOGGER.trace("merged pivs = " + pir.toString());

                        return new SDT(merged);
                    }
                } else {
                    int maxSufIndex = DataWords.paramLength(suffix.getActions()) + 1;
                    SDT rejSdt = makeRejectingBranch(currentParam.getId() + 1, maxSufIndex, type);
                    SDTTrueGuard trueGuard = new SDTTrueGuard(currentParam);
                    Map<SDTGuard, SDT> merged = new LinkedHashMap<>();
                    merged.put(trueGuard, rejSdt);
                    return new SDT(merged);
                }
            }
        }

        LOGGER.trace("potential " + potential.toString());

        // process each 'if' case
        // prepare by picking up the prefix values
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        LOGGER.trace("prefix list    " + prefixValues.toString());

        List<DisequalityGuard> diseqList = new ArrayList<DisequalityGuard>();
        for (DataValue<T> newDv : potential) {
        	if (filteredEquivClasses.contains(newDv)) {
	            LOGGER.trace(newDv.toString());

	            // this is the valuation of the suffixvalues in the suffix
	            SuffixValuation ifSuffixValues = new SuffixValuation();
	            ifSuffixValues.putAll(suffixValues); // copy the suffix valuation

	            EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, constants);
	            LOGGER.trace("eqGuard is: " + eqGuard.toString());
	            diseqList.add(new DisequalityGuard(currentParam, eqGuard.getRegister()));
	            // construct the equality guard
	            // find the data value in the prefix
	            // this is the valuation of the positions in the suffix
	            WordValuation ifValues = new WordValuation();
	            ifValues.putAll(values);
	            ifValues.put(pId, newDv);
	            SDT eqOracleSdt = oracle.treeQuery(prefix, suffix, ifValues, pir, constants, ifSuffixValues);

	            tempKids.put(eqGuard, eqOracleSdt);
        	}
        }

        Map<SDTGuard, SDT> merged;

        // process the 'else' case
        if (filteredEquivClasses.contains(fresh)) {
        	// this is the valuation of the positions in the suffix
	        WordValuation elseValues = new WordValuation();
	        elseValues.putAll(values);
	        elseValues.put(pId, fresh);

	        // this is the valuation of the suffixvalues in the suffix
	        SuffixValuation elseSuffixValues = new SuffixValuation();
	        elseSuffixValues.putAll(suffixValues);
	        elseSuffixValues.put(currentParam, fresh);

	        SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, pir, constants, elseSuffixValues);

	        SDTAndGuard deqGuard = new SDTAndGuard(currentParam, (diseqList.toArray(new DisequalityGuard[] {})));
	        LOGGER.trace("diseq guard = " + deqGuard.toString());

	        // merge the guards
	        merged = mergeGuards(tempKids, deqGuard, elseOracleSdt);
        } else {
        	// if no else case, we can only have a true guard
        	// TODO: add  support for multiple equalities with same outcome
        	assert tempKids.size() == 1;

        	Iterator<Map.Entry<EqualityGuard, SDT>> it = tempKids.entrySet().iterator();
        	Map.Entry<EqualityGuard, SDT> e = it.next();
        	merged = new LinkedHashMap<SDTGuard, SDT>();
        	merged.put(e.getKey(), e.getValue());
        }

        // only keep registers that are referenced by the merged guards
        pir.putAll(keepMem(merged));

        LOGGER.trace("temporary guards = " + tempKids.keySet());
        LOGGER.trace("merged guards = " + merged.keySet());
        LOGGER.trace("merged pivs = " + pir.toString());

        // clear the temporary map of children
        tempKids.clear();

        for (SDTGuard g : merged.keySet()) {
            assert !(g == null);
        }

        SDT returnSDT = new SDT(merged);
        return returnSDT;

    }

    // construct equality guard by picking up a data value from the prefix
    private EqualityGuard pickupDataValue(DataValue<T> newDv, List<DataValue> prefixValues, SuffixValue currentParam,
            WordValuation ifValues, Constants constants) {
        DataType type = currentParam.getType();
        int newDv_i;
        for (Map.Entry <Constant, DataValue<?>> entry : constants.entrySet()) {
            if (entry.getValue().equals(newDv)) {
                return new EqualityGuard(currentParam, entry.getKey());
            }
        }
        if (prefixValues.contains(newDv)) {
            // first index of the data value in the prefixvalues list
            newDv_i = prefixValues.indexOf(newDv) + 1;
            Register newDv_r = new Register(type, newDv_i);
            LOGGER.trace("current param = " + currentParam.toString());
            LOGGER.trace("New register = " + newDv_r.toString());
            return new EqualityGuard(currentParam, newDv_r);

        } // if the data value isn't in the prefix,
            // it is somewhere earlier in the suffix
        else {

            int smallest = Collections.min(ifValues.getAllKeys(newDv));
            return new EqualityGuard(currentParam, new SuffixValue(type, smallest));
        }
    }

    @Override
    // instantiate a parameter with a data value
    public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
            Constants constants, SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        LOGGER.trace("prefix values : " + prefixValues.toString());
        DataType type = param.getType();
        Deque<SDTGuard> guards = new LinkedList<>();
        guards.add(guard);

        while(!guards.isEmpty()) {
            SDTGuard current = guards.remove();
            if (current instanceof EqualityGuard) {
                LOGGER.trace("equality guard " + current.toString());
                EqualityGuard eqGuard = (EqualityGuard) current;
                SymbolicDataValue ereg = eqGuard.getRegister();
                if (ereg.isRegister()) {
                    LOGGER.trace("piv: " + piv.toString()
                            + " " + ereg.toString() + " " + param.toString());
                    Parameter p = piv.getOneKey((Register) ereg);
                    LOGGER.trace("p: " + p.toString());
                    int idx = p.getId();
                    return prefixValues.get(idx - 1);
                } else if (ereg.isSuffixValue()) {
                    Parameter p = new Parameter(type, ereg.getId());
                    return pval.get(p);
                } else if (ereg.isConstant()) {
                    return constants.get((Constant) ereg);
                }
            } else if (current instanceof SDTAndGuard) {
                guards.addAll(((SDTAndGuard) current).getGuards());
            }
        }

        Collection potSet = DataWords.<T>joinValsToSet(constants.<T>values(type), DataWords.<T>valSet(prefix, type),
                pval.<T>values(type));

        if (!potSet.isEmpty()) {
            LOGGER.trace("potSet = " + potSet.toString());
        } else {
            LOGGER.trace("potSet is empty");
        }
        DataValue fresh = this.getFreshValue(new ArrayList<DataValue<T>>(potSet));
        LOGGER.trace("fresh = " + fresh.toString());
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
        return suffix.getActions().size() > 0 ? suffix.getActions().firstSymbol() : null;
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

    private Word<PSymbolInstance> buildQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values) {

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

    /*
     * Creates a "unary tree" of depth maxIndex - nextSufIndex which leads to a
     * rejecting Leaf. Edges are of type {@link SDTTrueGuard}. Used to shortcut
     * output processing.
     */
    private SDT makeRejectingBranch(int nextSufIndex, int maxIndex, DataType type) {
        if (nextSufIndex == maxIndex) {
            // map.put(guard, SDTLeaf.REJECTING);
            return SDTLeaf.REJECTING;
        } else {
            Map<SDTGuard, SDT> map = new LinkedHashMap<>();
            SDTTrueGuard trueGuard = new SDTTrueGuard(new SuffixValue(type, nextSufIndex));
            map.put(trueGuard, makeRejectingBranch(nextSufIndex + 1, maxIndex, type));
            SDT sdt = new SDT(map);
            return sdt;
        }
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts) {
    	// for now, use generic restrictions with equality theory
    	return SuffixValueRestriction.genericRestriction(suffixValue, prefix, suffix, consts);
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
    	// for now, use generic restrictions with equality theory
    	return SuffixValueRestriction.genericRestriction(guard, prior);
    }

//    @Override
//    public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
//    	if (guard instanceof EqualityGuard && ((EqualityGuard) guard).getRegister().equals(register)) {
//    		return true;
//    	} else if (guard instanceof DisequalityGuard && ((DisequalityGuard)guard).getRegister().equals(register)) {
//    		return true;
//    	} else if (guard instanceof SDTMultiGuard) {
//    		boolean revealsGuard = false;
//    		for (SDTGuard g : ((SDTMultiGuard)guard).getGuards()) {
//    			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
//    		}
//    		return revealsGuard;
//    	}
//    	return false;
//    }
}
