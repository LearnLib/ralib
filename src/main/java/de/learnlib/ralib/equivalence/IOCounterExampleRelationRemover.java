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
package de.learnlib.ralib.equivalence;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.Slice;
import de.learnlib.ralib.oracles.mto.SliceBuilder;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.automatalib.words.Word;

/**
 * CE optimizer that tries to remove data relations from counterexamples
 * 
 * @author falk
 */
public class IOCounterExampleRelationRemover implements IOCounterExampleOptimizer {
    
    private final SliceBuilder builder;
    
    private final ConstraintSolver solver;
    
    private final Map<DataType, Theory> teachers;

    private final IOOracle sulOracle;
    
    private RegisterAutomaton hypothesis;
    
    private final HypVerifier verifier;
        
    public IOCounterExampleRelationRemover(
            Map<DataType, Theory> teachers,
            Constants constants, 
            ConstraintSolver solver,
            IOOracle sulOracle,
            HypVerifier verifier) {
        
        this.teachers = teachers;
        this.solver = solver;
        this.sulOracle = sulOracle;
        this.verifier = verifier;
        this.builder = new SliceBuilder(teachers, constants, solver);
    }
    
    @Override
    public DefaultQuery<PSymbolInstance, Boolean> optimizeCE(
            Word<PSymbolInstance> ce, Hypothesis hyp) {
    
        this.hypothesis = hyp;

        return new DefaultQuery<>(reduceCe(ce), true);
    }
        
    private Word<PSymbolInstance> reduceCe(Word<PSymbolInstance> ce) {    

        Map<Integer, DataValue> dvals = new HashMap<>();
        int idx = 1;
        for (DataValue v : DataWords.valsOf(ce)) {
            dvals.put(idx++, v);
        }
        
        for (int i=1; i<= dvals.size(); i++) {
            Map<Integer, DataValue> instMap = new HashMap<>(dvals);
            DataType type = dvals.get(i).getType();
            Theory theory = teachers.get(type);            
            DataValue fresh = theory.getFreshValue(theory.getPotential(
                    new ArrayList<>(DataWords.valSet(ce, type))));

            instMap.put(i, fresh);           
            Word<PSymbolInstance> cand = DataWords.instantiate(DataWords.actsOf(ce), instMap);
            System.out.println(cand);            
            Word<PSymbolInstance> candidate = sulOracle.trace(cand);
            
            System.out.println("---");
            System.out.println("CE " + builder.sliceFromWord(Word.epsilon(), ce));
            System.out.println("CA " + builder.sliceFromWord(Word.epsilon(), candidate));
            
            if (builder.sliceFromWord(Word.epsilon(), ce).equals(
                    builder.sliceFromWord(Word.epsilon(), candidate))) {
                System.out.println("eq");
                continue;
            }
            
           //System.out.println(candidate);
            if (this.verifier.isCEForHyp(candidate, hypothesis) != null) {
                //System.out.println("Found Prefix CE!!!");
                return reduceCe(candidate);
            }            
        }
                        
        return ce;
    }
    
    public Map<Integer, DataValue> instanciateSlice(Slice slice, DataType ... types) {
        SuffixValueGenerator sgen = new SuffixValueGenerator();
        List<SuffixValue> svals = new ArrayList<>();
        Map<Integer, DataValue> retMap = new HashMap<>();        
        int idx = 1;
        for (DataType t : types) {
            Theory theory = teachers.get(t);
            SuffixValue sv = sgen.next(t);            
            Map<SuffixValue, EnumSet<DataRelation>> relMap = new HashMap<>();
            for (SuffixValue psv : svals) {
                EnumSet<DataRelation> rels = slice.getSuffixRelationsFor(psv, sv);
                if (rels.isEmpty()) {
                    rels = EnumSet.of(DataRelation.DEFAULT);
                }
                for (DataRelation r : rels) {
                    System.out.println("   " + psv + " " + r + " " + sv);
                }
                relMap.put(psv, rels);
            }
            
            svals.add(sv);
            idx++;
        }        
        return retMap;
    }
    
}
