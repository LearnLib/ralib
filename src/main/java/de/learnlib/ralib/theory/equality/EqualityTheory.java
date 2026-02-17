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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.ralib.ct.Prefix;
import de.learnlib.ralib.data.*;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder.Version;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.*;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 * @author falk and sofia
 */
public abstract class EqualityTheory implements Theory {

//	public boolean useMoreEfficientSuffixOptimization = true;

	protected boolean useSuffixOpt = false;

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

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values,
            Constants consts, SuffixValuation suffixValues, MultiTheoryTreeOracle oracle) {
    	int currentId = values.size() + 1;

    	SuffixValue suffixValue = suffix.getSuffixValue(currentId);

    	Map<DataValue, SDTGuardElement> pot = getPotential(suffixValue.getDataType(), prefix, suffixValues, consts);
    	List<DataValue> potVals = new ArrayList<>();
    	pot.keySet().forEach(d -> potVals.add(d));
    	DataValue fresh = getFreshValue(potVals);

    	List<DataValue> equivClasses = new ArrayList<>(potVals);
    	equivClasses.add(fresh);
		EquivalenceClassFilter eqcFilter = new EquivalenceClassFilter(equivClasses, useSuffixOpt);
		List<DataValue> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(suffixValue), prefix, suffix.getActions(), values, consts);

		if (freshValues) {
			ParameterizedSymbol act = computeSymbol(suffix, currentId);
			if (act.getArity() > 0 && act instanceof OutputSymbol out) {
		        int idx = computeLocalIndex(suffix, currentId);
		        Word<PSymbolInstance> query = buildQuery(prefix, suffix, values);
		        Word<PSymbolInstance> trace = ioOracle.trace(query);

		        if (!trace.isEmpty() && trace.lastSymbol().getBaseSymbol().equals(act)) {
		            DataValue d = trace.lastSymbol().getParameterValues()[idx];
		            if (d instanceof FreshValue) {
		            	filteredEquivClasses = Arrays.asList(fresh);
		            }
		        } else {
		        	Queue<DataType> types = new LinkedList<>();
		        	DataType[] suffixTypes = DataWords.typesOf(suffix.getActions());
		        	for (int i = currentId - 1; i < suffixTypes.length; i++) {
		        		types.offer(suffixTypes[i]);
		        	}
		        	return SDT.makeRejectingSDT(currentId, types);
//		        	return SDT.makeRejectingSDT(suffix);
		        }
			}
		}

		if (!filteredEquivClasses.contains(fresh)) {
			fresh = Collections.max(filteredEquivClasses, (d1,d2) -> d1.compareTo(d2));
		}

    	Map<DataValue, SDT> ifSdts = new LinkedHashMap<>();
    	SDT elseSdt = null;
    	for (DataValue d : filteredEquivClasses) {
    		WordValuation nextValuation = new WordValuation();
    		nextValuation.putAll(values);
    		nextValuation.put(currentId, d);
    		SuffixValuation nextSuffixValuation = new SuffixValuation();
    		nextSuffixValuation.putAll(suffixValues);
    		nextSuffixValuation.put(suffixValue, d);

    		SDT sdt = oracle.treeQuery(prefix, suffix, nextValuation, consts, nextSuffixValuation);

    		if (d.equals(fresh)) {
    			elseSdt = sdt;
    		} else {
    			ifSdts.put(d, sdt);
    		}
    	}

    	Map<SDTGuard.EqualityGuard, SDT> eqChildren = getIfGuards(suffixValue, ifSdts, pot, elseSdt);
    	SDTGuard elseGuard = getElseGuard(suffixValue, eqChildren.keySet());

    	Map<SDTGuard, SDT> children = new LinkedHashMap<>();
    	children.putAll(eqChildren);
    	children.put(elseGuard, elseSdt);
    	return new SDT(children);
    }

    private Map<DataValue, SDTGuardElement> getPotential(DataType type, Word<PSymbolInstance> prefix, SuffixValuation suffixValues, Constants consts) {
    	Map<DataValue, SDTGuardElement> pot = new LinkedHashMap<>();

    	DataValue[] vals = DataWords.valsOf(prefix);
    	for (DataValue val : vals) {
    		if (val.getDataType().equals(type) && !consts.containsValue(val)) {
    			pot.put(val, val);
    		}
    	}

    	for (Map.Entry<SuffixValue, DataValue> e : suffixValues.entrySet()) {
    		DataValue d = e.getValue();
    		if (d != null && d.getDataType().equals(type) && !pot.containsKey(d)) {
    			pot.put(d, e.getKey());
    		}
    	}

    	for (Map.Entry<Constant, DataValue> e : consts.entrySet()) {
    		DataValue d = e.getValue();
    		if (d != null && d.getDataType().equals(type)) {
    			pot.put(d, e.getKey());
    		}
    	}

    	return pot;
    }

    private Map<SDTGuard.EqualityGuard, SDT> getIfGuards(SuffixValue suffixValue, Map<DataValue, SDT> sdts, Map<DataValue, SDTGuardElement> pot, SDT elseSdt) {
    	Map<SDTGuard.EqualityGuard, SDT> ifGuards = new LinkedHashMap<>();
    	for (Map.Entry<DataValue, SDT> e : sdts.entrySet()) {
    		DataValue d = e.getKey();
    		SDT sdt = e.getValue();
			SDTGuard.EqualityGuard eq = new SDTGuard.EqualityGuard(suffixValue, pot.get(d));
			List<SDTGuard.EqualityGuard> eqList = new ArrayList<>();
			eqList.add(eq);
    		if (!sdt.isEquivalentUnder(elseSdt, eqList)) {
//    		if (!SDT.equivalentUnderId(sdt, elseSdt)) {
    			ifGuards.put(eq, sdt);
    		}
    	}
    	return ifGuards;
    }

    private SDTGuard getElseGuard(SuffixValue suffixValue, Set<SDTGuard.EqualityGuard> eqGuards) {
    	if (eqGuards.isEmpty()) {
    		return new SDTGuard.SDTTrueGuard(suffixValue);
    	}
    	if (eqGuards.size() == 1) {
    		SDTGuard.EqualityGuard eq = eqGuards.iterator().next();
    		return new SDTGuard.DisequalityGuard(suffixValue, eq.register());
    	}
    	List<SDTGuard> deqList = new ArrayList<>();
    	eqGuards.forEach(eq -> deqList.add(new SDTGuard.DisequalityGuard(suffixValue, eq.register())));
    	return new SDTGuard.SDTAndGuard(suffixValue, deqList);
    }

    private boolean isFreshDataValue(ParameterizedSymbol act, int pId, Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values) {
        int idx = computeLocalIndex(suffix, pId);
        Word<PSymbolInstance> query = buildQuery(prefix, suffix, values);
        Word<PSymbolInstance> trace = ioOracle.trace(query);

        if (!trace.isEmpty() && trace.lastSymbol().getBaseSymbol().equals(act)) {
            DataValue d = trace.lastSymbol().getParameterValues()[idx];
            return d instanceof FreshValue;
        }
        return false;
    }

//    private Map<DataValue, SDTGuard> generateEquivClasses(SuffixValue suffixValue, Map<DataValue, SDTGuardElement> pot) {
//    	Map<DataValue, SDTGuard> equiv = new LinkedHashMap<>();
//    	for (Map.Entry<DataValue, SDTGuardElement> e : pot.entrySet()) {
//    		SDTGuard g = new SDTGuard.EqualityGuard(suffixValue, e.getValue());
//    	}
//    	return equiv;
//

//    private Map<DataValue, SDTGuard> filterEquivClasses(Map<DataValue, SDTGuard> valueGuards,
//			Word<PSymbolInstance> prefix,
//			SymbolicSuffix suffix,
//			SuffixValue suffixValue,
//			Map<DataValue, SDTGuardElement> potValuation,
//			SuffixValuation suffixVals,
//			Constants consts,
//			WordValuation values) {
//		List<DataValue> equivClasses = new ArrayList<>();
//		equivClasses.addAll(valueGuards.keySet());
//		EquivalenceClassFilter eqcFilter = new EquivalenceClassFilter(equivClasses, useSuffixOpt);
//		List<DataValue> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(suffixValue), prefix, suffix.getActions(), values);
//
//		Map<DataValue, SDTGuard> ret = new LinkedHashMap<>();
//		for (Map.Entry<DataValue, SDTGuard> e : valueGuards.entrySet()) {
//			DataValue ec = e.getKey();
//			if (filteredEquivClasses.contains(ec)) {
//				ret.put(ec, e.getValue());
//			}
//		}
//		return ret;
//    }

    // process a tree query
