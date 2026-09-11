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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
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
}
