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
package de.learnlib.ralib.learning;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.FreshSuffixValue;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.theory.equality.EqualRestriction;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * A symbolic suffix is a sequence of actions with
 * a constraint over the parameters in the sequence.
 *
 * @author falk
 */
public class SymbolicSuffix {

    /**
     * actions
     */
    private final Word<ParameterizedSymbol> actions;

    /**
     * restrictions on suffix values
     */
    private final Map<SuffixValue, SuffixValueRestriction> restrictions;

    /**
     * are generic suffix optimizations (fresh, equal to prior suffix value or unrestricted) used
     */
    private boolean genericOptimizations = true;

    public SymbolicSuffix(Word<PSymbolInstance> prefix,
            Word<PSymbolInstance> suffix) {
        this(prefix, suffix, new Constants());
    }

    public SymbolicSuffix(SymbolicSuffix s) {
    	genericOptimizations = s.genericOptimizations;
    	actions = Word.fromWords(s.actions);
    	restrictions = new LinkedHashMap<>();

    	for (Map.Entry<SuffixValue, SuffixValueRestriction> r : s.restrictions.entrySet())
    		restrictions.put(r.getKey(), r.getValue());
    }

    /**
     * creates a symbolic suffix from a prefix and a suffix
     * data word.
     * Shared data values between prefix and suffix become
     * free values. Equalities between data values in the
     * suffix data word are only preserved for un-free data
     * values. Preserving equalities for free data values
     * would lead to undesired effects (Falk).
     *
     * @param prefix
     * @param suffix
     * @param consts
     */
    public SymbolicSuffix(Word<PSymbolInstance> prefix,
            Word<PSymbolInstance> suffix, Constants consts) {

        this.actions = DataWords.actsOf(suffix);

        this.restrictions = new LinkedHashMap<>();

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        for (DataValue dv : DataWords.valsOf(suffix)) {
        	SuffixValue sv = svgen.next(dv.getType());
        	SuffixValueRestriction restriction = SuffixValueRestriction.genericRestriction(sv, prefix, suffix, consts);
        	restrictions.put(sv, restriction);
        }
    }

    /**
     * creates a symbolic suffix from a prefix and a suffix
     * data word.
     * Relations between data values will be optimized
     * according to theory
     *
     * @param prefix
     * @param suffix
     * @param restrictionBuilder - assigns restrictions on suffix values according to the theory
     */
    public SymbolicSuffix(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, SymbolicSuffixRestrictionBuilder restrictionBuilder) {
    	this.genericOptimizations = false;
    	this.actions = DataWords.actsOf(suffix);
        this.restrictions = restrictionBuilder.restrictSuffix(prefix, suffix);
    }


    public SymbolicSuffix(ParameterizedSymbol ps) {
        this(Word.fromSymbols(ps));
    }


    public SymbolicSuffix(Word<ParameterizedSymbol> actions) {
        this.actions = actions;
        this.restrictions = new LinkedHashMap<>();

        SuffixValueGenerator valgen = new SuffixValueGenerator();
        int idx = 1;
        for (ParameterizedSymbol ps : actions) {
            for (DataType t : ps.getPtypes()) {
                SuffixValue sv = valgen.next(t);
                restrictions.put(sv, new UnrestrictedSuffixValue(sv));
            }
        }
    }

    public SymbolicSuffix(Word<PSymbolInstance> prefix,
            SymbolicSuffix symSuffix) {
        this(prefix, symSuffix, new Constants());
    }

    public SymbolicSuffix(Word<PSymbolInstance> prefix,
            SymbolicSuffix symSuffix, Constants consts) {

        this.actions = symSuffix.actions.prepend(
                DataWords.actsOf(prefix).lastSymbol());

        this.restrictions = new LinkedHashMap<>();

        Word<PSymbolInstance> suffix = prefix.suffix(1);
        prefix = prefix.prefix(prefix.length() - 1);

        SuffixValueGenerator svgen = new SuffixValueGenerator();
        for (DataValue dv : DataWords.valsOf(suffix)) {
        	SuffixValue sv = svgen.next(dv.getType());
        	SuffixValueRestriction restriction = SuffixValueRestriction.genericRestriction(sv, prefix, suffix, consts);
        	restrictions.put(sv, restriction);
        }

        int actionArity = suffix.firstSymbol().getBaseSymbol().getArity();
        for (Map.Entry<SuffixValue, SuffixValueRestriction> e : symSuffix.restrictions.entrySet()) {
        	SuffixValue sv = e.getKey();
        	SuffixValueRestriction restriction = e.getValue();
        	SuffixValue s = new SuffixValue(sv.getType(), sv.getId()+actionArity);
        	restrictions.put(s, restriction.shift(actionArity));
        }
    }