//    @Override
//    public SDT treeQueryOld(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values,
//            Constants constants, SuffixValuation suffixValues, MultiTheoryTreeOracle oracle) {
//
//        int pId = values.size() + 1;
//
//        SuffixValue currentParam = suffix.getSuffixValue(pId);
//        DataType type = currentParam.getDataType();
//
//        Map<SDTGuard.EqualityGuard, SDT> tempKids = new LinkedHashMap<>();
//
//        Collection<DataValue> potSet = DataWords.joinValsToSet(constants.values(type),
//                DataWords.valSet(prefix, type), suffixValues.values(type));
//
//        List<DataValue> potList = new ArrayList<>(potSet);
//        List<DataValue> potential = getPotential(potList);
//
//        DataValue fresh = getFreshValue(potential);
//
//        List<DataValue> equivClasses = new ArrayList<>(potSet);
//        equivClasses.add(fresh);
//        //System.out.println(" prefix: " + prefix);
//        //System.out.println(" potential: " + potential);
//        //System.out.println(" eqs " + Arrays.toString(equivClasses.toArray()));
//        EquivalenceClassFilter eqcFilter = new EquivalenceClassFilter(equivClasses, useNonFreeOptimization);
//        List<DataValue> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(currentParam), prefix, suffix.getActions(), values);
//        assert filteredEquivClasses.size() > 0;
//
//        // TODO: integrate fresh-value optimization with restrictions
//        // special case: fresh values in outputs
//        if (freshValues) {
//
//            ParameterizedSymbol ps = computeSymbol(suffix, pId);
//
//            if (ps instanceof OutputSymbol && ps.getArity() > 0) {
//
//                int idx = computeLocalIndex(suffix, pId);
//                Word<PSymbolInstance> query = buildQuery(prefix, suffix, values);
//                Word<PSymbolInstance> trace = ioOracle.trace(query);
//
//                if (!trace.isEmpty() && trace.lastSymbol().getBaseSymbol().equals(ps)) {
//
//                    DataValue d = trace.lastSymbol().getParameterValues()[idx];
//
//                    if (d instanceof FreshValue) {
//                        d = getFreshValue(potential);
//                        values.put(pId, d);
//                        WordValuation trueValues = new WordValuation();
//                        trueValues.putAll(values);
//                        SuffixValuation trueSuffixValues = new SuffixValuation();
//                        trueSuffixValues.putAll(suffixValues);
//                        trueSuffixValues.put(currentParam, d);
//                        SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, constants, trueSuffixValues);
//
//                        LOGGER.trace(" single deq SDT : " + sdt.toString());
//
//                        Map<SDTGuard, SDT> merged = mergeGuards(tempKids, new SDTGuard.SDTAndGuard(currentParam, List.of()), sdt);
//
//                        LOGGER.trace("temporary guards = " + tempKids.keySet());
//                        LOGGER.trace("merged guards = " + merged.keySet());
//
//                        return new SDT(merged);
//                    }
//                } else {
//                    int maxSufIndex = DataWords.paramLength(suffix.getActions()) + 1;
//                    SDT rejSdt = makeRejectingBranch(currentParam.getId() + 1, maxSufIndex, type);
//                    SDTGuard.SDTTrueGuard trueGuard = new SDTGuard.SDTTrueGuard(currentParam);
//                    Map<SDTGuard, SDT> merged = new LinkedHashMap<>();
//                    merged.put(trueGuard, rejSdt);
//                    return new SDT(merged);
//                }
//            }
//        }
//
//        LOGGER.trace("potential " + potential.toString());
//
//        // process each 'if' case
//        // prepare by picking up the prefix values
//        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
//
//        LOGGER.trace("prefix list    " + prefixValues);
//
//        List<SDTGuard> diseqList = new ArrayList<>();
//        for (DataValue newDv : potential) {
//        	if (filteredEquivClasses.contains(newDv)) {
//	            LOGGER.trace(newDv.toString());
//
//	            // this is the valuation of the suffixvalues in the suffix
//	            SuffixValuation ifSuffixValues = new SuffixValuation();
//	            ifSuffixValues.putAll(suffixValues); // copy the suffix valuation
//
//	            SDTGuard.EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, constants);
//	            LOGGER.trace("eqGuard is: " + eqGuard);
//	            diseqList.add(new SDTGuard.DisequalityGuard(currentParam, eqGuard.register()));
//	            // construct the equality guard
//	            // find the data value in the prefix
//	            // this is the valuation of the positions in the suffix
//	            WordValuation ifValues = new WordValuation();
//	            ifValues.putAll(values);
//	            ifValues.put(pId, newDv);
//	            SDT eqOracleSdt = oracle.treeQuery(prefix, suffix, ifValues, constants, ifSuffixValues);
//
//	            tempKids.put(eqGuard, eqOracleSdt);
//        	}
//        }
//
//        Map<SDTGuard, SDT> merged;
//
//        // process the 'else' case
//        if (filteredEquivClasses.contains(fresh)) {
//        	// this is the valuation of the positions in the suffix
//	        WordValuation elseValues = new WordValuation();
//	        elseValues.putAll(values);
//	        elseValues.put(pId, fresh);
//
//	        // this is the valuation of the suffixvalues in the suffix
//	        SuffixValuation elseSuffixValues = new SuffixValuation();
//	        elseSuffixValues.putAll(suffixValues);
//	        elseSuffixValues.put(currentParam, fresh);
//
//	        SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, constants, elseSuffixValues);
//
//	        SDTGuard.SDTAndGuard deqGuard = new SDTGuard.SDTAndGuard(currentParam, diseqList);
//	        LOGGER.trace("diseq guard = " + deqGuard);
//
//	        // merge the guards
//	        merged = mergeGuards(tempKids, deqGuard, elseOracleSdt);
//        } else {
//        	// if no else case, we can only have a true guard
//        	// TODO: add  support for multiple equalities with same outcome
//        	assert tempKids.size() == 1;
//
//        	Iterator<Map.Entry<SDTGuard.EqualityGuard, SDT>> it = tempKids.entrySet().iterator();
//        	Map.Entry<SDTGuard.EqualityGuard, SDT> e = it.next();
//        	merged = new LinkedHashMap<SDTGuard, SDT>();
//        	merged.put(e.getKey(), e.getValue());
//        }
//
//        // only keep registers that are referenced by the merged guards
//        //pir.putAll(keepMem(merged));
//
//        LOGGER.trace("temporary guards = " + tempKids.keySet());
//        LOGGER.trace("merged guards = " + merged.keySet());
//
//        // clear the temporary map of children
//        tempKids.clear();
//
//        for (SDTGuard g : merged.keySet()) {
//            assert !(g == null);
//        }
//
//        SDT returnSDT = new SDT(merged);
//        return returnSDT;
//
//    }

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
    public Optional<DataValue> instantiate(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, Expression<Boolean> guard, int param,
            Constants constants, ConstraintSolver solver) {
    	Parameter p = new Parameter(ps.getPtypes()[param-1], param);
    	Set<DataValue> vals = DataWords.valSet(prefix, p.getDataType());
    	vals.addAll(vals.stream()
    			.filter(v -> v.getDataType().equals(p.getDataType()))
    			.collect(Collectors.toSet()));
    	vals.addAll(constants.values());
    	DataValue fresh = getFreshValue(new LinkedList<>(vals));

    	if (tryEquality(guard, p, fresh, solver, constants)) {
    		return Optional.of(fresh);
    	}

    	for (DataValue val : vals) {
    		if (tryEquality(guard, p, val, solver, constants)) {
    			return Optional.of(val);
    		}
    	}

    	return Optional.empty();
    }

    private boolean tryEquality(Expression<Boolean> guard, Parameter p, DataValue val, ConstraintSolver solver, Constants consts) {
    	Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<>();
    	valuation.put(p, val);
    	valuation.putAll(consts);
    	return solver.isSatisfiable(guard, valuation);
    }

    @Override
    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts, Version version) {
    	if (version == SymbolicSuffixRestrictionBuilder.Version.V1) {
    		return new UnrestrictedSuffixValue(suffixValue);
    	}
    	// for now, use generic restrictions with equality theory
    	return AbstractSuffixValueRestriction.genericRestriction(suffixValue, prefix, suffix, consts);
    }

    @Override
    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, RegisterValuation valuation, Constants consts) {
//    	if (version != SymbolicSuffixRestrictionBuilder.Version.V3) {
//    		return restrictSuffixValue(suffixValue, run.getPrefix(id), run.getSuffix(id), consts, version);
//    	}

//    	DataValue[] wVals = DataWords.valsOf(run.getWord());
//    	int splitIndex = DataWords.valsOf(run.getPrefix(id)).length + 1;
//    	RegisterValuation valuation = run.getValuation(id);
//    	int suffixValueIndex = splitIndex + suffixValue.getId() - 1;

//    	SuffixValuation suffixVals = new SuffixValuation();
//    	SuffixValueGenerator svgen = new SuffixValueGenerator();
//    	for (int i = splitIndex; i < wVals.length; i++) {
//    		suffixVals.put(svgen.next(wVals[i].getDataType()), wVals[i]);
//    	}
    	DataValue[] prefixVals = DataWords.valsOf(prefix);
    	DataValue[] suffixVals = DataWords.valsOf(suffix);
    	DataValue suffixDataValue = suffixVals[suffixValue.getId() - 1];

//		Collection<SymbolicDataValue> elements = new ArrayList<>();
//		elements.addAll(valuation.keySet());
//		elements.addAll(consts.keySet());
//		SuffixValueGenerator sgen = new SuffixValueGenerator();
//		for (DataValue d : suffixVals) {
//			elements.add(sgen.next(d.getDataType()));
//		}
//		elements.addAll(suffixVals.keySet());

    	// find all equalities
//    	List<DataValue> eqs = new ArrayList<>();
//    	List<Integer> eqIds = new ArrayList<>();
		int equal = 0;
		boolean equalsUnmapped = false;
    	Set<SymbolicDataValue> eqParams = new LinkedHashSet<>();
//    	Map<DataValue, Set<Register>> eqRegs = new LinkedHashMap<>();
//    	Map<DataValue, Set<Constant>> eqConsts = new LinkedHashMap<>();
//    	Map<DataValue, Set<SymbolicDataValue>> eqSuffixParams = new LinkedHashMap<>();
    	for (int i = 0; i < prefix.length(); i++) {
    		if (prefixVals[i].equals(suffixDataValue)) {
//    			eqs.add(prefixVals[i]);
//    			eqIds.add(i);
    			equal++;
    			boolean unmapped = true;
    			if (valuation.containsValue(prefixVals[i])) {
    				// register
    				eqParams.addAll(valuation.getAllKeysForValue(prefixVals[i]));
    				unmapped = false;
    			}
    			if (consts.containsValue(prefixVals[i])) {
    				// constant
    				eqParams.addAll(consts.getAllKeysForValue(prefixVals[i]));
    				unmapped = false;
    			}
    			equalsUnmapped = equalsUnmapped || unmapped;
    		}
    	}

    	for (int i = 0; i < suffixValue.getId() - 1; i++) {
    		// prior suffix value
    		if (suffixVals[i].equals(suffixDataValue)) {
    			SuffixValue sv = new SuffixValue(suffixDataValue.getDataType(), i+1);
    			equal++;
    			eqParams.add(sv);
    		}
    	}

    	// fresh
//    	if (eqs.isEmpty()) {
    	if (equal == 0) {
    		return new FreshSuffixValue(suffixValue);
    	}

    	// unmapped
    	if (equalsUnmapped && eqParams.isEmpty()) {
    		return new UnmappedEqualityRestriction(suffixValue);
    	}

    	// equal to one and only one element
//    	if (eqs.size() == 1 && eqParams.size() == 1) {
    	if (!equalsUnmapped && eqParams.size() == 1) {
    		return SuffixValueRestriction.equalityRestriction(suffixValue, eqParams.iterator().next());
    	}

    	// mapped
//    	if (eqs.size() == eqParams.size()) {
    	if (!equalsUnmapped) {
    		AbstractSuffixValueRestriction eqRestr = SuffixValueRestriction.equalityRestriction(suffixValue, eqParams);
    		FreshSuffixValue fresh = new FreshSuffixValue(suffixValue);
    		return new DisjunctionRestriction(suffixValue, eqRestr, fresh);
    	}

    	// mixed (should not happen, but good practice to have an else case)
    	return new UnrestrictedSuffixValue(suffixValue);

//    	if (eqs.size() == 1) {
//    		DataValue d = eqs.get(0);
//    		// register or constant?
//    		Set<Register> eqRegs = valuation.getAllKeysForValue(d);
//    		Set<Constant> eqConsts = consts.getAllKeysForValue(d);
//    		if (!eqRegs.isEmpty() && !eqConsts.isEmpty()) {
//    			Expression<Boolean> eqr = new NumericBooleanExpression(suffixValue, NumericComparator.EQ, eqRegs.iterator().next());
//    			Expression<Boolean> eqc = new NumericBooleanExpression(suffixValue, NumericComparator.EQ, eqConsts.iterator().next());
//    			return new SuffixValueRestriction(suffixValue, ExpressionUtil.or(eqr, eqc));
//    		}
//    		if (!eqRegs.isEmpty()) {
//    			return SuffixValueRestriction.equalityRestriction(suffixValue, eqRegs.iterator().next());
//    		}
//    		if (!eqConsts.isEmpty()) {
//    			return SuffixValueRestriction.equalityRestriction(suffixValue, eqConsts.iterator().next());
//    		}
//    		// suffix value?
//    		if (eqIds.get(0) > splitIndex) {
//    			SuffixValue sv = new SuffixValue(suffixValue.getDataType(), eqIds.get(0) - splitIndex + 1);
//    			return SuffixValueRestriction.equalityRestriction(suffixValue, sv);
//    		}
//    	}
//
//    	Set<SymbolicDataValue> diseqVals = new LinkedHashSet<>();
//    	diseqVals.addAll(valuation.keySet());
//    	diseqVals.addAll(consts.keySet());
//    	diseqVals.addAll(suffixVals.keySet());
//    	eqs.stream().forEach(d -> {
//    		diseqVals.removeAll(valuation.getAllKeysForValue(d));
//    		diseqVals.removeAll(consts.getAllKeysForValue(d));
//    		suffixVals.entrySet().stream().filter(e -> e.getValue().equals(d)).forEach(e -> diseqVals.remove(e.getKey()));
//    	});
////    	for (int i = offset; i < vals.length; i++) {
////    		if (!eqId.contains(i)) {
////    			diseqVals.add(new SuffixValue(vals[i].getDataType(), i+1));
////    		}
////    	}
//    	return SuffixValueRestriction.disequalityRestriction(suffixValue, diseqVals);
    }

