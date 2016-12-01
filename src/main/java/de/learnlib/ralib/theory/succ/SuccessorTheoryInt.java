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
package de.learnlib.ralib.theory.succ;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.DefaultGuardLogic;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SuccessorTheoryInt implements TypedTheory<Integer>{

    private DataType type = null;
    
    private boolean useNonFreeOptimization = true;
    
    public DataType<Integer> getType(){
    	return type;
    }
    
    @Override
    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
        int max = 0;        
        for (DataValue<Integer> i : vals) {
            max = Math.max(max, i.getId());
        }
        
        return new DataValue<>(type, max + 2);
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix, 
            WordValuation values, PIV pir, Constants constants, 
            SuffixValuation suffixValues, SDTConstructor oracle, IOOracle traceOracle) {

        System.out.println("SuccessorTheoryInt.treeQuery()");
        
        System.out.println("Prefix: " + prefix);
        System.out.println("Sym. Suffix: " + suffix);
        System.out.println("Word Val.: " + values);
        System.out.println("PiR: " + pir);
        System.out.println("Constants: " + constants);
        System.out.println("Suffix Vals.: " + suffixValues);
        
        // position determines current parameter
        int pId = values.size() + 1;
        SuffixValue sv = suffix.getDataValue(pId);
        DataType type = sv.getType();
        SuffixValue currentParam = new SuffixValue(type, pId);

        // compute potential
        Collection<DataValue<Integer>> potSet = DataWords.joinValsToSet(
                constants.values(type),
                DataWords.valSet(prefix, type),
                suffixValues.values(type));

        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public DataValue instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, 
            PIV piv, ParValuation pval, Constants constants, SDTGuard guard, 
            SymbolicDataValue.Parameter param, Set<DataValue<Integer>> oldDvs, boolean useSolver) {
        
        System.out.println("SuccessorTheoryInt.instantiate()");
        
        System.out.println("Prefix: " + prefix);
        System.out.println("Symbol: " + ps);
        System.out.println("PiR: " + piv);
        System.out.println("Par Val.: " + pval);
        System.out.println("Constants: " + constants);
        System.out.println("SDT Guard: " + guard);
        System.out.println("Parameter: " + param);        
        System.out.println("Known Vals.: " + oldDvs);
        
        if (guard instanceof SDTTrueGuard) {
            Collection<DataValue<Integer>> potSet = DataWords.<Integer>joinValsToSet(
                    constants.values(type),
                    DataWords.valSet(prefix, type),
                    pval.values(type));            
            
            return getFreshValue(new ArrayList<>(potSet));
        }
        
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public void setUseSuffixOpt(boolean useit) {
        System.err.println("Suffix Optimization currently not implemented for theory " + 
                this.getClass().getName());
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit) {
        System.err.println("Check for fresh outputs currently not implemented for theory " + 
                this.getClass().getName());
    }

    @Override
    public Collection<DataValue<Integer>> getAllNextValues(List<DataValue<Integer>> vals) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    
	@Override
	public List<EnumSet<DataRelation>> getRelations(List<DataValue<Integer>> left, DataValue<Integer> right) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EnumSet<DataRelation> recognizedRelations() {
		// TODO Auto-generated method stub
		return null;
	}

    static List<SuccessorMinterms[]> generateMinterms(int size) {        
        List<SuccessorMinterms[]> list = new ArrayList<>();
        int[] intMT = new int[size];
        Arrays.fill(intMT, 0);
        do {
            list.add(asMinterms(intMT));
            
        }  while (next(intMT));
        return list;
    }

    private static final int LIMIT = 4;

    private static boolean next(int[] cur) {        
        for (int i=0; i<cur.length; i++) {
            if (cur[i] < LIMIT) {
                cur[i]++;
                return true;
            } else {
                cur[i] = 0;
            }            
        }
        return false;
    }

    private static SuccessorMinterms[] asMinterms(int[] intMT) {
        SuccessorMinterms[] mt = new SuccessorMinterms[intMT.length];
        for (int i=0; i<mt.length; i++) {
            mt[i] = SuccessorMinterms.forInt(intMT[i]);
        }
        return mt;
    }

	public SDTGuardLogic getGuardLogic() {
		return new DefaultGuardLogic();
	}
    
    
    
}
