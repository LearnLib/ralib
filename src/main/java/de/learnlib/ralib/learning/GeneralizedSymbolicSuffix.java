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

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.theory.DataRelation;
import static de.learnlib.ralib.theory.DataRelation.DEFAULT;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class GeneralizedSymbolicSuffix {
    
    private final Word<ParameterizedSymbol> actions;
    
    private final SuffixValue[] suffixValues;
    
    private final EnumSet<DataRelation>[][] suffixRelations;
    
    private final EnumSet<DataRelation>[] prefixRelations; 
    
    
    public GeneralizedSymbolicSuffix(Word<PSymbolInstance> prefix, 
            Word<PSymbolInstance> suffix, Constants consts,
            Map<DataType, Theory> theories) {
                
        this.actions = DataWords.actsOf(suffix);        
        DataValue[] concSuffixVals = DataWords.valsOf(suffix);   
        this.suffixValues = new SuffixValue[concSuffixVals.length];
        this.prefixRelations = new EnumSet[concSuffixVals.length];
        this.suffixRelations = new EnumSet[concSuffixVals.length][];
                
        SymbolicDataValueGenerator.SuffixValueGenerator valgen = 
                new SymbolicDataValueGenerator.SuffixValueGenerator();
        
        int idx = 0;
        Map<DataType, List<DataValue>> groups = new HashMap<>();
        for (DataValue v: concSuffixVals) {
            this.suffixValues[idx] = valgen.next(v.getType());
            EnumSet<DataRelation> prefixRels = EnumSet.noneOf(DataRelation.class);
            
            // find relations to previous suffix values
            Theory t = theories.get(v.getType());
            List<DataValue> prevSuffixValues = groups.get(v.getType());
            if (prevSuffixValues == null) {
                prevSuffixValues = new ArrayList<>();
                groups.put(v.getType(), prevSuffixValues);
            }
            
            List<DataValue> pvals = Arrays.asList(DataWords.valsOf(prefix, v.getType()));
            List<DataValue> cvals = new ArrayList<>(consts.values(v.getType()));
            List<EnumSet<DataRelation>> prels = t.getRelations(pvals, v);
            List<EnumSet<DataRelation>> crels = t.getRelations(cvals, v);
            List<EnumSet<DataRelation>> srels = t.getRelations(prevSuffixValues, v);
            prefixRels.add(DataRelation.DEFAULT);
            if (prefix.length() == 0) {
                prefixRels.addAll(t.recognizedRelations());
            }
            prels.stream().forEach((rels) -> { prefixRels.addAll(rels); });
            crels.stream().forEach((rels) -> { prefixRels.addAll(rels); });            
            int lidx = 0;
            for (EnumSet<DataRelation> srel : srels) {
                // FIXME: not sure if this sufficient or if all prev. relations needed
                if (srel.isEmpty()) {
                    srel.add(DEFAULT);
                }
            }

            this.prefixRelations[idx] = prefixRels;
            this.suffixRelations[idx] = srels.toArray(new EnumSet[] {});
            prevSuffixValues.add(v);
            idx++;
        }        
    }    

    public GeneralizedSymbolicSuffix(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix symSuffix, 
            Constants consts, Map<DataType, Theory> theories) {
        
        // create suffix for prefix
        this.actions = symSuffix.actions.prepend(
                DataWords.actsOf(prefix).lastSymbol());
        
        Word<PSymbolInstance> suffix = prefix.suffix(1);
        prefix = prefix.prefix(prefix.length() - 1);        
        
        GeneralizedSymbolicSuffix pSuffix = new GeneralizedSymbolicSuffix(
                prefix, suffix, consts, theories);
        
        // System.out.println("pSuffix: " + pSuffix);
        // System.out.println("symSuffix: " + symSuffix);
        
        int psLength = DataWords.valsOf(suffix).length;
        int ssLength = symSuffix.suffixValues.length;
                
        this.suffixValues = new SuffixValue[psLength + ssLength];
        this.prefixRelations = new EnumSet[psLength + ssLength];
        this.suffixRelations = new EnumSet[psLength + ssLength][];
                
        SymbolicDataValueGenerator.SuffixValueGenerator valgen = 
                new SymbolicDataValueGenerator.SuffixValueGenerator();
        
        for (int i=0; i<psLength; i++) {
            this.suffixValues[i] = valgen.next(pSuffix.suffixValues[i].getType());
            this.prefixRelations[i] = pSuffix.prefixRelations[i];
            this.suffixRelations[i] = pSuffix.suffixRelations[i];
        }
        
        for (int i=0; i<ssLength; i++) {
            this.suffixValues[psLength + i] = 
                    valgen.next(symSuffix.suffixValues[i].getType());
            
            this.prefixRelations[psLength + i] = symSuffix.prefixRelations[i];
            
            int sameTypePrefix = DataWords.valsOf(suffix, 
                    this.suffixValues[psLength + i].getType()).length;
            
            int sameTypeSuffix = symSuffix.suffixRelations[i].length;
            
            this.suffixRelations[psLength + i] = 
                    new EnumSet[sameTypePrefix + sameTypeSuffix];
            
            Arrays.fill(this.suffixRelations[psLength + i], 0, sameTypePrefix, EnumSet.of(DEFAULT));
            
            // FIXME: do we need do clone enumsets?            
            System.arraycopy(symSuffix.suffixRelations[i], 0, 
                this.suffixRelations[psLength + i], sameTypePrefix, sameTypeSuffix);
        }
    }
        
    public GeneralizedSymbolicSuffix(ParameterizedSymbol ps, Map<DataType, Theory> theories) {
        
        Map<Integer, DataValue> dvs = new HashMap<>();
        int idx = 1;
        for (DataType t : ps.getPtypes()) {
            DataValue dv = theories.get(t).getFreshValue(new ArrayList<>());
            // FIXME: maybe we have to keep track of generated values
            dvs.put(idx++, dv);
        }
        Word<PSymbolInstance> inst = DataWords.instantiate(Word.fromSymbols(ps), dvs);
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                inst, inst, new Constants(), theories);
                
        this.actions = Word.fromSymbols(ps);
        this.suffixValues = symSuffix.suffixValues;
        this.prefixRelations = symSuffix.prefixRelations;
        this.suffixRelations = symSuffix.suffixRelations;
        for (int i=0; i<ps.getArity(); i++) {
            DataType t = ps.getPtypes()[i];
            this.prefixRelations[i] = theories.get(t).recognizedRelations();
        }        
    }
    
    public SuffixValue getValue(int i) {
        return suffixValues[i-1];
    }
            
    public Word<ParameterizedSymbol> getActions() {
        return actions;
    }
    
    public EnumSet<DataRelation> getPrefixRelations(int i) {
        return prefixRelations[i-1];
    }
    
    public EnumSet<DataRelation> getSuffixRelations(int i, int j) {
        DataType t = suffixValues[j-1].getType();
        // have to count types to convert i
        int idx = -1;
        for (int c=0; c<i; c++) {
            if (t.equals(suffixValues[c].getType())) {
                idx++;
            }
        }
        //System.out.println(i + "(" + idx+ ") : " + j);        
        return suffixRelations[j-1][idx];
    }

    @Override
    public String toString() {
        Map<Integer, DataValue> instValues = new HashMap<>();
        int id = 1;
        for (DataValue v : suffixValues) {
            instValues.put(id++, v);
        }
        String suffixString = DataWords.instantiate(actions, instValues).toString();
        suffixString += "_P" + Arrays.deepToString(prefixRelations);
        suffixString += "_S" + Arrays.deepToString(suffixRelations);
        return suffixString;
    }

    public SymbolicDataValue.SuffixValue getDataValue(int i) {
        return suffixValues[i-1];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeneralizedSymbolicSuffix other = (GeneralizedSymbolicSuffix) obj;
        if (!Objects.equals(this.actions, other.actions)) {
            return false;
        }
        if (!Arrays.deepEquals(this.suffixValues, other.suffixValues)) {
            return false;
        }
        if (!Arrays.deepEquals(this.suffixRelations, other.suffixRelations)) {
            return false;
        }
        if (!Arrays.deepEquals(this.prefixRelations, other.prefixRelations)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.actions);
        hash = 41 * hash + Arrays.deepHashCode(this.suffixValues);
        hash = 41 * hash + Arrays.deepHashCode(this.suffixRelations);
        hash = 41 * hash + Arrays.deepHashCode(this.prefixRelations);
        return hash;
    }
  
}