//    private <T> void addElements(Map<DataValue, Set<T>> map, DataValue dv, Collection<T> sdvs) {
//    	Set<T> vals = map.containsKey(dv) ? map.get(dv) : new LinkedHashSet<>();
//    	vals.addAll(sdvs);
//    	map.put(dv, vals);
//    }
//
//    private Optional<SymbolicDataValue> mappedFrom(Mapping<? extends SymbolicDataValue, DataValue> mapping, DataValue val) {
//    	Set<? extends SymbolicDataValue> keys = mapping.getAllKeysForValue(val);
//    	if (keys.isEmpty()) {
//    		return Optional.empty();
//    	}
//    	return Optional.of(keys.iterator().next());
//    }

    private BiMap<Integer, DataValue> pot(Word<PSymbolInstance> u, DataType type) {
    	BiMap<Integer, DataValue> pot = HashBiMap.create();
    	DataValue[] vals = DataWords.valsOf(u);
    	for (int i = 0; i < vals.length; i++) {
    		if (vals[i].getDataType().equals(type) && !pot.values().contains(vals[i])) {
    			pot.put(i+1, vals[i]);
    		}
    	}
    	return pot;
    }

    private Map<Integer, DataValue> potmap(Word<PSymbolInstance> u, RegisterValuation uValuation, Word<PSymbolInstance> w, RegisterValuation wValuation, DataType type) {
    	BiMap<DataValue, Integer> pot = pot(u, type).inverse();
    	Map<Integer, DataValue> map = new LinkedHashMap<>();
    	for (Map.Entry<Register, DataValue> uEntry : uValuation.entrySet()) {
    		DataValue wVal = wValuation.get(uEntry.getKey());
    		if (wVal != null && wVal.getDataType().equals(type)) {
    			int id = pot.get(uEntry.getValue());
    			map.put(id, wVal);
    		}
    	}
    	return map;
    }

    private Set<Integer> potmatch(Word<PSymbolInstance> w, DataValue d, Word<PSymbolInstance> u, RegisterValuation uValuation, Map<Integer, DataValue> potmap) {
    	List<DataValue> wVals = new ArrayList<>(Arrays.asList(DataWords.valsOf(w, d.getDataType())));
    	Set<Integer> indices = new LinkedHashSet<>();

    	// add indices for each mapped occurrence of d
    	for (Map.Entry<Integer, DataValue> potmapEntry : potmap.entrySet()) {
    		if (potmapEntry.getValue().equals(d)) {
    			indices.add(potmapEntry.getKey());
    			wVals.remove(d);
    		}
    	}

//    	potmap.forEach((i,dv) -> {if (dv.equals(d)) indices.add(i);});

    	// if there are more occurrences of d than the unmapped, add all indices of unmapped data values
    	if (wVals.contains(d)) {
    		BiMap<Integer, DataValue> pot = pot(u, d.getDataType());
        	pot.forEach((i,dv) -> {if (!uValuation.containsValue(dv)) indices.add(i);});
    	}

    	return indices;
//    	BiMap<DataValue, Integer> pot = pot(u, d.getDataType()).inverse();
//    	Set<Integer> ret = new LinkedHashSet<>();
//    	for (Map.Entry<DataValue, Integer> potEntry : pot.entrySet()) {
//    		DataValue potVal = potEntry.getKey();
//    		int potId = potEntry.getValue();
//    		if (potmap.get(potId) != null || !uValuation.values().contains(potVal)) {
//    			ret.add(potId);
//    		}
//    	}
//    	return ret;
    }

    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue,
    		Word<PSymbolInstance> prefix,
    		Word<PSymbolInstance> suffix,
    		Word<PSymbolInstance> u,
    		RegisterValuation prefixValuation,
    		RegisterValuation uValuation,
    		Constants consts) {
    	int index = suffixValue.getId() - 1;
    	DataValue[] suffixVals = DataWords.valsOf(suffix);
    	Collection<DataValue> prefixVals = Arrays.asList(DataWords.valsOf(prefix));
    	DataValue[] uVals = DataWords.valsOf(u);
    	DataValue d = suffixVals[index];

    	List<DataValue> eqList = new ArrayList<>();
    	Map<Integer, DataValue> potmap = potmap(u, uValuation, prefix, prefixValuation, d.getDataType());
    	potmatch(prefix, d, u, uValuation, potmap).forEach(i -> eqList.add(uVals[i-1]));

    	List<SuffixValue> suffixEqList = new ArrayList<>();
    	List<SuffixValue> priorSuffixes = new ArrayList<>();
    	for (int i = 0; i < index; i++) {
			SuffixValue s = new SuffixValue(d.getDataType(), i+1);
    		priorSuffixes.add(s);
    		if (suffixVals[i].equals(d)) {
    			suffixEqList.add(s);
    		}
    	}

    	List<DataValue> unmappedEqList = new ArrayList<>(eqList);
    	for (SuffixValue s : suffixEqList) {
    		DataValue dv = suffixVals[s.getId()-1];
    		if (!prefixVals.contains(dv)) {
    			unmappedEqList.remove(dv);
    		}
    	}

    	Set<Constant> constEqList = new LinkedHashSet<>(consts.getAllKeysForValue(d));
    	constEqList.forEach(c -> unmappedEqList.remove(consts.get(c)));
//    	assert constEqList.size() <= 1 : "Constant mapping is not injective";


    	FreshSuffixValue fresh = new FreshSuffixValue(suffixValue);
		UnmappedEqualityRestriction uer = new UnmappedEqualityRestriction(suffixValue);

    	Collection<Register> regsEqList = dataValueToRegister(eqList, uValuation);
    	regsEqList.forEach(r -> unmappedEqList.remove(uValuation.get(r)));

//    	if (useMoreEfficientSuffixOptimization) {
//    		Set<SymbolicDataValue> eqParams = new LinkedHashSet<>();
//    		eqParams.addAll(regsEqList);
//    		eqParams.addAll(constEqList);
//    		eqParams.addAll(suffixEqList);
//    		SuffixValueRestriction eqRestr = SuffixValueRestriction.equalityRestriction(suffixValue, eqParams);
//    		if (unmappedEqList.isEmpty()) {
//    			if (eqParams.isEmpty()) {
//    				return new FreshSuffixValue(suffixValue);
//    			}
//    			return eqRestr;
//    		}
//    		UnmappedEqualityRestriction unmappedRestr = new UnmappedEqualityRestriction(suffixValue);
//    		if (eqParams.isEmpty()) {
//    			return unmappedRestr;
//    		}
//    		return DisjunctionRestriction.create(suffixValue, eqRestr, unmappedRestr);
//    	}

    	AbstractSuffixValueRestriction eqRestrSuffix = SuffixValueRestriction.equalityRestriction(suffixValue, suffixEqList);
    	AbstractSuffixValueRestriction eqRestrReg = SuffixValueRestriction.equalityRestriction(suffixValue, regsEqList);
    	AbstractSuffixValueRestriction eqRestrConst = SuffixValueRestriction.equalityRestriction(suffixValue, constEqList);

    	if (unmappedEqList.isEmpty()) {
    		// no unmapped equality
	    	if (regsEqList.size() == 1 && suffixEqList.isEmpty() && constEqList.isEmpty()) {
	    		// equals one register
	    		AbstractSuffixValueRestriction eqr = SuffixValueRestriction.equalityRestriction(suffixValue, regsEqList);
	    		return DisjunctionRestriction.create(suffixValue, eqr, fresh);
	    	}
	    	if (regsEqList.isEmpty() && suffixEqList.size() > 0 && constEqList.isEmpty()) {
	    		// equals prior suffix values
	    		return SuffixValueRestriction.equalityRestriction(suffixValue, suffixEqList.get(0));
	    	}
	    	if (regsEqList.isEmpty() && suffixEqList.isEmpty() && constEqList.size() == 1) {
	    		// equals one constant
	    		return SuffixValueRestriction.equalityRestriction(suffixValue, constEqList);
	    	}
	    	if (regsEqList.isEmpty() && suffixEqList.isEmpty() && constEqList.isEmpty()) {
	    		// equals nothing
	    		return fresh;
	    	}
	    	// equals any number of register, constant, prior suffix value, but no unmapped
	    	return DisjunctionRestriction.create(suffixValue, fresh, eqRestrSuffix, eqRestrReg, eqRestrConst);
    	} else if (regsEqList.isEmpty() && suffixEqList.isEmpty() && constEqList.isEmpty()) {
    		// equals only unmapped
    		return DisjunctionRestriction.create(suffixValue, uer, fresh);
    	}

    	// all classes of equality
    	List<Register> regsDiseqList = new ArrayList<>(uValuation.keySet());
    	regsDiseqList.removeAll(regsEqList);
    	List<Constant> constDiseqList = new ArrayList<>(consts.keySet());
    	constDiseqList.removeAll(constEqList);
    	List<SuffixValue> suffixDiseqList = new ArrayList<>(priorSuffixes);
    	suffixDiseqList.removeAll(suffixEqList);
    	SuffixValueRestriction diseqRestrRegs = SuffixValueRestriction.disequalityRestriction(suffixValue, regsDiseqList);
    	SuffixValueRestriction diseqRestrConst = SuffixValueRestriction.disequalityRestriction(suffixValue, constDiseqList);
    	SuffixValueRestriction diseqRestrSuffix = SuffixValueRestriction.disequalityRestriction(suffixValue, suffixDiseqList);
    	return DisjunctionRestriction.create(suffixValue, diseqRestrRegs, diseqRestrConst, diseqRestrSuffix);
    }

    private Collection<Register> dataValueToRegister(Collection<DataValue> vals, RegisterValuation valuation) {
    	Collection<Register> regs = new ArrayList<>();
    	valuation.forEach((r, v) -> {if (vals.contains(v)) regs.add(r);});
    	return regs;
    }

    @Override
    public AbstractSuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, AbstractSuffixValueRestriction> prior, SymbolicSuffixRestrictionBuilder.Version version) {
    	// for now, use generic restrictions with equality theory
    	return AbstractSuffixValueRestriction.genericRestriction(guard, prior);
    }

    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> u, PSymbolInstance action, Set<DataValue> memorable, Constants consts) {
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
    	DataValue[] actionVals = action.getParameterValues();
    	int index = suffixValue.getId() - 1;

    	if (consts.containsValue(actionVals[index])) {
    		return SuffixValueRestriction.equalityRestriction(suffixValue, consts.getAllKeysForValue(actionVals[index]).iterator().next());
    	}

    	Set<SuffixValue> prior = new LinkedHashSet<>();
    	for (int i = 0; i < index; i++) {
    		if (actionVals[index].equals(actionVals[i])) {
    			SuffixValue s = new SuffixValue(actionVals[i].getDataType(), i + 1);
    			prior.add(s);
    		}
    	}

    	AbstractSuffixValueRestriction eq = uVals.contains(actionVals[index]) ?
    			(memorable.contains(actionVals[index]) ?
    					SuffixValueRestriction.equalityRestriction(suffixValue, actionVals[index]) :
    						new UnmappedEqualityRestriction(suffixValue)) :
    							null;

    	if (prior.isEmpty()) {
    		return eq == null ? new FreshSuffixValue(suffixValue) : eq;
    	}

    	AbstractSuffixValueRestriction eqPrior = SuffixValueRestriction.equalityRestriction(suffixValue, prior);

    	if (eq == null) {
    		return eqPrior;
    	}

    	return DisjunctionRestriction.create(suffixValue, eq, eqPrior);

//    	if (uVals.contains(actionVals[index])) {
//    		if (memorable.contains(actionVals[index])) {
//    			return SuffixValueRestriction.equalityRestriction(suffixValue, actionVals[index]);
//    		}
//    		for (int i = 0; i < index; i++) {
//    			if (actionVals[index].equals(actionVals[i])) {
//    				SuffixValue prior = new SuffixValue(actionVals[i].getDataType(), i+1);
//    				return SuffixValueRestriction.equalityRestriction(suffixValue, prior);
//    			}
//    		}
//    		return new UnmappedEqualityRestriction(suffixValue);
//    	}

//    	return new FreshSuffixValue(suffixValue);
    }

    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> u, PSymbolInstance action, Set<DataValue> memorable, Set<DataValue> regs, Constants consts) {
    	int id = suffixValue.getId() - 1;
    	DataValue[] actionValues = action.getParameterValues();
    	if (regs.contains(actionValues[id])) {
    		return new TrueRestriction(suffixValue);
    	}
    	return restrictSuffixValue(suffixValue, u, action, memorable, consts);
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

    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictSDTs(SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
//    	Map<List<SDTGuard>, Boolean> paths1 = sdt1.getAllPaths(new ArrayList<>());
//    	Map<List<SDTGuard>, Boolean> paths2 = sdt2.getAllPaths(new ArrayList<>());
//    	for (Map.Entry<List<SDTGuard>, Boolean> e1 : paths1.entrySet()) {
//    		for (Map.Entry<List<SDTGuard>, Boolean> e2 : paths2.entrySet()) {
//    			if (e1.getValue() != e2.getValue()) {
//    				List<SDTGuard> path1 = e1.getKey();
//    				List<SDTGuard> path2 = e2.getKey();
//    				int n = path1.size();
//    				assert path2.size() == n : "SDTs are not compatible";
//    				Expression[] exprs = new Expression[n + n];
//    				Iterator<SDTGuard> it1 = path1.iterator();
//    				Iterator<SDTGuard> it2 = path2.iterator();
//    				for (int i = 0; i < exprs.length; i++) {
//    					exprs[i] = SDTGuard.toExpr(it1.next());
//    					exprs[i+n] = SDTGuard.toExpr(it2.next());
//    				}
//    				Expression<Boolean> expr = ExpressionUtil.and(exprs);
//    				if (solver.isSatisfiable(expr, new Mapping<>())) {
//    					List<SDTGuard> sorted1 = new ArrayList<>(path1);
//    					List<SDTGuard> sorted2 = new ArrayList<>(path2);
//    					sorted1.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
//    					sorted2.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
//    					Iterator<SDTGuard> git1 = sorted1.iterator();
//    					Iterator<SDTGuard> git2 = sorted2.iterator();
//    					Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    					while (git1.hasNext()) {
//    						assert git2.hasNext();
//    						SDTGuard g1 = git1.next();
//    						SDTGuard g2 = git2.next();
//    						SuffixValue s = g1.getParameter();
//    						assert g2.getParameter().equals(s);
//
//    						SuffixValueRestriction r1 = new SuffixValueRestriction(s, SDTGuard.toExpr(g1));
//    						SuffixValueRestriction r2 = new SuffixValueRestriction(s, SDTGuard.toExpr(g2));
//    						AbstractSuffixValueRestriction r3 = restrictions.get(s);
//    						AbstractSuffixValueRestriction r = ConjunctionRestriction.create(s, r1, r2, r3);
//    						ret.put(s, r);
//    					}
//    				}
//    			}
//    		}
//    	}
//    	List<List<SDTGuard>> paths = separatingPaths(sdt1, sdt2, solver);
//    	List<List<AbstractSuffixValueRestriction>> paths = restrictionsSeparatingPaths(sdt1, sdt2, solver);
    	List<AbstractSuffixValueRestriction> restrs = restrictionsSeparatingPaths(sdt1, sdt2, restrictions, solver);
    	if (restrs == null) {
    		throw new RuntimeException("SDTs have no comparable paths with different outcomes");
    	}
    	Map<SuffixValue, List<AbstractSuffixValueRestriction>> groups = group(restrs);
    	Map<SuffixValue, AbstractSuffixValueRestriction> flattened = flatten(groups);
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : restrictions.entrySet()) {
    		AbstractSuffixValueRestriction dis = flattened.get(e.getKey());
    		AbstractSuffixValueRestriction con = ConjunctionRestriction.create(e.getKey(), dis, e.getValue());
    		ret.put(e.getKey(), con);
    	}
    	return ret;
//    	assert paths.size() == 2;
//    	List<AbstractSuffixValueRestriction> path1 = paths.get(0);
//    	List<AbstractSuffixValueRestriction> path2 = paths.get(1);
//    	Iterator<AbstractSuffixValueRestriction> rit1 = path1.iterator();
//    	Iterator<AbstractSuffixValueRestriction> rit2 = path2.iterator();
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    	while (rit1.hasNext()) {
//    		assert rit2.hasNext();
//    		AbstractSuffixValueRestriction r1 = rit1.next();
//    		AbstractSuffixValueRestriction r2 = rit2.next();
//    		SuffixValue s = r1.getParameter();
//    		assert r2.getParameter().equals(s);
//
//    		AbstractSuffixValueRestriction r3 = restrictions.get(s);
//    		AbstractSuffixValueRestriction r = ConjunctionRestriction.create(s, r1, r2, r3);
//    		ret.put(s, r);
//    	}
//    	return ret;
    }

    private static Map<SuffixValue, List<AbstractSuffixValueRestriction>> group(List<AbstractSuffixValueRestriction> restrs) {
    	Map<SuffixValue, List<AbstractSuffixValueRestriction>> ret = new LinkedHashMap<>();
    	for (AbstractSuffixValueRestriction r : restrs) {
    		SuffixValue s = r.getParameter();
    		List<AbstractSuffixValueRestriction> list = ret.containsKey(s) ? ret.get(s) : new ArrayList<>();
    		list.add(r);
    		ret.put(s, list);
    	}
    	return ret;
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> flatten(Map<SuffixValue, List<AbstractSuffixValueRestriction>> groups) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	for (Map.Entry<SuffixValue, List<AbstractSuffixValueRestriction>> e : groups.entrySet()) {
    		AbstractSuffixValueRestriction dis = DisjunctionRestriction.create(e.getKey(), e.getValue());
    		ret.put(e.getKey(), dis);
    	}
    	return ret;
    }

//    private static List<List<SDTGuard>> separatingPaths(SDT sdt1, SDT sdt2, ConstraintSolver solver) {
//    	Map<List<SDTGuard>, Boolean> paths1 = sdt1.getAllPaths(new ArrayList<>());
//    	Map<List<SDTGuard>, Boolean> paths2 = sdt2.getAllPaths(new ArrayList<>());
//    	for (Map.Entry<List<SDTGuard>, Boolean> e1 : paths1.entrySet()) {
//    		for (Map.Entry<List<SDTGuard>, Boolean> e2 : paths2.entrySet()) {
//    			if (e1.getValue() != e2.getValue()) {
//    				List<SDTGuard> path1 = e1.getKey();
//    				List<SDTGuard> path2 = e2.getKey();
//    				int n = path1.size();
//    				assert path2.size() == n : "SDTs are not compatible";
//    				Expression[] exprs = new Expression[n + n];
//    				Iterator<SDTGuard> it1 = path1.iterator();
//    				Iterator<SDTGuard> it2 = path2.iterator();
//    				for (int i = 0; i < n; i++) {
//    					exprs[i] = SDTGuard.toExpr(it1.next());
//    					exprs[i+n] = SDTGuard.toExpr(it2.next());
//    				}
//    				Expression<Boolean> expr = ExpressionUtil.and(exprs);
//    				if (solver.isSatisfiable(expr, new Mapping<>())) {
//    					List<SDTGuard> sorted1 = new ArrayList<>(path1);
//    					List<SDTGuard> sorted2 = new ArrayList<>(path2);
//    					sorted1.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
//    					sorted2.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
//    					List<List<SDTGuard>> ret = new ArrayList<>();
//    					ret.add(sorted1);
//    					ret.add(sorted1);
//    					return ret;
//    				}
//    			}
//    		}
//    	}
//    	return null;
//    }

    private static List<AbstractSuffixValueRestriction> restrictionsSeparatingPaths(SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
    	Map<List<SDTGuard>, Boolean> paths1 = sdt1.getAllPaths(new ArrayList<>());
    	Map<List<SDTGuard>, Boolean> paths2 = sdt2.getAllPaths(new ArrayList<>());
    	for (Map.Entry<List<SDTGuard>, Boolean> e1 : paths1.entrySet()) {
    		for (Map.Entry<List<SDTGuard>, Boolean> e2 : paths2.entrySet()) {
    			if (e1.getValue() != e2.getValue()) {
    				List<SDTGuard> path1 = e1.getKey();
    				List<SDTGuard> path2 = e2.getKey();
    				int n = path1.size();
    				assert path2.size() == n : "SDTs are not compatible";
    				Expression[] exprs = new Expression[n + n];
    				Iterator<SDTGuard> it1 = path1.iterator();
    				Iterator<SDTGuard> it2 = path2.iterator();
    				for (int i = 0; i < n; i++) {
    					exprs[i] = SDTGuard.toExpr(it1.next());
    					exprs[i+n] = SDTGuard.toExpr(it2.next());
    				}
    				Expression<Boolean> expr = ExpressionUtil.and(exprs);
    				if (solver.isSatisfiable(expr, new Mapping<>())) {
    					List<SDTGuard> sorted1 = new ArrayList<>(path1);
    					List<SDTGuard> sorted2 = new ArrayList<>(path2);
    					sorted1.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
    					sorted2.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
    					List<AbstractSuffixValueRestriction> ret = new ArrayList<>();
//    					List<AbstractSuffixValueRestriction> restr1 = new ArrayList<>();
//    					List<AbstractSuffixValueRestriction> restr2 = new ArrayList<>();
    					ret.addAll(EqualityTheory.pathsConjunctionsToRestrictions(sorted1, sorted2, restrictions));
//    					sorted1.forEach(g -> restr1.add(guardToRestriction(g)));
//    					sorted2.forEach(g -> restr2.add(guardToRestriction(g)));
//    					ret.add(restr1);
//    					ret.add(restr2);
    					return ret;
    				}
    			}
    		}
    	}
    	return null;
    }

    private static List<AbstractSuffixValueRestriction> pathToRestrictions(List<SDTGuard> path, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
    	List<AbstractSuffixValueRestriction> ret = new ArrayList<>();
    	for (SDTGuard guard : path) {
    		if (guard instanceof SDTGuard.SDTTrueGuard) {
    			ret.add(restrictions.get(guard.getParameter()));
    		} else {
    			ret.add(guardToRestriction(guard));
    		}
    	}
    	return ret;
    }

    private static AbstractSuffixValueRestriction guardsConjunctionToRestriction(SDTGuard guard1, SDTGuard guard2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
    	SuffixValue param = guard1.getParameter();
    	assert guard2.getParameter().equals(param);
    	if (guard1 instanceof SDTGuard.EqualityGuard geq) {
    		assert isElseGuard(guard2) || (guard2 instanceof SDTGuard.EqualityGuard && ((SDTGuard.EqualityGuard) guard2).register().equals(geq.register()));
			return SuffixValueRestriction.equalityRestriction(param, SDTGuardElement.castToExpression(geq.register()));
    	} else if (guard2 instanceof SDTGuard.EqualityGuard geq) {
    		assert isElseGuard(guard1);
			return SuffixValueRestriction.equalityRestriction(param, SDTGuardElement.castToExpression(geq.register()));
    	}
    	assert isElseGuard(guard1) && isElseGuard(guard2);
    	if (guard1 instanceof SDTGuard.SDTTrueGuard && guard2 instanceof SDTGuard.SDTTrueGuard) {
    		return restrictions.get(guard1.getParameter());
    	}
    	return new FreshSuffixValue(param);
    }

    private static List<AbstractSuffixValueRestriction> pathsConjunctionsToRestrictions(List<SDTGuard> path1, List<SDTGuard> path2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
    	assert path1.size() == path2.size();
    	Iterator<SDTGuard> it1 = path1.iterator();
    	Iterator<SDTGuard> it2 = path2.iterator();
    	List<AbstractSuffixValueRestriction> ret = new ArrayList<>();
    	while (it1.hasNext()) {
    		SDTGuard g1 = it1.next();
    		SDTGuard g2 = it2.next();
    		ret.add(guardsConjunctionToRestriction(g1, g2, restrictions));
    	}
    	return ret;
    }

    private static AbstractSuffixValueRestriction guardToRestriction(SDTGuard guard) {
    	if (isElseGuard(guard)) {
    		return new FreshSuffixValue(guard.getParameter());
    	}
    	if (guard instanceof SDTGuard.EqualityGuard geq) {
    		return SuffixValueRestriction.equalityRestriction(geq.getParameter(), geq.register().asExpression());
    	}
    	return new SuffixValueRestriction(guard.getParameter(), SDTGuard.toExpr(guard));
    }

    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictSDT(SDT sdt, Set<DataValue> regs, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
    	List<List<AbstractSuffixValueRestriction>> paths = pathsBranchingOnRegisters(new ArrayList<>(), new ArrayList<>(), sdt, regs, restrictions, solver);
    	Map<SuffixValue, List<AbstractSuffixValueRestriction>> flattenedPaths = new LinkedHashMap<>();
    	for (List<AbstractSuffixValueRestriction> path : paths) {
    		for (AbstractSuffixValueRestriction r : path) {
    			SuffixValue s = r.getParameter();
    			List<AbstractSuffixValueRestriction> restrs = flattenedPaths.get(s);
    			if (restrs == null) {
    				restrs = new ArrayList<>();
    			}
    			restrs.add(r);
    			flattenedPaths.put(s, restrs);
    		}
    	}
    	assert flattenedPaths.size() <= restrictions.size();
//    	List<Map<SuffixValue, AbstractSuffixValueRestriction>> pathRestrictions = new ArrayList<>();
//    	for (List<SDTGuard> path : paths) {
//    		pathRestrictions.add(pathToRestriction(path));
//    	}
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> restrictionsEntry : restrictions.entrySet()) {
    		SuffixValue s = restrictionsEntry.getKey();
    		AbstractSuffixValueRestriction original = restrictionsEntry.getValue();
    		List<AbstractSuffixValueRestriction> disjuncts = flattenedPaths.get(s);
//    		List<AbstractSuffixValueRestriction> disjuncts = new ArrayList<>();
//    		for (Map<SuffixValue, AbstractSuffixValueRestriction> restr : pathRestrictions) {
//    			disjuncts.add(restr.get(s));
//    		}
    		if (sdtHasGuardOnMemorable(sdt, s, regs)) {
    			ret.put(s, new TrueRestriction(s));
    		} else {
	    		AbstractSuffixValueRestriction dis = DisjunctionRestriction.create(s, disjuncts);
	    		AbstractSuffixValueRestriction con = ConjunctionRestriction.create(s, original, dis);
	    		ret.put(s, con);
    		}
    	}
    	return ret;
    }

    private static List<List<AbstractSuffixValueRestriction>> pathsBranchingOnRegisters(List<List<AbstractSuffixValueRestriction>> paths, List<AbstractSuffixValueRestriction> path, SDT sdt, Set<DataValue> regs, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
//    	sdt = sortSDT(sdt);
    	List<List<AbstractSuffixValueRestriction>> ret = new ArrayList<>(paths);
    	if (Collections.disjoint(sdt.getDataValues(), regs)) {
    		return ret;
    	}

//    	boolean hasElseBranch = false;

    	for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
    		if (!Collections.disjoint(e.getValue().getDataValues(), regs)) {
    			List<AbstractSuffixValueRestriction> extendedPath = new ArrayList<>(path);
    			extendedPath.add(guardToRestriction(e.getKey()));
//    			int oldSize = ret.size();
    			ret.addAll(pathsBranchingOnRegisters(paths, extendedPath, e.getValue(), regs, restrictions, solver));
//    			if (ret.size() > oldSize && isElseGuard(e.getKey())) {
//    				hasElseBranch = true;
//    			}
    		}
    	}
    	for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
    		SDTGuard guard = e.getKey();
    		if (guard instanceof SDTGuard.EqualityGuard geq) {
    			if (regs.contains(geq.register())) {
    				// found equality guard, now find corresponding else
    				Optional<SDTGuard> gelseOpt = sdt.getChildren()
    						.keySet()
    						.stream()
    						.filter(g -> isElseGuard(g))
    						.findFirst();
    				if (gelseOpt.isEmpty()) {
    					break;
    				}
    				SDTGuard gelse = gelseOpt.get();
    				SDT sdtEq = e.getValue();
    				SDT sdtElse = sdt.getChildren().get(gelse);
//    				SDTGuard gelse = hasElseBranch ?
//    						gelseOpt.get() :
//    							new SDTGuard.DisequalityGuard(geq.getParameter(), geq.register());

    				// find two separating subpaths
//    				List<List<AbstractSuffixValueRestriction>> sep = restrictionsSeparatingPaths(sdtEq, sdtElse, solver);
//    				List<List<SDTGuard>> sep = separatingPaths(sdtEq, sdtElse, solver);
    				List<AbstractSuffixValueRestriction> sep = restrictionsSeparatingPaths(sdtEq, sdtElse, restrictions, solver);
    				if (sep == null) {
    		    		throw new RuntimeException("SDTs have no comparable paths with different outcomes");
    		    	}
//    				assert sep.size() == 2;

    				List<AbstractSuffixValueRestriction> fullPath1 = new ArrayList<>(path);
    				List<AbstractSuffixValueRestriction> fullPath2 = new ArrayList<>(path);
    				fullPath1.add(new TrueRestriction(geq.getParameter()));
//    				fullPath1.add(guardToRestriction(geq));
//    				fullPath1.addAll(sep.get(0));
    				fullPath1.addAll(sep);
    				fullPath2.add(new TrueRestriction(gelse.getParameter()));
//    				fullPath2.add(guardToRestriction(gelse));
//    				fullPath2.addAll(sep.get(1));
    				fullPath2.addAll(sep);
    				ret.add(fullPath1);
    				ret.add(fullPath2);
    			}
    		}
    	}
    	return ret;
    }

    private static boolean sdtHasGuardOnMemorable(SDT sdt, SuffixValue s, Set<DataValue> regs) {
    	Set<List<SDTGuard>> paths = sdt.getAllPaths(new ArrayList<>()).keySet();
    	for (List<SDTGuard> path : paths) {
    		for (SDTGuard guard : path) {
    			if (guard.getParameter().equals(s)) {
        			if (!Collections.disjoint(guard.getRegisters(), regs)) {
        				return true;
        			}
    			}
    		}
    	}
    	return false;
    }