    public SymbolicSuffix(Word<PSymbolInstance> prefix, SymbolicSuffix symSuffix, SymbolicSuffixRestrictionBuilder restrictionBuilder) {

    	this.genericOptimizations = false;
        this.actions = symSuffix.actions.prepend(
                DataWords.actsOf(prefix).lastSymbol());

        Word<PSymbolInstance> suffix = prefix.suffix(1);
        prefix = prefix.prefix(prefix.length() - 1);
        this.restrictions = restrictionBuilder.restrictSuffix(prefix, suffix);

        int actionArity = suffix.firstSymbol().getBaseSymbol().getArity();
        for (Map.Entry<SuffixValue, SuffixValueRestriction> e : symSuffix.restrictions.entrySet()) {
        	SuffixValue sv = e.getKey();
        	SuffixValueRestriction restriction = e.getValue();
        	SuffixValue s = new SuffixValue(sv.getType(), sv.getId()+actionArity);
        	restrictions.put(s, restriction.shift(actionArity));
        }
    }

    public SymbolicSuffix(Word<ParameterizedSymbol> actions, Map<Integer, SuffixValue> dataValues,
			Set<SuffixValue> freeValues) {
    	this.actions = actions;
    	this.restrictions = new LinkedHashMap<>();

    	Set<SuffixValue> seen = new LinkedHashSet<>();
    	for (Map.Entry<Integer, SuffixValue> e : dataValues.entrySet()) {
    		SuffixValue sv = e.getValue();
    		SuffixValue suffixValue = new SuffixValue(sv.getType(), e.getKey());
    		if (freeValues.contains(sv)) {
    			restrictions.put(suffixValue, new UnrestrictedSuffixValue(suffixValue));
    			seen.add(sv);
    		} else if (seen.contains(sv)) {
    			int id = dataValues.entrySet()
   			         .stream().filter((a) -> (a.getValue().equals(sv)))
   			         .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
   			         .findFirst()
   			         .get()
   			         .getKey();
    			SuffixValue equalSV = new SuffixValue(suffixValue.getType(), id);
    			restrictions.put(suffixValue, new EqualRestriction(suffixValue, equalSV));
    		} else {
    			restrictions.put(suffixValue, new FreshSuffixValue(suffixValue));
    			seen.add(sv);
    		}
    	}
	}

    public SymbolicSuffix(Word<ParameterizedSymbol> actions, Map<SuffixValue, SuffixValueRestriction> restrictions) {
    	this.genericOptimizations = false;
    	this.actions = actions;
    	this.restrictions = restrictions;
    }

    public SuffixValueRestriction getRestriction(SuffixValue sv) {
    	return restrictions.get(sv);
    }

    public SuffixValue getSuffixValue(int i) {
    	for (SuffixValue sv : restrictions.keySet()) {
    		if (sv.getId() == i)
    			return sv;
    	}
    	return null;
    }

	public SuffixValue getDataValue(int i) {
		return (SuffixValue)restrictions.keySet().toArray()[i-1];
    }

	public Set<SuffixValue> getDataValues() {
		return restrictions.keySet();
	}

    public Set<SuffixValue> getFreeValues() {
    	Set<SuffixValue> freeValues = new LinkedHashSet<>();
    	for (Map.Entry<SuffixValue, SuffixValueRestriction> restr : restrictions.entrySet()) {
    		if (restr.getValue() instanceof UnrestrictedSuffixValue)
    			freeValues.add(restr.getKey());
    	}
    	return freeValues;
    }

    public Set<SuffixValue> getValues() {
    	LinkedHashSet<SuffixValue> suffixValues = new LinkedHashSet<>(restrictions.keySet());
        return suffixValues;
    }

    public Word<ParameterizedSymbol> getActions() {
        return actions;
    }

    public SymbolicSuffix concat(SymbolicSuffix other) {

    	Word<ParameterizedSymbol> actions = this.getActions().concat(other.actions);
    	Map<SuffixValue, SuffixValueRestriction> concatRestr = new LinkedHashMap<>();
    	int arity = restrictions.size();
    	concatRestr.putAll(restrictions);
    	for (Map.Entry<SuffixValue, SuffixValueRestriction> e : other.restrictions.entrySet()) {
    		SuffixValue sv = new SuffixValue(e.getKey().getType(), e.getKey().getId()+arity);
    		SuffixValueRestriction restr = e.getValue().shift(arity);
    		concatRestr.put(sv, restr);
    	}
    	return new SymbolicSuffix(actions, concatRestr);
    }

    public int length() {
    	return actions.length();
    }

    public boolean isOptimizationGeneric() {
    	return genericOptimizations;
    }

    @Override
    public String toString() {
        Word<PSymbolInstance> dw =
        		DataWords.instantiate(actions, restrictions.keySet());
    	return "((" + dw.toString() + "))" + Arrays.toString(restrictions.values().toArray());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SymbolicSuffix other = (SymbolicSuffix) obj;
        if (this.restrictions != other.restrictions && (this.restrictions == null || !this.restrictions.equals(other.restrictions))) {
        	return false;
        }
        if (this.actions != other.actions && (this.actions == null || !this.actions.equals(other.actions))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.restrictions != null ? this.restrictions.hashCode() : 0);
        hash = 37 * hash + (this.actions != null ? this.actions.hashCode() : 0);
        return hash;
    }

}
