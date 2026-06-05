/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
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
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
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

    @Override
    public boolean isUsingSuffixOptimization() {
    	return useSuffixOpt;
    }

    public List<DataValue> getPotential(List<DataValue> vals) {
        return vals;
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
			if (act.getArity() > 0 && act instanceof OutputSymbol) {
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

    @Override
    // instantiate a parameter with a data value
    public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, SuffixValuation pval,
            Constants constants, SDTGuard guard, SuffixValue param, Set<DataValue> oldDvs) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        LOGGER.trace("prefix values : " + prefixValues);
        DataType type = param.getDataType();
        Deque<SDTGuard> guards = new ArrayDeque<>();
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
            } else if (current instanceof SDTGuard.SDTAndGuard sdtAndGuard) {
                guards.addAll(sdtAndGuard.conjuncts());
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

    @Override
    public Optional<DataValue> instantiate(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, Expression<Boolean> guard, int param,
            List<DataValue> prior, Constants constants, ConstraintSolver solver) {
    	Parameter p = new Parameter(ps.getPtypes()[param-1], param);
    	Set<DataValue> vals = DataWords.valSet(prefix, p.getDataType());
    	vals.addAll(vals.stream()
    			.filter(v -> v.getDataType().equals(p.getDataType()))
    			.collect(Collectors.toSet()));
    	vals.addAll(constants.values());
    	vals.addAll(prior);
    	DataValue fresh = getFreshValue(new LinkedList<>(vals));

    	if (isSatisfiableWithEquality(guard, p, fresh, prior, solver, constants)) {
    		return Optional.of(fresh);
    	}

    	for (DataValue val : vals) {
    		if (isSatisfiableWithEquality(guard, p, val, prior, solver, constants)) {
    			return Optional.of(val);
    		}
    	}

    	return Optional.empty();
    }

    private boolean isSatisfiableWithEquality(Expression<Boolean> guard, Parameter p, DataValue val, List<DataValue> prior, ConstraintSolver solver, Constants consts) {
    	Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<>();
    	ParameterGenerator pgen = new ParameterGenerator();
    	for (DataValue d : prior) {
    		Parameter param = pgen.next(d.getDataType());
    		valuation.put(param, d);
    	}
    	valuation.put(p, val);
    	valuation.putAll(consts);
    	return solver.isSatisfiable(guard, valuation);
    }

    @Override
    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Constants consts) {
    	// for now, use generic restrictions with equality theory
    	return AbstractSuffixValueRestriction.genericRestriction(suffixValue, prefix, suffix, consts);
    }

    /**
     * @param u
     * @param type
     * @return position-injective potential of {@code u} matching data type {@code type}
     */
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

    /**
     * Mapping of indices in the potential of {@code u} to data values in {@code w} such that
     * for each index {@code l}, the data value at position {@code l} in {@code u} maps to
     * the same register in {@code uValuation} as the corresponding data value in {@code w}
     * does in {@code wValuation}.
     *
     * @param u
     * @param uValuation
     * @param w
     * @param wValuation
     * @param type
     * @return
     */
    public Map<Integer, DataValue> potmap(Word<PSymbolInstance> u, RegisterValuation uValuation, Word<PSymbolInstance> w, RegisterValuation wValuation, DataType type) {
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

    /**
     * The indices {@code l} of {@code u} such that if a hypothesis reaches {@code wValuation}
     * after a run over {@code w}, then there is a position-injective extension of
     * {@code uValuation} under which a data value {@code d} at index {@code l} of {@code u} will
     * satisfy an equality guard {@code (s == d)}.
     *
     * @param w
     * @param d
     * @param u
     * @param uValuation
     * @param potmap
     * @return
     */
    public Set<Integer> potmatch(Word<PSymbolInstance> w, DataValue d, Word<PSymbolInstance> u, RegisterValuation uValuation, Map<Integer, DataValue> potmap) {
    	List<DataValue> wVals = new ArrayList<>(Arrays.asList(DataWords.valsOf(w, d.getDataType())));
    	Set<Integer> indices = new LinkedHashSet<>();

    	// add indices for each mapped occurrence of d
    	for (Map.Entry<Integer, DataValue> potmapEntry : potmap.entrySet()) {
    		if (potmapEntry.getValue().equals(d)) {
    			indices.add(potmapEntry.getKey());
    			wVals.remove(d);
    		}
    	}

    	// if there are more occurrences of d than the unmapped, add all indices of unmapped data values
    	if (wVals.contains(d)) {
    		BiMap<Integer, DataValue> pot = pot(u, d.getDataType());
        	pot.forEach((i,dv) -> {if (!uValuation.containsValue(dv)) indices.add(i);});
    	}

    	return indices;
    }

    @Override
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

    	// find data values in u that the current suffix value may equal
    	List<DataValue> eqList = new ArrayList<>();
    	Map<Integer, DataValue> potmap = potmap(u, uValuation, prefix, prefixValuation, d.getDataType());
    	potmatch(prefix, d, u, uValuation, potmap).forEach(i -> eqList.add(uVals[i-1]));

    	// find prior suffix values that the current suffix value may equal
    	List<SuffixValue> suffixEqList = new ArrayList<>();
    	List<SuffixValue> priorSuffixes = new ArrayList<>();
    	for (int i = 0; i < index; i++) {
			SuffixValue s = new SuffixValue(d.getDataType(), i+1);
    		priorSuffixes.add(s);
    		if (suffixVals[i].equals(d)) {
    			suffixEqList.add(s);
    		}
    	}

    	// find constants the current suffix value may equal
    	Set<Constant> constEqList = new LinkedHashSet<>(consts.getAllKeysForValue(d));

    	// find registers in u that the current suffix value may equal
    	Collection<Register> regsEqList = dataValueToRegister(eqList, uValuation);

    	// collect unmapped data values in u that the current suffix value may equal
    	List<DataValue> unmappedEqList = new ArrayList<>(eqList);
    	for (SuffixValue s : suffixEqList) {
    		DataValue dv = suffixVals[s.getId()-1];
    		if (!prefixVals.contains(dv)) {
    			unmappedEqList.remove(dv);
    		}
    	}
    	constEqList.forEach(c -> unmappedEqList.remove(consts.get(c)));
    	regsEqList.forEach(r -> unmappedEqList.remove(uValuation.get(r)));

    	FreshSuffixValue restrrFresh = new FreshSuffixValue(suffixValue);
		UnmappedEqualityRestriction eqRestrUnmapped = new UnmappedEqualityRestriction(suffixValue);
    	AbstractSuffixValueRestriction eqRestrSuffix = SuffixValueRestriction.equalityRestriction(suffixValue, suffixEqList);
    	AbstractSuffixValueRestriction eqRestrReg = SuffixValueRestriction.equalityRestriction(suffixValue, regsEqList);
    	AbstractSuffixValueRestriction eqRestrConst = SuffixValueRestriction.equalityRestriction(suffixValue, constEqList);

    	if (unmappedEqList.isEmpty()) {
    		// no unmapped equality
	    	if (regsEqList.size() == 1 && /*suffixEqList.isEmpty() &&*/ constEqList.isEmpty()) {
	    		// equals one register
	    		AbstractSuffixValueRestriction eqr = SuffixValueRestriction.equalityRestriction(suffixValue, regsEqList);
	    		return eqr;
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
	    		return restrrFresh;
	    	}
	    	// equals any number of register, constant, prior suffix value, but no unmapped
	    	return DisjunctionRestriction.create(suffixValue, restrrFresh, eqRestrSuffix, eqRestrReg, eqRestrConst);
    	} else if (regsEqList.isEmpty() && suffixEqList.isEmpty() && constEqList.isEmpty()) {
    		// equals only unmapped
    		return DisjunctionRestriction.create(suffixValue, eqRestrUnmapped, restrrFresh);
    	}

    	// all classes of equality, collect all data values the current suffix value can not equal
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
    public AbstractSuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
    	// for now, use generic restrictions with equality theory
    	return AbstractSuffixValueRestriction.genericRestriction(guard, prior);
    }

    /**
     * Compute restriction on {@code suffixValue} by examining the relationship between its
     * corresponding data value in {@code action} and data values in {@code u}.
     *
     * @param suffixValue
     * @param u
     * @param action
     * @param memorable
     * @param consts
     * @return
     */
    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> u, PSymbolInstance action, Set<DataValue> memorable, Constants consts) {
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(u));
    	DataValue[] actionVals = action.getParameterValues();
    	int index = suffixValue.getId() - 1;

    	if (consts.containsValue(actionVals[index])) {
    		// we never have accidental equality with constants, so restriction can be equality with constant
    		return SuffixValueRestriction.equalityRestriction(suffixValue, consts.getAllKeysForValue(actionVals[index]));
    	}

    	Set<SuffixValue> prior = new LinkedHashSet<>();
    	for (int i = 0; i < index; i++) {
    		if (actionVals[index].equals(actionVals[i])) {
    			SuffixValue s = new SuffixValue(actionVals[i].getDataType(), i + 1);
    			prior.add(s);
    		}
    	}

    	AbstractSuffixValueRestriction unmappedWithFreshRestr = DisjunctionRestriction.create(suffixValue, new UnmappedEqualityRestriction(suffixValue), new FreshSuffixValue(suffixValue));

    	// determine type of equality with data values in u
    	AbstractSuffixValueRestriction eq = uVals.contains(actionVals[index]) ?
    			((memorable.contains(actionVals[index])) ?
    					SuffixValueRestriction.equalityRestriction(suffixValue, actionVals[index]) :
    						unmappedWithFreshRestr) :
    							null;

    	if (prior.isEmpty()) {
    		return eq == null ? new FreshSuffixValue(suffixValue) : eq;
    	}

    	AbstractSuffixValueRestriction eqPrior = SuffixValueRestriction.equalityRestriction(suffixValue, prior);

    	if (eq == null) {
    		return eqPrior;
    	}

    	return DisjunctionRestriction.create(suffixValue, eq, eqPrior);
    }

    /**
     * Compute restriction on {@code suffixValue} by examining the relationship between its
     * corresponding data value in {@code action} and data values in {@code u}. Any equality
     * with a missing register will result in a {@link TrueRestriction}.
     *
     * @param suffixValue
     * @param u
     * @param action
     * @param memorable
     * @param missingRegs
     * @param consts
     * @return
     */
    public AbstractSuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<PSymbolInstance> u, PSymbolInstance action, Set<DataValue> memorable, Set<DataValue> missingRegs, Constants consts) {
    	int id = suffixValue.getId() - 1;
    	DataValue[] actionValues = action.getParameterValues();
    	if (missingRegs.contains(actionValues[id])) {
    		return new TrueRestriction(suffixValue);
    	}
    	return restrictSuffixValue(suffixValue, u, action, memorable, consts);
    }

    @Override
    public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
        if (guard instanceof SDTGuard.EqualityGuard equalityGuard && equalityGuard.register().equals(register)) {
    		return true;
        } else if (guard instanceof SDTGuard.DisequalityGuard disequalityGuard && disequalityGuard.register().equals(register)) {
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

    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionsFromPruning(SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrictions, Set<DataValue> mappedInPrefix, List<DataValue> action1Vals, List<DataValue> action2Vals, ConstraintSolver solver) {
    	Optional<List<Map.Entry<SDTGuard, SDTGuard>>> pathsOpt = prune(sdt1, sdt2, solver);
    	assert pathsOpt.isPresent();
    	List<Map.Entry<SDTGuard, SDTGuard>> paths = pathsOpt.get();
    	List<SDTGuard> path = pathConjunction(paths);
    	return pathToRestrictions(path, oldRestrictions, mappedInPrefix, action1Vals, action2Vals, false);
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionsFromPruning(SDT sdt, Set<DataValue> missingRegs, Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrictions, Set<DataValue> mappedInPrefix, List<DataValue> actionVals, ConstraintSolver solver) {
    	List<Map.Entry<SDTGuard, SDTGuard>> paths = EqualityTheory.pruneRegClosed(sdt, missingRegs, solver);
    	List<SDTGuard> path = pathConjunction(paths);
    	return pathToRestrictions(path, oldRestrictions, mappedInPrefix, actionVals, Arrays.asList(), true);
    }

    private static List<SDTGuard> pathConjunction(List<Map.Entry<SDTGuard, SDTGuard>> paths) {
    	List<SDTGuard> path = new ArrayList<>();
    	for (Map.Entry<SDTGuard, SDTGuard> pair : paths) {
    		SDTGuard left = pair.getKey();
    		SDTGuard right = pair.getValue();
    		assert left.getParameter().equals(right.getParameter()) : "Guard pair do not match";
    		SDTGuard.EqualityGuard eg = left instanceof SDTGuard.EqualityGuard ?
    				(SDTGuard.EqualityGuard) left : (
    						right instanceof SDTGuard.EqualityGuard ?
    								(SDTGuard.EqualityGuard) right :
    									null);
    		if (eg != null) {
    			path.add(eg);
    		} else {
    			path.add(new SDTGuard.SDTTrueGuard(left.getParameter()));
    		}
    	}
    	return path;
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> pathToRestrictions(List<SDTGuard> path, Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrictions, Set<DataValue> mappedInPrefix, List<DataValue> action1Vals, List<DataValue> action2Vals, boolean isRegClosed) {
    	int arity = action1Vals.size();
    	Map<SuffixValue, AbstractSuffixValueRestriction> restr = new LinkedHashMap<>();
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> old : oldRestrictions.entrySet()) {
    		SuffixValue sv = old.getKey();
    		AbstractSuffixValueRestriction oldRestr = old.getValue();
    		SDTGuard guard = path.get(sv.getId() - arity - 1);
    		restr.put(sv, guardToRestriction(guard, oldRestr, mappedInPrefix, action1Vals, action2Vals, isRegClosed));
    	}
    	return restr;
    }

    private static AbstractSuffixValueRestriction guardToRestriction(SDTGuard guard, AbstractSuffixValueRestriction oldRestriction, Set<DataValue> mappedInPrefix, List<DataValue> action1Vals, List<DataValue> action2Vals, boolean isRegClosed) {
    	SuffixValue suffixValue = guard.getParameter();
    	if (guard instanceof SDTGuard.EqualityGuard eg) {
    		SDTGuardElement element = eg.register();
    		if (element instanceof DataValue d) {
				if (mappedInPrefix.contains(d)) {
					return new EqualityRestriction(suffixValue, Set.of(d));
				}
				Set<SDTGuardElement> potentiallyEqualSuffixValues = potentiallyEqualSuffixValues(d, action1Vals);
				if (isRegClosed) {
					if (action1Vals.contains(d) || action2Vals.contains(d)) {
						return DisjunctionRestriction.create(suffixValue,
								new UnmappedEqualityRestriction(suffixValue),
								new EqualityRestriction(suffixValue, potentiallyEqualSuffixValues),
								new FreshSuffixValue(suffixValue));
					}
					return DisjunctionRestriction.create(suffixValue,
							new UnmappedEqualityRestriction(suffixValue),
							new FreshSuffixValue(suffixValue));
				}
				return new EqualityRestriction(suffixValue, potentiallyEqualSuffixValues);
    		} else if (element instanceof SuffixValue sv) {
    			return new EqualityRestriction(suffixValue, Set.of(sv));
    		} else if (element instanceof Constant c) {
    			return new EqualityRestriction(suffixValue, Set.of(c));
    		} else {
    			throw new IllegalArgumentException("Invalid value in equality: " + eg.register());
    		}
    	}

    	if (oldRestriction instanceof EqualityRestriction er) {
    		Set<SDTGuardElement> suffixVals = new LinkedHashSet<>();
    		for (SDTGuardElement elem : er.getGuardElements()) {
    			if (elem instanceof DataValue d) {
    				if (mappedInPrefix.contains(d)) {
    					return new EqualityRestriction(suffixValue, Set.of(d));
    				}
    				Set<SDTGuardElement> potentiallyEqualSuffixValues = potentiallyEqualSuffixValues(d, action1Vals);
    				return new EqualityRestriction(suffixValue, potentiallyEqualSuffixValues);
    			} else if (elem instanceof Constant c) {
    				return new EqualityRestriction(suffixValue, Set.of(c));
    			} else if (elem instanceof SuffixValue) {
    				suffixVals.add(elem);
    			}
    		}
    		assert !suffixVals.isEmpty() : "Invalid equality restriction: " + er;
    		return new EqualityRestriction(suffixValue, suffixVals);
    	}

    	assert oldRestriction.containsFresh() : "Restriction invalid at this point: " + oldRestriction;
    	return new FreshSuffixValue(suffixValue);
    }

    /**
     * Find a "common" path in {@code sdt1} and {@code sdt2} (i.e., a path in {@code sdt1} and
     * another path in {@code sdt2} such that the conjunction of these two paths is satisfiable)
     * with different outcomes.
     *
     * @param sdt1
     * @param sdt2
     * @param restrictions
     * @param solver
     * @return {@code Optional} containing a "common" path in {@code sdt1} and {@code sdt2}, if such a path exists
     */
    private static Optional<List<Map.Entry<SDTGuard, SDTGuard>>> prune(SDT sdt1, SDT sdt2, ConstraintSolver solver) {
    	Map<List<SDTGuard>, Boolean> paths1 = sdt1.getAllPaths(new ArrayList<>());
    	Map<List<SDTGuard>, Boolean> paths2 = sdt2.getAllPaths(new ArrayList<>());
    	for (Map.Entry<List<SDTGuard>, Boolean> e1 : paths1.entrySet()) {
    		for (Map.Entry<List<SDTGuard>, Boolean> e2 : paths2.entrySet()) {
    			if (!e1.getValue().equals(e2.getValue())) {
    				// paths have different outcomes
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
    					// common path
    					List<SDTGuard> sorted1 = new ArrayList<>(path1);
    					List<SDTGuard> sorted2 = new ArrayList<>(path2);
    					// sort paths in ascending suffix value order
    					sorted1.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));
    					sorted2.sort((g1, g2) -> Integer.compare(g1.getParameter().getId(), g2.getParameter().getId()));

    					List<Map.Entry<SDTGuard, SDTGuard>> ret = new ArrayList<>();
    					Iterator<SDTGuard> pathIt1 = sorted1.iterator();
    					Iterator<SDTGuard> pathIt2 = sorted2.iterator();
    					while (pathIt1.hasNext()) {
    						assert pathIt2.hasNext();
    						ret.add(new SimpleEntry<>(pathIt1.next(), pathIt2.next()));
    					}

    					return Optional.of(ret);
    				}
    			}
    		}
    	}
    	return Optional.empty();
    }

    private static List<Map.Entry<SDTGuard, SDTGuard>> pruneRegClosed(SDT sdt, Set<DataValue> missingRegs, ConstraintSolver solver) {
    	return pruneRegClosed(new ArrayList<>(), sdt, missingRegs, solver);
    }

    private static List<Map.Entry<SDTGuard, SDTGuard>> pruneRegClosed(List<Map.Entry<SDTGuard, SDTGuard>> path, SDT sdt, Set<DataValue> missingRegs, ConstraintSolver solver) {
    	if (sdt.getChildren() == null) {
    		return new ArrayList<>();
    	}

    	Map<SDTGuard, SDT> children = sdt.getChildren();
    	for (Map.Entry<SDTGuard, SDT> child : children.entrySet()) {
    		SDTGuard guard = child.getKey();
    		if (guard instanceof SDTGuard.EqualityGuard ifGuard) {
    			SDTGuardElement element = ifGuard.register();
    			if (element instanceof DataValue d && missingRegs.contains(d)) {
    				SDTGuard elseGuard = findElseGuard(children.keySet());
    				SDT ifSdt = child.getValue();
    				SDT elseSdt = children.get(elseGuard);
    				Optional<List<Map.Entry<SDTGuard, SDTGuard>>> prunedPathsOpt = EqualityTheory.prune(ifSdt, elseSdt, solver);
    				assert prunedPathsOpt.isPresent();
    				List<Map.Entry<SDTGuard, SDTGuard>> prunedPaths = prunedPathsOpt.get();

    				path.add(Map.entry(ifGuard, elseGuard));
    				path.addAll(prunedPaths);
    				return path;
    			}
    		}

    		path.add(Map.entry(guard, guard));
    		List<Map.Entry<SDTGuard, SDTGuard>> potPath = pruneRegClosed(path, child.getValue(), missingRegs, solver);
    		if (!potPath.isEmpty()) {
    			return potPath;
    		}
    	}
    	return new ArrayList<>();
    }

    private static SDTGuard findElseGuard(Set<SDTGuard> guards) {
    	for (SDTGuard guard : guards) {
    		if (isElseGuard(guard)) {
    			return guard;
    		}
    	}
    	throw new IllegalStateException("No else guard to corresponding equality guard");
    }

    /**
     * @param g
     * @return {@code true} if and only if {@code g} is an else guard
     */
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

    /**
     * Derive restrictions for an extended symbolic suffix, i.e., {@code suffix} prepended by
     * the last symbol of {@code uExt1}. The new restrictions are derived by examining the paths
     * of {@code sdt1} and {@code sdt2} to find a "common" path in {@code sdt1} and {@code sdt2}
     * with different outcomes. The restrictions will have data values mapped to {@code uExt1}.
     *
     * @param sdt1
     * @param sdt2
     * @param uExt1
     * @param uExt2
     * @param u1RpBijection
     * @param u2RpBijection
     * @param consts
     * @param suffix
     * @param solver
     * @return
     */
    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDTs(SDT sdt1, SDT sdt2, Prefix uExt1, Prefix uExt2, Bijection<DataValue> u1RpBijection, Bijection<DataValue> u2RpBijection, boolean sameLeaf, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb1 = uExt1.lastSymbol();
    	PSymbolInstance symb2 = uExt2.lastSymbol();
    	if (!symb1.getBaseSymbol().equals(symb2.getBaseSymbol())) {
    		throw new IllegalArgumentException("One-symbol extensions do not match");
    	}
    	int arity = symb1.getBaseSymbol().getArity();

    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestr = suffix.getRestrictions();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrShifted = AbstractSuffixValueRestriction.shift(oldRestr, arity);
    	SDT sdt1Shifted = sdt1.shift(arity);
    	SDT sdt2Shifted = sdt2.shift(arity);

    	Bijection<DataValue> uExt1AncestorRenaming = uExt1.getBijection(uExt1.getPath().getPrior(suffix)).inverse();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrShiftedRenamed = AbstractSuffixValueRestriction.relabel(oldRestrShifted, uExt1AncestorRenaming.toVarMapping());

    	Mapping<DataValue, SuffixValue> actionRenaming1 = actionValueToSuffixValue(uExt1);
    	Mapping<DataValue, SuffixValue> actionRenaming2 = actionValueToSuffixValue(uExt2);
    	SDT sdt1ActionRenamed = sdt1Shifted.relabel(SDTRelabeling.fromMapping(actionRenaming1));
    	SDT sdt2ActionRenamed = sdt2Shifted.relabel(SDTRelabeling.fromMapping(actionRenaming2));
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrActionRenamed = AbstractSuffixValueRestriction.relabel(oldRestrShiftedRenamed, actionRenaming1);

    	Bijection<DataValue> uExt2Renaming = EqualityTheory.collisionFreeRenaming(uExt1, uExt2, u1RpBijection, u2RpBijection, suffix, sameLeaf);
    	SDT sdt2Renamed = sdt2ActionRenamed.relabel(SDTRelabeling.fromBijection(uExt2Renaming));

    	Set<DataValue> mappedInPrefix = u1RpBijection.keySet();
    	List<DataValue> action1Vals = Arrays.asList(symb1.getParameterValues());
    	List<DataValue> action2Vals = new ArrayList<>();
    	renameCollection(action2Vals, Arrays.asList(symb2.getParameterValues()), uExt2Renaming);

    	Map<SuffixValue, AbstractSuffixValueRestriction> restrPruned = restrictionsFromPruning(sdt1ActionRenamed, sdt2Renamed, oldRestrActionRenamed, mappedInPrefix, action1Vals, action2Vals, solver);

    	Bijection<DataValue> uExt2FromAncestorRenaming = uExt2.getBijection(uExt2.getPath().getPrior(suffix)).inverse();
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrElseParams = addActionParameter(restrPruned, actionRenaming2, uExt1AncestorRenaming.inverse(), uExt2FromAncestorRenaming);

    	return restrElseParams;
    }

    private static void renameCollection(Collection<DataValue> dest, Collection<DataValue> col, Bijection<DataValue> renaming) {
    	for (DataValue d : col) {
    		if (renaming.containsKey(d)) {
    			dest.add(renaming.get(d));
    		}
    	}
    }

    /**
     * Derive restrictions for an extended symbolic suffix, i.e., {@code suffix} prepended by
     * the last symbol of {@code uExt}. The restrictions are derived by examining paths of
     * {@code sdt} to find and isolate paths that reveal unmapped data values. Each suffix value
     * with a guard on unmapped data value will have a {@link TrueRestriction}, while any other
     * will have a restriction given by the conjunction of existing restrictions in {@code suffix}
     * and restrictions derived from the paths in {@code sdt} which reveal the unmapped data
     * values.
     *
     * @param sdt
     * @param uExt
     * @param rp
     * @param consts
     * @param suffix
     * @param solver
     * @return
     */
    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDT(SDT sdt, Prefix u, Prefix uExt, Bijection<DataValue> rp, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver, boolean useImprovedRegClosed) {
    	PSymbolInstance symb = uExt.lastSymbol();
    	int arity = symb.getBaseSymbol().getArity();
    	List<DataValue> actionVals = Arrays.asList(symb.getParameterValues());

    	Set<DataValue> missingRegs = new LinkedHashSet<>(sdt.getDataValues());
    	missingRegs.removeAll(rp.keySet());

    	if (!Collections.disjoint(actionVals, missingRegs) || !useImprovedRegClosed) {
    		return transferRestriction(sdt, u, uExt, rp, consts, suffix, solver);
    	}

    	Bijection<DataValue> ancestorRenaming = uExt.getBijection(uExt.getPath().getPrior(suffix)).inverse();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestr = suffix.getRestrictions();
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrRenamed = AbstractSuffixValueRestriction.relabel(oldRestr, ancestorRenaming.toVarMapping());

    	SDT sdtShifted = sdt.shift(arity);
    	Map<SuffixValue, AbstractSuffixValueRestriction> oldRestrRenamedShifted = AbstractSuffixValueRestriction.shift(oldRestrRenamed, arity);

    	Set<DataValue> mappedInPrefix = rp.keySet();

    	return restrictionsFromPruning(sdtShifted, missingRegs, oldRestrRenamedShifted, mappedInPrefix, actionVals, solver);
    }

    public static Map<SuffixValue, AbstractSuffixValueRestriction> transferRestriction(SDT sdt, Prefix u, Prefix uExt, Bijection<DataValue> rp, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb = uExt.lastSymbol();
    	ArrayList<DataValue> symbVals = new ArrayList<>(Arrays.asList(symb.getParameterValues()));

    	Set<DataValue> missingRegs = new LinkedHashSet<>(sdt.getDataValues());
    	missingRegs.removeAll(rp.keySet());

    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = AbstractSuffixValueRestriction.shift(suffix.getRestrictions(), symb.getBaseSymbol().getArity());
    	sdt = sdt.shift(symb.getBaseSymbol().getArity());

    	Bijection<DataValue> ancestorRenaming = uExt.getBijection(uExt.getPath().getPrior(suffix));

    	// find missing registers in restriction and replace those that are in the action with suffix params
    	Mapping<DataValue, SuffixValue> suffixValueRenaming = new Mapping<>();
    	for (DataValue r : missingRegs) {
    		if (symbVals.contains(r) && ancestorRenaming.containsKey(r)) {
    			SuffixValue s = new SuffixValue(r.getDataType(), symbVals.indexOf(r));
    			suffixValueRenaming.put(r, s);
    		}
    	}
    	ret = AbstractSuffixValueRestriction.relabel(ret, suffixValueRenaming);

    	// replace unmapped restriction depending on sdt guards
    	ret = replaceUnmappedRestriction(ret, u, uExt, sdt);

    	return AbstractSuffixValueRestriction.relabel(ret, ancestorRenaming.inverse().toVarMapping());
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> replaceUnmappedRestriction(Map<SuffixValue, AbstractSuffixValueRestriction> restr, Prefix u, Prefix uExt, SDT sdt) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	List<DataValue> symbVals = Arrays.asList(uExt.lastSymbol().getParameterValues());
    	Set<DataValue> uMem = u.getRegisters();

    	Set<SuffixValue> unmappedSuffixVals = AbstractSuffixValueRestriction.unmappedSuffixValues(restr);
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : restr.entrySet()) {
    		SuffixValue sv = e.getKey();
    		if (!unmappedSuffixVals.contains(sv)) {
    			ret.put(sv, e.getValue());
    			continue;
    		}
    		List<AbstractSuffixValueRestriction> disjuncts = new ArrayList<>();
    		disjuncts.add(new FreshSuffixValue(sv));
    		Set<SDTGuardElement> eqElems = new LinkedHashSet<>();
    		for (SDTGuard g : sdt.getGuards(sv)) {
    			if (g instanceof SDTGuard.EqualityGuard eg) {
    				SDTGuardElement element = eg.register();
    				if (SDTGuardElement.isDataValue(element)) {
    					DataValue d = (DataValue) element;
    					if (uMem.contains(d)) {
    						eqElems.add(element);
    					} else {
    						disjuncts.add(new UnmappedEqualityRestriction(sv));
    					}
    					if (symbVals.contains(d)) {
							eqElems.addAll(EqualityTheory.potentiallyEqualSuffixValues(d, symbVals));
						}
    				} else if (SDTGuardElement.isSuffixValue(element)) {
    					eqElems.add(element);
    				}
    			}
    		}
    		if (!eqElems.isEmpty()) {
    			disjuncts.add(new EqualityRestriction(sv, eqElems));
    		}
    		ret.put(sv, DisjunctionRestriction.create(sv, disjuncts));
    	}
    	return ret;
    }

    /**
     * @param vals
     * @param type
     * @return fresh data value of type {@code type} not present in {@code vals}
     */
    public static DataValue getFreshValue(Collection<DataValue> vals, DataType type) {
        BigDecimal dv = new BigDecimal("-1");
        for (DataValue d : vals) {
            dv = dv.max(d.getValue());
        }

        return new DataValue(type, BigDecimal.ONE.add(dv));
    }

    private static Bijection<DataValue> collisionFreeRenaming(Prefix uExt1, Prefix uExt2, Bijection<DataValue> u1RpBijection, Bijection<DataValue> u2RpBijection, SymbolicSuffix suffix, boolean sameLeaf) {
    	Set<DataValue> usedVals = DataWords.valSet(uExt1);
    	Bijection<DataValue> freshRenaming = new Bijection<>();
    	for (DataValue d : DataWords.valsOf(uExt2)) {
    		DataValue fresh = EqualityTheory.getFreshValue(usedVals, d.getDataType());
    		freshRenaming.put(d, fresh);
    		usedVals.add(fresh);
    	}

    	Bijection<DataValue> uExt1AncestorRenaming = uExt1.getBijection(uExt1.getPath().getPrior(suffix));
    	Bijection<DataValue> uExt2AncestorRenaming = uExt2.getBijection(uExt2.getPath().getPrior(suffix));
    	Bijection<DataValue> uExt1RpBijection = uExt1.getRpBijection();
    	Bijection<DataValue> uExt2RpBijection = uExt2.getRpBijection();

    	List<Bijection<DataValue>> bijections = new ArrayList<>();

    	bijections.add(uExt2AncestorRenaming.compose(uExt1AncestorRenaming.inverse()));
    	bijections.add(u2RpBijection.compose(u1RpBijection.inverse()));
    	if (sameLeaf) {
    		bijections.add(uExt2RpBijection.compose(uExt1RpBijection.inverse()));
    	}

    	Bijection<DataValue> renaming = new Bijection<>(freshRenaming);
    	for (Bijection<DataValue> b : bijections) {
    		renaming = updateRenaming(renaming, b);
    	}
    	return renaming;
    }

    private static Bijection<DataValue> updateRenaming(Bijection<DataValue> renaming, Bijection<DataValue> b) {
    	Bijection<DataValue> ret = new Bijection<>(renaming);
    	for (Map.Entry<DataValue, DataValue> e : b.entrySet()) {
    		ret.put(e.getKey(), e.getValue());
    	}
    	return ret;
    }

    private static Mapping<DataValue, SuffixValue> actionValueToSuffixValue(Word<PSymbolInstance> uExt) {
    	List<DataValue> uVals = Arrays.asList(DataWords.valsOf(uExt.prefix(uExt.length() - 1)));
    	DataValue[] actionVals = uExt.lastSymbol().getParameterValues();

    	Mapping<DataValue, SuffixValue> ret = new Mapping<>();
    	for (int i = 0; i < actionVals.length; i++) {
    		DataValue d = actionVals[i];
    		if (!uVals.contains(d)) {
    			SuffixValue sv = new SuffixValue(d.getDataType(), i + 1);
    			ret.put(d, sv);
    		}
    	}
    	return ret;
    }

    /**
     * Compute set of suffix values corresponding to each data value in {@code vals} with the
     * same type as {@code d}. The id for each suffix value is given by the position of its
     * corresponding value in {@code vals}.
     *
     * @param d
     * @param vals
     * @return set of suffix values corresponding to {@code vals} with the same type as {@code d}
     */
    private static Set<SDTGuardElement> potentiallyEqualSuffixValues(DataValue d, List<DataValue> vals) {
    	Set<SDTGuardElement> ret = new LinkedHashSet<>();
    	for (int i = 0; i < vals.size(); i++) {
    		if (vals.get(i).getDataType().equals(d.getDataType())) {
    			ret.add(new SuffixValue(d.getDataType(), i + 1));
    		}
    	}
    	return ret;
    }

    private static Set<DataValue> getDataValueElements(Map<SuffixValue, AbstractSuffixValueRestriction> restr) {
    	return AbstractSuffixValueRestriction.getElements(restr)
    			.stream()
    			.filter(e -> e instanceof DataValue)
    			.map(d -> (DataValue) d)
    			.collect(Collectors.toSet());
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> addActionParameter(Map<SuffixValue, AbstractSuffixValueRestriction> restr, Mapping<DataValue, SuffixValue> actionRenaming, Bijection<DataValue> toAncestorRenaming, Bijection<DataValue> fromAncestorToExtRenaming) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restr;
    	Set<DataValue> restrVals = getDataValueElements(restr);
    	for (DataValue d : restrVals) {
    		if (toAncestorRenaming.containsKey(d)) {
    			DataValue dAncestor = toAncestorRenaming.get(d);
    			DataValue dExt = fromAncestorToExtRenaming.get(dAncestor);
    			assert dExt != null : "Data value of ancestor node not present in bijection";
    			if (actionRenaming.containsKey(dExt)) {
    				for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restr, d)) {
    					SuffixValue sv = er.cast().getParameter();
    					assert er instanceof EqualityRestriction : "Unsupported restriction type";
    					EqualityRestriction replace = (EqualityRestriction) er;
    					Set<SDTGuardElement> elems = new LinkedHashSet<>(replace.getGuardElements());
    					elems.add(actionRenaming.get(dExt));
    					EqualityRestriction by = new EqualityRestriction(sv, elems);
    					ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
    				}
    			}
    		}
    	}
    	return ret;
    }
}