//    private static Map<SuffixValue, AbstractSuffixValueRestriction> pathToRestriction(List<SDTGuard> path) {
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    	for (SDTGuard g : path) {
//    		SuffixValue s = g.getParameter();
//    		Expression<Boolean> expr = SDTGuard.toExpr(g);
//    		AbstractSuffixValueRestriction restr = g instanceof SDTGuard.SDTTrueGuard ?
//    				new FreshSuffixValue(s) :
//    					new SuffixValueRestriction(s, expr);
//    		ret.put(s, restr);
//    	}
//    	return ret;
//    }

    private static boolean isElseGuard(SDTGuard g) {
    	if (g instanceof SDTGuard.SDTTrueGuard) {
    		return true;
    	}
    	if (g instanceof SDTGuard.DisequalityGuard) {
    		return true;
    	}
    	if (g instanceof SDTGuard.SDTAndGuard andGuard) {
    		for (SDTGuard conjunct : andGuard.conjuncts()) {
    			if (!isElseGuard(conjunct)) {
    				return false;
    			}
    		}
    		return true;
    	}
    	return false;
    }

    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDTs(SDT sdt1, SDT sdt2, Prefix uExt1, Prefix uExt2, Bijection<DataValue> rp1, Bijection<DataValue> rp2, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb1 = uExt1.lastSymbol();
    	PSymbolInstance symb2 = uExt2.lastSymbol();
    	if (!symb1.getBaseSymbol().equals(symb2.getBaseSymbol())) {
    		throw new IllegalArgumentException("One-symbol extensions do not match");
    	}

    	List<DataValue> u1Vals = Arrays.asList(DataWords.valsOf(uExt1.prefix(uExt1.size() - 1)));
    	List<DataValue> u2Vals = Arrays.asList(DataWords.valsOf(uExt2.prefix(uExt2.size() - 1)));
    	List<DataValue> symb1Vals = Arrays.asList(symb1.getParameterValues());
    	List<DataValue> symb2Vals = Arrays.asList(symb2.getParameterValues());

    	Bijection<DataValue> u1Renaming = uExt1.getBijection(uExt1.getPath().getPrior(suffix));
    	Bijection<DataValue> u2Renaming = uExt2.getBijection(uExt2.getPath().getPrior(suffix));
//    	SDT sdt1Renamed = sdt1.relabel(SDTRelabeling.fromBijection(u1Renaming));
//    	SDT sdt2Renamed = sdt2.relabel(SDTRelabeling.fromBijection(u2Renaming));
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = suffix.getRestrictions();
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = EqualityTheory.restrictSDTs(sdt1Renamed, sdt2Renamed, suffix.getRestrictions(), solver);
    	ret = AbstractSuffixValueRestriction.shift(ret, symb1.getBaseSymbol().getArity());

    	Set<DataValue> vals = new LinkedHashSet<>();
    	AbstractSuffixValueRestriction.getElements(ret).stream().filter(e -> e instanceof DataValue).forEach(e -> vals.add((DataValue) e));
    	for (DataValue d : vals) {
    		if (consts.containsValue(d)) {
    			continue;
    		}
    		DataValue d1 = u1Renaming.inverse().get(d);
    		DataValue d2 = u2Renaming.inverse().get(d);
    		assert d1 != null && d2 != null : "Unmapped data values in restriction";

    		Set<SDTGuardElement> suffixVals = potentiallyEqualSuffixValues(d, symb1Vals);

    		if (!u1Vals.contains(d1) || !u2Vals.contains(d2)) {
    			assert symb1Vals.contains(d1) && symb2Vals.contains(d2);
    			ret = replaceEqualityRestriction(ret, d, suffixVals);
    		} else if (u1Vals.contains(d1) && u2Vals.contains(d2) && (symb1Vals.contains(d1) || symb2Vals.contains(d2))) {
    			Set<SDTGuardElement> eqVals = new LinkedHashSet<>(suffixVals);
    			eqVals.add(d);
    			ret = replaceEqualityRestriction(ret, d, eqVals);
    		}

//    		if (!u1Vals.contains(d1) && !u2Vals.contains(d2)) {
//    			// only in actions
//    			assert symb1Vals.contains(d1) && symb2Vals.contains(d2) : "Fresh data value in restriction";
//    			ret = replaceEqualityRestriction(ret, d, suffixVals);
//    		} else if (rp1.containsKey(d1) && rp2.containsKey(d2)) {
//    			// mapped in both prefixes
//    			if (symb1Vals.contains(d1) || symb2Vals.contains(d2)) {
//        			Set<SDTGuardElement> eqVals = new LinkedHashSet<>(suffixVals);
//        			eqVals.add(d1);
//    				ret = replaceEqualityRestriction(ret, d, eqVals);
//    			}
//    		} else {
//    			ret = replaceEqualityRestriction(ret, d, Set.of());
//    		}
    	}

    	ret = replaceUnmappedEqualityRestrictions(ret);

    	return AbstractSuffixValueRestriction.relabel(ret, rp1.toVarMapping());
    }

    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDT(SDT sdt, Prefix uExt, Bijection<DataValue> rp, Constants consts, Set<DataValue> missingRegs, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb = uExt.lastSymbol();
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(uExt.prefix(uExt.size() - 1)));
    	List<DataValue> symbVals = Arrays.asList(symb.getParameterValues());

    	Bijection<DataValue> renaming = uExt.getBijection(uExt.getPath().getPrior(suffix));
//    	SDT sdtRenamed = sdt.relabel(SDTRelabeling.fromBijection(renaming));
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = suffix.getRestrictions();
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = EqualityTheory.restrictSDT(sdtRenamed, missingRegs, suffix.getRestrictions(), solver);
    	ret = AbstractSuffixValueRestriction.shift(ret, symb.getBaseSymbol().getArity());

    	Set<DataValue> vals = new LinkedHashSet<>();
    	AbstractSuffixValueRestriction.getElements(ret).stream().filter(e -> e instanceof DataValue).forEach(e -> vals.add((DataValue) e));
    	for (DataValue d : vals) {
    		if (consts.containsValue(d)) {
    			continue;
    		}

    		Set<SDTGuardElement> suffixVals = potentiallyEqualSuffixValues(d, symbVals);

    		DataValue dRenamed = renaming.inverse().get(d);
    		assert dRenamed != null : "Unmapped data value in restriction";

    		if (!rp.containsKey(dRenamed)) {
    			assert symbVals.contains(dRenamed) : "Missing register present in restriction";
    			ret = replaceEqualityRestriction(ret, d, suffixVals);
    		} else if (symbVals.contains(dRenamed)) {
    			Set<SDTGuardElement> eqVals = new LinkedHashSet<>(suffixVals);
    			eqVals.add(d);
    			ret = replaceEqualityRestriction(ret, dRenamed, eqVals);
    		}
    	}

    	ret = replaceUnmappedEqualityRestrictions(ret);

    	return AbstractSuffixValueRestriction.relabel(ret, rp.toVarMapping());
    }

    private static Set<SDTGuardElement> potentiallyEqualSuffixValues(DataValue d, List<DataValue> vals) {
    	Set<SDTGuardElement> ret = new LinkedHashSet<>();
    	for (int i = 0; i < vals.size(); i++) {
    		if (vals.get(i).getDataType().equals(d.getDataType())) {
    			ret.add(new SuffixValue(d.getDataType(), i + 1));
    		}
    	}
    	return ret;
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> replaceEqualityRestriction(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, DataValue d, Set<SDTGuardElement> elements) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	ret.putAll(restrictions);
    	Set<ElementRestriction> handled = new LinkedHashSet<>();
    	for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, d)) {
    		if (handled.contains(er)) {
    			continue;
    		}
    		if (elements == null || elements.size() == 0) {
    			ret.put(er.cast().getParameter(), new TrueRestriction(er.cast().getParameter()));
//    			ret = AbstractSuffixValueRestriction.replaceRestriction(ret, er.cast(), new TrueRestriction(er.cast().getParameter()));
    		} else {
    			assert er instanceof EqualityRestriction;
    			EqualityRestriction replace = (EqualityRestriction) er;
    			Set<SDTGuardElement> elems = replace.getGuardElements();
    			elems.remove(d);
    			elems.addAll(elements);
    			EqualityRestriction by = new EqualityRestriction(replace.getParameter(), elems);
    			ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
    		}
    		handled.add(er);
    	}
    	return ret;
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> replaceUnmappedEqualityRestrictions(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	ret.putAll(restrictions);
    	for (SuffixValue suffixValue : AbstractSuffixValueRestriction.unmappedSuffixValues(restrictions)) {
    		ret.put(suffixValue, new TrueRestriction(suffixValue));
    	}
    	return ret;
    }

