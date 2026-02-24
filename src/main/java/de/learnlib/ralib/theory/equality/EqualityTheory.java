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

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
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

    	// if there are more occurrences of d than the unmapped, add all indices of unmapped data values
    	if (wVals.contains(d)) {
    		BiMap<Integer, DataValue> pot = pot(u, d.getDataType());
        	pot.forEach((i,dv) -> {if (!uValuation.containsValue(dv)) indices.add(i);});
    	}

    	return indices;
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

    	FreshSuffixValue fresh = new FreshSuffixValue(suffixValue);
		UnmappedEqualityRestriction uer = new UnmappedEqualityRestriction(suffixValue);

    	Collection<Register> regsEqList = dataValueToRegister(eqList, uValuation);
    	regsEqList.forEach(r -> unmappedEqList.remove(uValuation.get(r)));

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

    /**
     * Compare {@code sdt1} and {@code sdt2} for "common" paths with different outcomes and
     * derive restrictions from these.
     *
     * @param sdt1
     * @param sdt2
     * @param restrictions
     * @param solver
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionsSeparatingPathsInSDTs(SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
//    	List<AbstractSuffixValueRestriction> restrs = restrictionsSeparatingPaths(sdt1, sdt2, restrictions, solver);
    	List<Map.Entry<SDTGuard, SDTGuard>> paths = restrictionsSeparatingPaths(sdt1, sdt2, restrictions, solver);
    	if (paths == null) {
    		return restrictions;
    	}

    	List<AbstractSuffixValueRestriction> restrs = pathsToRestrictions(paths, restrictions, Set.of());
    	return restrictionListToMap(restrs);
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionListToMap(List<AbstractSuffixValueRestriction> restrList) {
    	Map<SuffixValue, List<AbstractSuffixValueRestriction>> groups = new LinkedHashMap<>();
    	for (AbstractSuffixValueRestriction r : restrList) {
    		SuffixValue s = r.getParameter();
    		List<AbstractSuffixValueRestriction> list = groups.containsKey(s) ? groups.get(s) : new ArrayList<>();
    		list.add(r);
    		groups.put(s, list);
    	}

    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	for (Map.Entry<SuffixValue, List<AbstractSuffixValueRestriction>> e : groups.entrySet()) {
    		AbstractSuffixValueRestriction dis = DisjunctionRestriction.create(e.getKey(), e.getValue());
    		ret.put(e.getKey(), dis);
    	}
    	return ret;
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    	Map<SuffixValue, List<AbstractSuffixValueRestriction>> groups = group(restrList);
//    	Map<SuffixValue, AbstractSuffixValueRestriction> flattened = flatten(groups);
//    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
//    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> e : restrictions.entrySet()) {
//    		AbstractSuffixValueRestriction dis = flattened.get(e.getKey());
//    		AbstractSuffixValueRestriction con = ConjunctionRestriction.create(e.getKey(), dis, e.getValue());
//    		ret.put(e.getKey(), con);
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

    private static List<Map.Entry<SDTGuard, SDTGuard>> restrictionsSeparatingPaths(SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
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
//    					List<AbstractSuffixValueRestriction> ret = new ArrayList<>();
//    					ret.addAll(EqualityTheory.pathsConjunctionsToRestrictions(sorted1, sorted2, restrictions));

    					List<Map.Entry<SDTGuard, SDTGuard>> ret = new ArrayList<>();
    					Iterator<SDTGuard> pathIt1 = sorted1.iterator();
    					Iterator<SDTGuard> pathIt2 = sorted2.iterator();
    					while (pathIt1.hasNext()) {
    						assert pathIt2.hasNext();
    						ret.add(new SimpleEntry<>(pathIt1.next(), pathIt2.next()));
    					}

    					return ret;
    				}
    			}
    		}
    	}
    	return null;
    }

//    private static List<AbstractSuffixValueRestriction> restrictionsSeparatingPaths(SDT sdt1, SDT sdt2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
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
//    					List<AbstractSuffixValueRestriction> ret = new ArrayList<>();
//    					ret.addAll(EqualityTheory.pathsConjunctionsToRestrictions(sorted1, sorted2, restrictions));
//    					return ret;
//    				}
//    			}
//    		}
//    	}
//    	return null;
//    }

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

    private static List<AbstractSuffixValueRestriction> pathsToRestrictions(List<Map.Entry<SDTGuard, SDTGuard>> paths, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Set<DataValue> missingRegs) {
    	List<AbstractSuffixValueRestriction> ret = new ArrayList<>();
    	for (Map.Entry<SDTGuard, SDTGuard> pathStep : paths) {
//    		ret.add(guardsConjunctionToRestriction(pathStep.getKey(), pathStep.getValue(), restrictions));
    		ret.add(guardsToRestriction(pathStep, restrictions, missingRegs));
    	}
    	return ret;
    }

//    private static List<AbstractSuffixValueRestriction> pathsConjunctionsToRestrictions(List<SDTGuard> path1, List<SDTGuard> path2, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
//    	assert path1.size() == path2.size();
//    	Iterator<SDTGuard> it1 = path1.iterator();
//    	Iterator<SDTGuard> it2 = path2.iterator();
//    	List<AbstractSuffixValueRestriction> ret = new ArrayList<>();
//    	while (it1.hasNext()) {
//    		SDTGuard g1 = it1.next();
//    		SDTGuard g2 = it2.next();
//    		ret.add(guardsConjunctionToRestriction(g1, g2, restrictions));
//    	}
//    	return ret;
//    }

    private static AbstractSuffixValueRestriction guardToRestriction(SDTGuard guard) {
    	if (isElseGuard(guard)) {
    		return new FreshSuffixValue(guard.getParameter());
    	}
    	if (guard instanceof SDTGuard.EqualityGuard geq) {
    		return SuffixValueRestriction.equalityRestriction(geq.getParameter(), geq.register().asExpression());
    	}
    	return new SuffixValueRestriction(guard.getParameter(), SDTGuard.toExpr(guard));
    }

    private static AbstractSuffixValueRestriction guardsToRestriction(Map.Entry<SDTGuard, SDTGuard> guards, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Set<DataValue> missingRegs) {
    	SuffixValue suffixValue = guards.getKey().getParameter();
    	AbstractSuffixValueRestriction rOld = restrictions.get(suffixValue);
    	assert guards.getValue().getParameter().equals(suffixValue);
    	if (guards.getKey() instanceof SDTGuard.SDTTrueGuard && guards.getValue() instanceof SDTGuard.SDTTrueGuard) {
    		return rOld;
    	}
    	if (guards.getKey() instanceof SDTGuard.EqualityGuard geq) {
    		if (missingRegs.contains(geq.register())) {
    			// could be the disjunction Unmappped OR Fresh?
    			return new TrueRestriction(suffixValue);
    		}
    		if (guards.getValue() instanceof SDTGuard.EqualityGuard geqOther) {
    			return SuffixValueRestriction.equalityRestriction(suffixValue, geq.register().asExpression(), geqOther.register().asExpression());
    		}
    		assert isElseGuard(guards.getValue());
    		if (rOld.containsFresh()) {
    			return DisjunctionRestriction.create(suffixValue,
        				SuffixValueRestriction.equalityRestriction(suffixValue, geq.register().asExpression()),
        				new FreshSuffixValue(suffixValue));
    		}
    		return rOld;
    	}
    	assert isElseGuard(guards.getKey());
    	if (guards.getValue() instanceof SDTGuard.EqualityGuard geq) {
    		if (rOld.containsFresh()) {
    			return DisjunctionRestriction.create(suffixValue,
        				SuffixValueRestriction.equalityRestriction(suffixValue, geq.register().asExpression()),
        				new FreshSuffixValue(suffixValue));
    		}
    		return rOld;
    	}
    	assert isElseGuard(guards.getValue());
    	if (rOld.containsFresh()) {
    		return new FreshSuffixValue(suffixValue);
    	}
    	return rOld;
    }

    private static Map<SuffixValue, AbstractSuffixValueRestriction> restrictSDT(SDT sdt, Set<DataValue> regs, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
    	List<List<Map.Entry<SDTGuard, SDTGuard>>> paths = pathsBranchingOnRegisters(new ArrayList<>(), new ArrayList<>(), sdt, regs, restrictions, solver);
    	List<List<AbstractSuffixValueRestriction>> restrLists = new ArrayList<>();
    	for (List<Map.Entry<SDTGuard, SDTGuard>> path : paths) {
    		restrLists.add(pathsToRestrictions(path, restrictions, regs));
    	}
    	Map<SuffixValue, List<AbstractSuffixValueRestriction>> flattenedPaths = new LinkedHashMap<>();
    	for (List<AbstractSuffixValueRestriction> restrList : restrLists) {
    		for (AbstractSuffixValueRestriction r : restrList) {
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
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	for (Map.Entry<SuffixValue, AbstractSuffixValueRestriction> restrictionsEntry : restrictions.entrySet()) {
    		SuffixValue s = restrictionsEntry.getKey();
//    		AbstractSuffixValueRestriction original = restrictionsEntry.getValue();
    		List<AbstractSuffixValueRestriction> disjuncts = flattenedPaths.get(s);
//    		if (sdtHasGuardOnMemorable(sdt, s, regs)) {
//    			ret.put(s, new TrueRestriction(s));
//    		} else {
	    		AbstractSuffixValueRestriction dis = DisjunctionRestriction.create(s, disjuncts);
//	    		AbstractSuffixValueRestriction con = ConjunctionRestriction.create(s, original, dis);
	    		ret.put(s, dis);
//    		}
    	}
    	return ret;
    }

    private static List<List<Map.Entry<SDTGuard, SDTGuard>>> pathsBranchingOnRegisters(List<List<Map.Entry<SDTGuard, SDTGuard>>> paths, List<Map.Entry<SDTGuard, SDTGuard>> path, SDT sdt, Set<DataValue> regs, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
    	List<List<Map.Entry<SDTGuard, SDTGuard>>> ret = new ArrayList<>(paths);
    	if (Collections.disjoint(sdt.getDataValues(), regs)) {
    		return ret;
    	}

    	for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
    		if (!Collections.disjoint(e.getValue().getDataValues(), regs)) {
    			List<Map.Entry<SDTGuard, SDTGuard>> extendedPath = new ArrayList<>(path);
    			extendedPath.add(new SimpleEntry<>(e.getKey(), e.getKey()));
//    			extendedPath.add(guardToRestriction(e.getKey()));
    			ret.addAll(pathsBranchingOnRegisters(paths, extendedPath, e.getValue(), regs, restrictions, solver));
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
    						.filter(EqualityTheory::isElseGuard)
    						.findFirst();
    				if (gelseOpt.isEmpty()) {
    					throw new RuntimeException("Encountered an equality guard with no corresponding else guard");
    				}
    				SDTGuard gelse = gelseOpt.get();
    				SDT sdtEq = e.getValue();
    				SDT sdtElse = sdt.getChildren().get(gelse);

    				// find two separating subpaths
    				List<Map.Entry<SDTGuard, SDTGuard>> sep = restrictionsSeparatingPaths(sdtEq, sdtElse, restrictions, solver);
    				if (sep == null) {
    					// could not separate paths, ignore and keep going
    					continue;
    		    	}

    				List<Map.Entry<SDTGuard, SDTGuard>> fullPath = new ArrayList<>(path);
    				fullPath.add(new SimpleEntry<>(geq, gelse));
    				fullPath.addAll(sep);
    				ret.add(fullPath);

//    				List<AbstractSuffixValueRestriction> fullPath1 = new ArrayList<>(path);
//    				List<AbstractSuffixValueRestriction> fullPath2 = new ArrayList<>(path);
//    				fullPath1.add(new TrueRestriction(geq.getParameter()));
//    				fullPath1.addAll(sep);
//    				fullPath2.add(new TrueRestriction(gelse.getParameter()));
//    				fullPath2.addAll(sep);
//    				ret.add(fullPath1);
//    				ret.add(fullPath2);
    			}
    		}
    	}
    	return ret;
    }

//    private static List<List<AbstractSuffixValueRestriction>> pathsBranchingOnRegisters(List<List<AbstractSuffixValueRestriction>> paths, List<AbstractSuffixValueRestriction> path, SDT sdt, Set<DataValue> regs, Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, ConstraintSolver solver) {
//    	List<List<AbstractSuffixValueRestriction>> ret = new ArrayList<>(paths);
//    	if (Collections.disjoint(sdt.getDataValues(), regs)) {
//    		return ret;
//    	}
//
//    	for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
//    		if (!Collections.disjoint(e.getValue().getDataValues(), regs)) {
//    			List<AbstractSuffixValueRestriction> extendedPath = new ArrayList<>(path);
//    			extendedPath.add(guardToRestriction(e.getKey()));
//    			ret.addAll(pathsBranchingOnRegisters(paths, extendedPath, e.getValue(), regs, restrictions, solver));
//    		}
//    	}
//    	for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
//    		SDTGuard guard = e.getKey();
//    		if (guard instanceof SDTGuard.EqualityGuard geq) {
//    			if (regs.contains(geq.register())) {
//    				// found equality guard, now find corresponding else
//    				Optional<SDTGuard> gelseOpt = sdt.getChildren()
//    						.keySet()
//    						.stream()
//    						.filter(EqualityTheory::isElseGuard)
//    						.findFirst();
//    				if (gelseOpt.isEmpty()) {
//    					break;
//    				}
//    				SDTGuard gelse = gelseOpt.get();
//    				SDT sdtEq = e.getValue();
//    				SDT sdtElse = sdt.getChildren().get(gelse);
//
//    				// find two separating subpaths
//    				List<AbstractSuffixValueRestriction> sep = restrictionsSeparatingPaths(sdtEq, sdtElse, restrictions, solver);
//    				if (sep == null) {
//    		    		throw new RuntimeException("SDTs have no comparable paths with different outcomes");
//    		    	}
//
//    				List<AbstractSuffixValueRestriction> fullPath1 = new ArrayList<>(path);
//    				List<AbstractSuffixValueRestriction> fullPath2 = new ArrayList<>(path);
//    				fullPath1.add(new TrueRestriction(geq.getParameter()));
//    				fullPath1.addAll(sep);
//    				fullPath2.add(new TrueRestriction(gelse.getParameter()));
//    				fullPath2.addAll(sep);
//    				ret.add(fullPath1);
//    				ret.add(fullPath2);
//    			}
//    		}
//    	}
//    	return ret;
//    }

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
    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDTs(SDT sdt1, SDT sdt2, Prefix uExt1, Prefix uExt2, Bijection<DataValue> u1RpBijection, Bijection<DataValue> u2RpBijection, Constants consts, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb1 = uExt1.lastSymbol();
    	PSymbolInstance symb2 = uExt2.lastSymbol();
    	if (!symb1.getBaseSymbol().equals(symb2.getBaseSymbol())) {
    		throw new IllegalArgumentException("One-symbol extensions do not match");
    	}

    	// mappings to the immediate ancestor node of suffix
    	Bijection<DataValue> uExt1AncestorRenaming = new Bijection<>();
    	Bijection<DataValue> uExt2AncestorRenaming = new Bijection<>();
    	uExt1AncestorRenaming.putAll(uExt1.getBijection(uExt1.getPath().getPrior(suffix)));
    	uExt2AncestorRenaming.putAll(uExt2.getBijection(uExt2.getPath().getPrior(suffix)));

    	// find all data values in SDTs and the existing restriction
    	Set<DataValue> vals1 = new LinkedHashSet<>(sdt1.getDataValues());
    	Set<DataValue> vals2 = new LinkedHashSet<>(sdt2.getDataValues());
    	for (Expression<BigDecimal> d : AbstractSuffixValueRestriction.getElements(suffix.getRestrictions())) {
    		if (d instanceof DataValue) {
    			// d is in restriction, so must be known at the immediate ancestor of suffix
    			assert uExt1AncestorRenaming.containsValue(d) && uExt2AncestorRenaming.containsValue(d);
    			vals1.add(uExt1AncestorRenaming.getKey(d));
    			vals2.add(uExt2AncestorRenaming.getKey(d));
    		}
    	}

    	Set<DataValue> unmapped1 = new LinkedHashSet<>(vals1);
    	Set<DataValue> unmapped2 = new LinkedHashSet<>(vals2);
    	unmapped1.removeAll(u1RpBijection.keySet());
    	unmapped2.removeAll(u2RpBijection.keySet());
    	Set<DataValue> mapped1 = new LinkedHashSet<>(vals1);
    	Set<DataValue> mapped2 = new LinkedHashSet<>(vals2);
    	mapped1.removeAll(unmapped1);
    	mapped2.removeAll(unmapped2);

    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = AbstractSuffixValueRestriction.shift(suffix.getRestrictions(), symb1.getBaseSymbol().getArity());
    	// replace pre-existing unmapped restrictions with true restriction
    	ret = replaceUnmappedEqualityRestrictions(ret);

    	// shift SDT suffix parameters
    	sdt1 = sdt1.shift(symb1.getBaseSymbol().getArity());
    	sdt2 = sdt2.shift(symb2.getBaseSymbol().getArity());

    	/*
    	 * Data values in SDTs not known in u1 (and u2) must correspond to data values in the action,
    	 * otherwise register closedness would make them known.
    	 * So rename all unmapped data values to corresponding suffix values in action
    	 */
    	SDT sdt1Mapped = sdt1.relabel(SDTRelabeling.fromMapping(remappingToSuffixValues(unmapped1, symb1)));
    	SDT sdt2Mapped = sdt2.relabel(SDTRelabeling.fromMapping(remappingToSuffixValues(unmapped2, symb2)));
    	ret = replaceUnmappedDataValuesWithSuffixValues(ret, unmapped1, symb1, unmapped2, symb2);

    	/*
    	 * Add remappings data values in u1 and u2 which are not known in immediate ancestor
    	 * of suffix, in order to avoid collisions with other data values of ancestor rp
    	 */
    	addFreshRenamings(uExt1AncestorRenaming, uExt2AncestorRenaming, mapped1, u1RpBijection, u2RpBijection);
    	addFreshRenamings(uExt2AncestorRenaming, uExt1AncestorRenaming, mapped2, u2RpBijection, u1RpBijection);

    	// remap to immediate ancestor for "common ground" between SDTs
    	SDT sdt1Renamed = sdt1Mapped.relabel(SDTRelabeling.fromBijection(uExt1AncestorRenaming));
    	SDT sdt2Renamed = sdt2Mapped.relabel(SDTRelabeling.fromBijection(uExt2AncestorRenaming));

    	// find new restrictions from separating path
    	ret = EqualityTheory.restrictionsSeparatingPathsInSDTs(sdt1Renamed, sdt2Renamed, ret, solver);

    	// add equality with suffix value for each equality in restrictions with value in action
    	List<ElementRestriction> alreadyReplaced = new ArrayList<>();
    	ret = addSuffixValuesToActionEqualities(ret, uExt1AncestorRenaming, symb1, alreadyReplaced);
    	ret = addSuffixValuesToActionEqualities(ret, uExt2AncestorRenaming, symb2, alreadyReplaced);

    	// map restrictions to uExt1
    	return AbstractSuffixValueRestriction.relabel(ret, uExt1AncestorRenaming.inverse().toVarMapping());
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
     * @param missingRegs
     * @param suffix
     * @param solver
     * @return
     */
    public static Map<SuffixValue, AbstractSuffixValueRestriction> restrictionFromSDT(SDT sdt, Prefix uExt, Bijection<DataValue> rp, Constants consts, Set<DataValue> missingRegs, SymbolicSuffix suffix, ConstraintSolver solver) {
    	PSymbolInstance symb = uExt.lastSymbol();

    	// mapping from uExt to the RP of the immediate ancestor node of suffix
    	Bijection<DataValue> renaming = new Bijection<>();
    	renaming.putAll(uExt.getBijection(uExt.getPath().getPrior(suffix)));

    	// find all unmapped data values in SDT
    	Set<DataValue> unmapped = new LinkedHashSet<>(sdt.getDataValues());
    	unmapped.removeAll(rp.keySet());

    	/*
    	 * Add renamings for any data value in the SDT which is not known at the ancestor node
    	 * in order to avoid collisions with other data values of the ancestor node
    	 */
    	addFreshRenamings(renaming, sdt.getDataValues());

    	// translate unmapped data values to the fresh renamings
    	Set<DataValue> unmappedRenamed = new LinkedHashSet<>();
    	unmapped.forEach(d -> unmappedRenamed.add(renaming.get(d)));

    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = AbstractSuffixValueRestriction.shift(suffix.getRestrictions(), symb.getBaseSymbol().getArity());
    	ret = replaceUnmappedEqualityRestrictions(ret);

    	SDT sdtShifted = sdt.shift(symb.getBaseSymbol().getArity());
    	SDT sdtRenamed = sdtShifted.relabel(SDTRelabeling.fromBijection(renaming));
    	ret = restrictSDT(sdtRenamed, unmappedRenamed, ret, solver);

    	// if there are unmapped registers in restriction, replace them with unmapped restriction
    	ret = replaceMissingRegsWithUnmapped(ret, unmapped, renaming);

    	// add suffix values to equality restrictions of values in symbol
    	ret = addSuffixValuesToActionEqualities(ret, renaming, symb, new ArrayList<>());

    	// relabel to uExt
    	return AbstractSuffixValueRestriction.relabel(ret, renaming.inverse().toVarMapping());
    }

    /**
     * Replace data values in {@code restriction} that are present in {@code unmapped1} or
     * {@code unmapped2} with suffix values according to the position of corresponding data
     * values in {@code symbol1} and {@code symbol2}, respectively.
     *
     * @param restrictions
     * @param unmapped1
     * @param symbol1
     * @param unmapped2
     * @param symbol2
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> replaceUnmappedDataValuesWithSuffixValues(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Set<DataValue> unmapped1, PSymbolInstance symbol1, Set<DataValue> unmapped2, PSymbolInstance symbol2) {
    	Mapping<DataValue, SuffixValue> renaming1 = remappingToSuffixValues(unmapped1, symbol1);
    	Mapping<DataValue, SuffixValue> renaming2 = remappingToSuffixValues(unmapped2, symbol2);

    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = AbstractSuffixValueRestriction.relabel(restrictions, renaming1);
    	return AbstractSuffixValueRestriction.relabel(ret, renaming2);
    }

    /**
     * Compute mapping from data values in {@code vals} to suffix values corresponding to
     * data value positions in {@code symbol}. A precondition of this method is that all
     * data values in {@code vals} are in {@code symbol}.
     *
     * @param vals
     * @param symbol
     * @return
     */
    private static Mapping<DataValue, SuffixValue> remappingToSuffixValues(Set<DataValue> vals, PSymbolInstance symbol) {
    	ArrayList<DataValue> actionVals = new ArrayList<>(symbol.getBaseSymbol().getArity());
    	actionVals.addAll(Arrays.asList(symbol.getParameterValues()));
    	Mapping<DataValue, SuffixValue> renaming = new Mapping<>();

    	for (DataValue d : vals) {
    		int id = actionVals.indexOf(d);
    		assert id >= 0 : "Unmapped data value not present in action";

    		SuffixValue s = new SuffixValue(actionVals.get(id).getDataType(), id + 1);
    		renaming.put(d, s);
    	}

    	return renaming;
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

    /**
     * For any data value in {@code mapped} which is not present in the key set of {@code renaming1},
     * add a mapping to {@code renaming1} to a fresh data value, with respect to the values
     * of {@code renaming1}. For each such mapping added, also adds a corresponding mapping
     * to {@code renaming2}, according to the composition of {@code rpBijection1} with the
     * inverse of {@code rpBijection2}.
     *
     * The purpose of the method is to avoid collisions between mapped data values in two
     * prefixes and data values in the RP of an ancestor node to which the two prefixes
     * are being compared.
     *
     * @param renaming1
     * @param renaming2
     * @param mapped
     * @param rpBijection1
     * @param rpBijection2
     */
    private static void addFreshRenamings(Bijection<DataValue> renaming1, Bijection<DataValue> renaming2, Set<DataValue> mapped, Bijection<DataValue> rpBijection1, Bijection<DataValue> rpBijection2) {
    	assert (renaming1.isEmpty() && renaming2.isEmpty()) || !Collections.disjoint(renaming1.values(), renaming2.values()) : "Renamings do not have the same set of values";
    	for (DataValue d1 : mapped) {
    		if (!renaming1.containsKey(d1)) {
    			// not mapped for the representative prefix of the node above the u1 extension
    			DataValue fresh = getFreshValue(renaming1.values(), d1.getDataType());
    			DataValue d2 = rpBijection1.compose(rpBijection2.inverse()).get(d1);
    			assert d2 != null : "No corresponding data value for " + d1;

    			renaming1.put(d1, fresh);
    			renaming2.put(d2, fresh);
    		}
    	}
    }

    /**
     * For any data value in {@code mapped} which is not present in key set of {@code renaming},
     * add a mapping to {@code renaming} to a fresh data value, with respect to the values of
     * {@code renaming}.
     *
     * @param renaming
     * @param mapped
     */
    private static void addFreshRenamings(Bijection<DataValue> renaming, Set<DataValue> mapped) {
    	for (DataValue d : mapped) {
    		if (!renaming.containsKey(d)) {
    			// not mapped for the representative prefix of the node above u extension
    			DataValue fresh = getFreshValue(renaming.values(), d.getDataType());
    			renaming.put(d, fresh);
    		}
    	}
    }

    /**
     * For each equality in {@code restrictions} with a data value present in {@code symbol},
     * add a disjunction with all suffix values corresponding to {@code symbol} which that data
     * value might equal (i.e., any suffix value of the same type).
     *
     * @param restrictions set of suffix value restrictions
     * @param renaming renaming of data values from {@code symbol} to {@restrictions}
     * @param symbol
     * @param alreadyReplaced set of restrictions that have already been handled
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> addSuffixValuesToActionEqualities(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Bijection<DataValue> renaming, PSymbolInstance symbol, List<ElementRestriction> alreadyReplaced) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;
    	List<DataValue> actionVals = Arrays.asList(symbol.getParameterValues());

    	Set<DataValue> restrictionValues = new LinkedHashSet<>();
    	AbstractSuffixValueRestriction.getElements(restrictions)
    		.stream()
    		.filter(e -> e instanceof DataValue)
    		.forEach(e -> restrictionValues.add((DataValue) e));

    	for (DataValue d : restrictionValues) {
    		DataValue dRenamed = renaming.getKey(d);
    		if (dRenamed != null && actionVals.contains(dRenamed)) {
    			for (ElementRestriction er : AbstractSuffixValueRestriction.getRestrictionsOnElement(restrictions, d)) {
    				if (alreadyReplaced.contains(er)) {
    					continue;
    				}
    				assert er instanceof EqualityRestriction : "Encountered incompatible restriction type";
    				EqualityRestriction replace = (EqualityRestriction) er;
    				Set<SDTGuardElement> elems = replace.getGuardElements();
    				elems.addAll(potentiallyEqualSuffixValues(dRenamed, actionVals));
    				EqualityRestriction by = new EqualityRestriction(replace.getParameter(), elems);
    				ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace, by);
    				alreadyReplaced.add(er);
    			}
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

    /**
     * Replace any equality restrictions in {@code restrictions} with data values in
     * {@code missingRegs} with {@link UnmappedEqualityRestriction}.
     *
     * @param restrictions
     * @param missingRegs
     * @param renaming
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> replaceMissingRegsWithUnmapped(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions, Set<DataValue> missingRegs, Bijection<DataValue> renaming) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = restrictions;

    	for (DataValue d : missingRegs) {
    		DataValue dRenamed = renaming.get(d);
    		if (dRenamed != null) {
    			for (ElementRestriction replace : AbstractSuffixValueRestriction.getRestrictionsOnElement(ret, dRenamed)) {
    				AbstractSuffixValueRestriction by = new UnmappedEqualityRestriction(replace.cast().getParameter());
    				ret = AbstractSuffixValueRestriction.replaceRestriction(ret, replace.cast(), by);
    			}
    		}
    	}

    	return ret;
    }

    /**
     * Replace any {@link UnmappedEqualityRestriction} in {@code restrictions} with {@link TrueRestriction}
     *
     * @param restrictions
     * @return
     */
    private static Map<SuffixValue, AbstractSuffixValueRestriction> replaceUnmappedEqualityRestrictions(Map<SuffixValue, AbstractSuffixValueRestriction> restrictions) {
    	Map<SuffixValue, AbstractSuffixValueRestriction> ret = new LinkedHashMap<>();
    	ret.putAll(restrictions);
    	for (SuffixValue suffixValue : AbstractSuffixValueRestriction.unmappedSuffixValues(restrictions)) {
    		ret.put(suffixValue, new TrueRestriction(suffixValue));
    	}
    	return ret;
    }
}
