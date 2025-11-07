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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.*;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTLeaf;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

/**
 * @author falk and sofia
 */
public abstract class EqualityTheory implements Theory {

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

    public List<DataValue> getPotential(List<DataValue> vals) {
        return vals;
    }

    // given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT. Base case: always add the 'else' guard first.
    private Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard.EqualityGuard, SDT> eqs, SDTGuard.SDTAndGuard deqGuard, SDT deqSdt) {
        Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
        List<SDTGuard> deqList = new ArrayList<>();
        List<SDTGuard.EqualityGuard> eqList = new ArrayList<>();
        for (Map.Entry<SDTGuard.EqualityGuard, SDT> e : eqs.entrySet()) {
            SDT eqSdt = e.getValue();
            SDTGuard.EqualityGuard eqGuard = e.getKey();
            LOGGER.trace("comparing guards: " + eqGuard.toString() + " to " + deqGuard.toString()
                    + "\nSDT    : " + eqSdt.toString() + "\nto SDT : " + deqSdt.toString());
            List<SDTGuard.EqualityGuard> ds = new ArrayList<>();
            ds.add(eqGuard);
            LOGGER.trace("remapping: " + ds);
            if (!(eqSdt.isEquivalentUnder(deqSdt, ds))) {
                LOGGER.trace("--> not eq.");
                deqList.add(SDTGuard.toDeqGuard(eqGuard));
                eqList.add(eqGuard);
            } else {
                LOGGER.trace("--> equivalent");
            }

        }
        if (eqList.isEmpty()) {
            retMap.put(new SDTGuard.SDTTrueGuard(deqGuard.getParameter()), deqSdt);
        } else if (eqList.size() == 1) {
            SDTGuard.EqualityGuard q = eqList.get(0);
            retMap.put(q, eqs.get(q));
            retMap.put(SDTGuard.toDeqGuard(q), deqSdt);
        } else if (eqList.size() > 1) {
            for (SDTGuard.EqualityGuard q : eqList) {
                retMap.put(q, eqs.get(q));
            }
            retMap.put(new SDTGuard.SDTAndGuard(deqGuard.getParameter(), deqList), deqSdt);
        }
        assert !retMap.isEmpty();

        return retMap;
    }

    // process a tree query
    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values,
            Constants constants, SuffixValuation suffixValues, MultiTheoryTreeOracle oracle) {

        int pId = values.size() + 1;

        SuffixValue currentParam = suffix.getSuffixValue(pId);
        DataType type = currentParam.getDataType();

        Map<SDTGuard.EqualityGuard, SDT> tempKids = new LinkedHashMap<>();

        Collection<DataValue> potSet = DataWords.joinValsToSet(constants.values(type),
                DataWords.valSet(prefix, type), suffixValues.values(type));

        List<DataValue> potList = new ArrayList<>(potSet);
        List<DataValue> potential = getPotential(potList);

        DataValue fresh = getFreshValue(potential);

        List<DataValue> equivClasses = new ArrayList<>(potSet);
        equivClasses.add(fresh);
        //System.out.println(" prefix: " + prefix);
        //System.out.println(" potential: " + potential);
        //System.out.println(" eqs " + Arrays.toString(equivClasses.toArray()));
        EquivalenceClassFilter eqcFilter = new EquivalenceClassFilter(equivClasses, useNonFreeOptimization);
        List<DataValue> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(currentParam), prefix, suffix.getActions(), values);
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
                        SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, constants, trueSuffixValues);

                        LOGGER.trace(" single deq SDT : " + sdt.toString());

                        Map<SDTGuard, SDT> merged = mergeGuards(tempKids, new SDTGuard.SDTAndGuard(currentParam, List.of()), sdt);

                        LOGGER.trace("temporary guards = " + tempKids.keySet());
                        LOGGER.trace("merged guards = " + merged.keySet());

                        return new SDT(merged);
                    }
                } else {
                    int maxSufIndex = DataWords.paramLength(suffix.getActions()) + 1;
                    SDT rejSdt = makeRejectingBranch(currentParam.getId() + 1, maxSufIndex, type);
                    SDTGuard.SDTTrueGuard trueGuard = new SDTGuard.SDTTrueGuard(currentParam);
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

        LOGGER.trace("prefix list    " + prefixValues);

        List<SDTGuard> diseqList = new ArrayList<>();
        for (DataValue newDv : potential) {
        	if (filteredEquivClasses.contains(newDv)) {
	            LOGGER.trace(newDv.toString());

	            // this is the valuation of the suffixvalues in the suffix
	            SuffixValuation ifSuffixValues = new SuffixValuation();
	            ifSuffixValues.putAll(suffixValues); // copy the suffix valuation

	            SDTGuard.EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, constants);
	            LOGGER.trace("eqGuard is: " + eqGuard);
	            diseqList.add(new SDTGuard.DisequalityGuard(currentParam, eqGuard.register()));
	            // construct the equality guard
	            // find the data value in the prefix
	            // this is the valuation of the positions in the suffix
	            WordValuation ifValues = new WordValuation();
	            ifValues.putAll(values);
	            ifValues.put(pId, newDv);
	            SDT eqOracleSdt = oracle.treeQuery(prefix, suffix, ifValues, constants, ifSuffixValues);

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

	        SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, constants, elseSuffixValues);

	        SDTGuard.SDTAndGuard deqGuard = new SDTGuard.SDTAndGuard(currentParam, diseqList);
	        LOGGER.trace("diseq guard = " + deqGuard);

	        // merge the guards
	        merged = mergeGuards(tempKids, deqGuard, elseOracleSdt);
        } else {
        	// if no else case, we can only have a true guard
        	// TODO: add  support for multiple equalities with same outcome
        	assert tempKids.size() == 1;

        	Iterator<Map.Entry<SDTGuard.EqualityGuard, SDT>> it = tempKids.entrySet().iterator();
        	Map.Entry<SDTGuard.EqualityGuard, SDT> e = it.next();
        	merged = new LinkedHashMap<SDTGuard, SDT>();
        	merged.put(e.getKey(), e.getValue());
        }

        // only keep registers that are referenced by the merged guards
        //pir.putAll(keepMem(merged));

        LOGGER.trace("temporary guards = " + tempKids.keySet());
        LOGGER.trace("merged guards = " + merged.keySet());

        // clear the temporary map of children
        tempKids.clear();

        for (SDTGuard g : merged.keySet()) {
            assert !(g == null);
        }

        SDT returnSDT = new SDT(merged);
        return returnSDT;

    }

    // construct equality guard by picking up a data value from the prefix
    private SDTGuard.EqualityGuard pickupDataValue(DataValue newDv, List<DataValue> prefixValues, SuffixValue currentParam,
            WordValuation ifValues, Constants constants) {
        DataType type = currentParam.getDataType();
        int newDv_i;
        for (Map.Entry <Constant, DataValue> entry : constants.entrySet()) {
            if (entry.getValue().equals(newDv)) {
                return new SDTGuard.EqualityGuard(currentParam, entry.getKey());
            }
        }
        if (prefixValues.contains(newDv)) {
            // first index of the data value in the prefixvalues list
            newDv_i = prefixValues.indexOf(newDv) + 1;
            Register newDv_r = new Register(type, newDv_i);
            LOGGER.trace("current param = " + currentParam);
            LOGGER.trace("New register = " + newDv_r);
            return new SDTGuard.EqualityGuard(currentParam, newDv);

        } // if the data value isn't in the prefix,
            // it is somewhere earlier in the suffix
        else {

            int smallest = Collections.min(ifValues.getAllKeysForValue(newDv));
            return new SDTGuard.EqualityGuard(currentParam, new SuffixValue(type, smallest));
        }
    }

    @Override
    // instantiate a parameter with a data value
    public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, SuffixValuation pval,
            Constants constants, SDTGuard guard, SuffixValue param, Set<DataValue> oldDvs) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        LOGGER.trace("prefix values : " + prefixValues);
        DataType type = param.getDataType();
        Deque<SDTGuard> guards = new LinkedList<>();
        guards.add(guard);

        while(!guards.isEmpty()) {
            SDTGuard current = guards.remove();
            if (current instanceof SDTGuard.EqualityGuard eqGuard) {
                LOGGER.trace("equality guard " + current);
                SDTGuardElement ereg = eqGuard.register();
                if (SDTGuardElement.isDataValue(ereg)) {

                    Parameter p = new Parameter(ereg.getDataType(), prefixValues.indexOf( (DataValue) ereg)+1);
                    LOGGER.trace("p: " + p.toString());
                    int idx = p.getId();
                    return prefixValues.get(idx - 1);
                } else if (SDTGuardElement.isSuffixValue(ereg)) {
                    return pval.get( (SuffixValue) ereg);
                } else if (SDTGuardElement.isConstant(ereg)) {
                    return constants.get((Constant) ereg);
                }
            } else if (current instanceof SDTGuard.SDTAndGuard) {
                guards.addAll(((SDTGuard.SDTAndGuard) current).conjuncts());
            }
            // todo: this only works under the assumption that disjunctions only contain disequality guards
        }

        Collection<DataValue> potSet = DataWords.joinValsToSet(constants.values(type), DataWords.valSet(prefix, type),
                pval.values(type));

        if (!potSet.isEmpty()) {
            LOGGER.trace("potSet = " + potSet);
        } else {
            LOGGER.trace("potSet is empty");
        }
        DataValue fresh = this.getFreshValue(new ArrayList<DataValue>(potSet));
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
            SDTGuard.SDTTrueGuard trueGuard = new SDTGuard.SDTTrueGuard(new SuffixValue(type, nextSufIndex));
            map.put(trueGuard, makeRejectingBranch(nextSufIndex + 1, maxIndex, type));
            SDT sdt = new SDT(map);
            return sdt;
        }
    }

    @Override
    public DataValue instantiate(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, Set<DataValue> pval,
            Constants constants,
            Expression<Boolean> guard, int param,
            ConstraintSolver solver) {
    	Parameter p = new Parameter(ps.getPtypes()[param-1], param);
    	Set<DataValue> vals = DataWords.valSet(prefix, p.getDataType());
    	vals.addAll(vals.stream()
    			.filter(v -> v.getDataType().equals(p.getDataType()))
    			.collect(Collectors.toSet()));
    	vals.addAll(constants.values());
    	DataValue fresh = getFreshValue(new LinkedList<>(vals));

    	if (tryEquality(guard, p, fresh, solver, constants)) {
    		return fresh;
    	}

    	for (DataValue val : vals) {
    		if (tryEquality(guard, p, val, solver, constants)) {
    			return val;
    		}
    	}

    	throw new IllegalArgumentException("Guard is not equality/disequality: " + guard);
    }

    private boolean tryEquality(Expression<Boolean> guard, Parameter p, DataValue val, ConstraintSolver solver, Constants consts) {
    	Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<>();
    	valuation.put(p, val);
    	valuation.putAll(consts);
    	return solver.isSatisfiable(guard, valuation);
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

    @Override
    public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
    	if (guard instanceof SDTGuard.EqualityGuard && ((SDTGuard.EqualityGuard) guard).register().equals(register)) {
    		return true;
    	} else if (guard instanceof SDTGuard.DisequalityGuard && ((SDTGuard.DisequalityGuard)guard).register().equals(register)) {
    		return true;
    	} else if (guard instanceof SDTGuard.SDTAndGuard ag) {
    		boolean revealsGuard = false;
    		for (SDTGuard g : ag.conjuncts()) {
    			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
    		}
    		return revealsGuard;
        } else if (guard instanceof SDTGuard.SDTOrGuard og) {
            boolean revealsGuard = false;
            for (SDTGuard g : og.disjuncts()) {
                revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
            }
            return revealsGuard;
        }    	return false;
    }
}