//    private static boolean isEqualityGuard(SDTGuard guard) {
//    	if (guard instanceof SDTGuard.EqualityGuard) {
//    		return true;
//    	}
//    	if (guard instanceof SDTGuard.SDTOrGuard orGuard) {
//    		for (SDTGuard g : orGuard.disjuncts()) {
//    			if (!isEqualityGuard(g)) {
//    				return false;
//    			}
//    		}
//    		return true;
//    	}
//    	return false;
//    }

//    private static List<SDTGuard.EqualityGuard> eqGuardConjunction(SDTGuard.SDTOrGuard orGuard, SDTGuard guard) {
//    	List<SDTGuard.EqualityGuard> other = new ArrayList<>();
//    	if (guard instanceof SDTGuard.EqualityGuard geq) {
//    		other.add(geq);
//    	} else if (guard instanceof SDTGuard.SDTOrGuard gor) {
//    		for (SDTGuard g : gor.disjuncts()) {
//    			if (g instanceof SDTGuard.EqualityGuard ge) {
//    				other.add(ge);
//    			}
//    		}
//    	}
//
//    	List<SDTGuard.EqualityGuard> ret = new ArrayList<>();
//    	for (SDTGuard g : orGuard.disjuncts()) {
//    		if (g instanceof SDTGuard.EqualityGuard geq1) {
//    			for (SDTGuard.EqualityGuard geq2 : other) {
//    				if (geq1.equals(geq2)) {
//    					ret.add(geq1);
//    				}
//    			}
//    		}
//    	}
//    	return ret;
//    }

    // sort sdt so that the else branch comes first
//    private static SDT sortSDT(SDT sdt) {
//    	if (sdt instanceof SDTLeaf) {
//    		return sdt;
//    	}
//    	Map<SDTGuard, SDT> children = new LinkedHashMap<>();
//    	children.putAll(sdt.getChildren());
//    	Map<SDTGuard, SDT> sorted = new LinkedHashMap<>();
//    	for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
//    		if (isElseGuard(e.getKey())) {
//    			SDTGuard gElse = e.getKey();
//    			SDT sdtElse = sortSDT(e.getValue());
//    			sorted.put(gElse, sdtElse);
//    			children.remove(gElse);
//    			break;
//    		}
//    	}
//    	for (Map.Entry<SDTGuard, SDT> e : children.entrySet()) {
//    		SDT child = sortSDT(e.getValue());
//    		sorted.put(e.getKey(), child);
//    	}
//    	return new SDT(sorted);
//    }
}
