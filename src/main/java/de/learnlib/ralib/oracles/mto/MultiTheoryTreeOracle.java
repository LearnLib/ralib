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
package de.learnlib.ralib.oracles.mto;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryBranching.Node;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryTreeOracle implements TreeOracle, SDTConstructor {

    private final DataWordOracle oracle;

    private final Constants constants;

    private final Map<DataType, Theory> teachers;

    private final ConstraintSolver solver;
    
    private static LearnLogger log
            = LearnLogger.getLogger(MultiTheoryTreeOracle.class);

    public MultiTheoryTreeOracle(DataWordOracle oracle,
            Map<DataType, Theory> teachers, Constants constants, 
            ConstraintSolver solver) {
        this.oracle = oracle;
        this.teachers = teachers;
        this.constants = constants;
        this.solver = solver;
    }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        PIV pir = new PIV();
        SDT sdt = treeQuery(prefix, suffix,
                new WordValuation(), pir, constants, new SuffixValuation());
        
//        System.out.println(prefix + " . " + suffix);
//        System.out.println(sdt);
        
        // move registers to 1 ... n
        VarMapping rename = new VarMapping();
        RegisterGenerator gen = new RegisterGenerator();
        Set<Register> regs = sdt.getRegisters();
        PIV piv = new PIV();

        for (Entry<Parameter, Register> e : pir.entrySet()) {
            if (regs.contains(e.getValue())) {
                Register r = e.getValue();
                rename.put(r, gen.next(r.getType()));
                piv.put(e.getKey(), (Register) rename.get(r));
            }
        }

        TreeQueryResult tqr = new TreeQueryResult(piv, sdt.relabel(rename));
        log.finer("PIV: " + piv);

        return tqr;
    }

    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values, PIV pir,
            Constants constants,
            SuffixValuation suffixValues) {
        
//        System.out.println("prefix = " + prefix + "   suffix = " + suffix + "    values = " + values);

        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                    suffix.getActions(), values);

            Word<PSymbolInstance> trace = prefix.concat(concSuffix);
            DefaultQuery<PSymbolInstance, Boolean> query
                    = new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            boolean qOut = query.getOutput();

//            System.out.println("Trace = " + trace.toString() + " >>> "
//                    + (qOut ? "ACCEPT (+)" : "REJECT (-)"));
            return qOut ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;

            // return accept / reject as a leaf
        }

        // OTHERWISE get the first noninstantiated data value in the suffix and its type
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);

        Theory teach = teachers.get(sd.getType());

        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        return teach.treeQuery(prefix, suffix, values, pir,
                constants, suffixValues, this);
    }

    /**
     * This method computes the initial branching for an SDT. It re-uses
     * existing valuations where possible.
     *
     */
    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree... sdts) {

        log.log(Level.INFO,
                "computing initial branching for {0} after {1}",
                new Object[]{ps, prefix});

        //TODO: check if this casting can be avoided by proper use of generics
        //TODO: the problem seems to be 
        //System.out.println("using " + sdts.length + " SDTs");
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
            if (sdts[i] instanceof SDTLeaf) {
                casted[i] = (SDTLeaf) sdts[i];
            } else {
                casted[i] = (SDT) sdts[i];
            }
        }

        MultiTheoryBranching mtb = getInitialBranching(
                prefix, ps, piv, new ParValuation(),
                new ArrayList<SDTGuard>(), casted);

        log.log(Level.FINEST, mtb.toString());

        return mtb;
    }

    @Override
    // get the initial branching for the symbol ps after prefix given a certain tree
    public MultiTheoryBranching getInitialBranching(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            List<SDTGuard> guards, SDT... sdts) {
        Node n;

        if (sdts.length == 0) {
            n = createFreshNode(1, prefix, ps, piv, pval);
            return new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, sdts);
        } else {
            n = createNode(1, prefix, ps, piv, pval, sdts);
            MultiTheoryBranching fluff = new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, sdts);
            return fluff;
        }

    }


    private Set<SDTGuard> getFinestGuards(Map<SDTGuard, SDTGuard> gMap) {
        Set<SDTGuard> retSet = new LinkedHashSet<>();
        for (SDTGuard s : gMap.keySet()) {
            if (!gMap.containsValue(s)) {
                retSet.add(s);
            }

        }
        return retSet;
    }

    // returns a map of which guards refine each other
    // (the entry <g1,g2> means that g1 refines g2
    private Map<SDTGuard, SDTGuard> mapGuards(
            Set<SDTGuard> unmapped, Parameter param) {

        Set<SDTGuard> guards = new LinkedHashSet<>();
        guards.addAll(unmapped);
        SDTGuard g = new SDTTrueGuard(
                new SuffixValue(param.getType(), param.getId()));

        SDTGuard finer = g;
        SDTGuard coarser = g;
        Map<SDTGuard, SDTGuard> refines = new LinkedHashMap<>();
        MultiTheorySDTLogicOracle mlo
                = new MultiTheorySDTLogicOracle(constants, solver);
        for (SDTGuard n : guards) {
            if (mlo.doesRefine(g.toTG(), new PIV(), n.toTG(), new PIV())
                    && (!mlo.doesRefine(n.toTG(), new PIV(),
                            g.toTG(), new PIV()))) {
                finer = g;
                coarser = n;

            } else if (mlo.doesRefine(n.toTG(), new PIV(), g.toTG(), new PIV())
                    && (!mlo.doesRefine(g.toTG(), new PIV(),
                            n.toTG(), new PIV()))) {
                finer = n;
                coarser = g;
            }
            refines.put(finer, coarser);
        }

        log.log(Level.FINEST,
                "!!!!!!! refines " + refines.toString());
        return refines;
    }

    private Map<SDTGuard, SDT> collectKids(SDT... sdts) {
        Map<SDTGuard, SDT> allKids = new LinkedHashMap<>();
        for (SDT sdt : sdts) {
            if (sdt != null && !sdt.getChildren().isEmpty()) {
                for (Map.Entry<SDTGuard, SDT> e
                        : sdt.getChildren().entrySet()) {
                    allKids.put(e.getKey(), e.getValue());
                }
            }
        }
        return allKids;
    }

    private List<SDTGuard> getAllCoarser(Map<SDTGuard, SDTGuard> guardChain) {
        Set<SDTGuard> retSet = accGuards(guardChain);

        return new ArrayList<>(retSet);
    }

    private Set<SDTGuard> accGuards(Map<SDTGuard, SDTGuard> guardChain) {
        Set<SDTGuard> retSet = new LinkedHashSet<>();
        boolean flag = false;
        for (Map.Entry<SDTGuard, SDTGuard> e : guardChain.entrySet()) {
            if (retSet.contains(e.getKey())) {
                retSet.add(e.getValue());
                flag = true;
            }
        }
        if (flag == true) {
            return accGuards(guardChain);
        } else {
            return retSet;
        }
    }

    private Node createFreshNode(int i, Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps,
            PIV piv, ParValuation pval) {

        if (i == ps.getArity() + 1) {
            return new Node(new Parameter(null, i));
        } else {
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            DataType type = ps.getPtypes()[i - 1];
            log.log(Level.FINEST, "current type: " + type.getName());
            Parameter p = new Parameter(type, i);
            SDTGuard guard = new SDTTrueGuard(new SuffixValue(type, i));
            Theory teach = teachers.get(type);

            DataValue dvi = teach.instantiate(prefix, ps, piv, pval,
                    constants, guard, p, new LinkedHashSet<>());
            ParValuation otherPval = new ParValuation();
            otherPval.putAll(pval);
                otherPval.put(p, dvi);

            nextMap.put(dvi, createFreshNode(i + 1, prefix,
                    ps, piv, otherPval));

            guardMap.put(dvi, guard);
            return new Node(p, nextMap, guardMap);
        }
    }
    
    private Node createNode(int i, Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            SDT... sdts) {
        Node n = createNode(i, prefix, ps, piv, pval, new LinkedHashMap(), sdts);
        return n;
    }
    
    private Node createNode(int i, Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps,
            PIV piv, ParValuation pval, Map<Parameter, Set<DataValue>> oldDvs, 
            SDT... sdts) {

        if (i == ps.getArity() + 1) {
            return new Node(new Parameter(null, i));
        } else {

            // obtain the data type, teacher, parameter
            DataType type = ps.getPtypes()[i - 1];
            Theory teach = teachers.get(type);
            Parameter p = new Parameter(type, i);

            int numSdts = sdts.length;
            // initialize maps for next nodes
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            // set current SDT
            SDT curr = sdts[0];
            // populate array of next sdts (nxt may be of length 0)
            SDT[] nxt = new SDT[numSdts - 1];
            for (int y = 1; y < numSdts; y++) {
                nxt[y - 1] = sdts[y];
            }
            
            
            // get the children of the current sdt (this may be empty)
            Map<SDTGuard, SDT> currChildren = curr.getChildren();
            // get the children of the next sdt (this may be empty)
            Map<SDTGuard, SDT> nxtChildren = collectKids(nxt);
            Map<SDTGuard, SDT> allChildren = new LinkedHashMap<>();
            if (currChildren != null) {
            allChildren.putAll(currChildren);
            allChildren.putAll(nxtChildren);
            }
            // initialize set of finest guards; initialize list of coarser guards
            Set<SDTGuard> finest = new LinkedHashSet<>();

            // populate the map of which guards refine each other (may be empty)
            Map<SDTGuard, SDTGuard> refines
                    = mapGuards(allChildren.keySet(), p);
            SDTGuard c = new SDTTrueGuard(
                    new SuffixValue(p.getType(), p.getId()));
            finest = getFinestGuards(refines);
            if (finest.isEmpty()) {
                finest.add(c);
            }

            for (SDTGuard guard : finest) {
                // initialize list of coarser guards
                // add all guards that are coarser than this one
                // always add the true guard
                List<SDTGuard> coarser = new ArrayList<>();
                coarser.add(new SDTTrueGuard(
                        new SuffixValue(p.getType(), p.getId())));
                coarser.addAll(getAllCoarser(refines));
                
                DataValue dvi = null;
                
//                System.out.println(oldDvs.toString());
                if (oldDvs.containsKey(p)) {
                    dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, oldDvs.get(p));
                } else {
                    dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, new LinkedHashSet<>());
                }
                
               
                //if (dvi == null) {
                
                // instantiate the parameter p according to guard and values
                //dvi = teach.instantiate(prefix, ps, piv,
                //        pval, constants, guard, p);
                //}
                
                // add instantiated value to new ParValuation
                ParValuation otherPval = new ParValuation();
                otherPval.putAll(pval);
                    otherPval.put(p, dvi);

                // initialize set of sdts
                // try to find the sdt that this guard maps to
                Set<SDT> nextLevelSdts = new LinkedHashSet<>();
                SDT cSdt = null;
                if (currChildren != null)
                	cSdt = currChildren.get(guard);
                // if we can find it in the 'current' sdt, then add it
                if (cSdt != null) {
                    nextLevelSdts.add(cSdt);
                } else {
                    SDT nSdt = nxtChildren.get(guard);
                    if (nSdt != null) {
                        nextLevelSdts.add(nSdt);
                    }
                }

//                System.out.println("next level sdts: " + nextLevelSdts.toString());
                // at this point, nextLevelSdts must contain exactly one sdt
                //    assert nextLevelSdts.size() == 1;
                // now, we add the sdts from the coarser guards
                for (SDTGuard coarserGuard : coarser) {
                    SDT xSdt = nxtChildren.get(coarserGuard);
                    if (xSdt != null) {
                        nextLevelSdts.add(xSdt);
                    }
                }

                SDT[] newSdts = nextLevelSdts.toArray(
                        new SDT[nextLevelSdts.size()]);

                nextMap.put(dvi, createNode(i + 1, prefix, ps, piv,
                        otherPval, newSdts));
                guardMap.put(dvi, guard);
            }
            log.log(Level.FINEST, "guardMap: " + guardMap.toString());
            log.log(Level.FINEST, "nextMap: " + nextMap.toString());
            assert !nextMap.isEmpty();
            assert !guardMap.isEmpty();
            return new Node(p, nextMap, guardMap);

        }

    }
    
    public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix,
    		SymbolicSuffix suffix, SymbolicDecisionTree sdt, PIV piv) {
    	
    	assert (sdt instanceof SDT);
    	Map<Word<PSymbolInstance>, Boolean> words = new LinkedHashMap<Word<PSymbolInstance>, Boolean>();
    	instantiate(words,
    			prefix, suffix, (SDT)sdt, piv,
    			new ParValuation(), 0,
    			new SuffixValueGenerator(),
    			new ParameterGenerator());
    	return words;
    }
    
    private void instantiate(Map<Word<PSymbolInstance>, Boolean> words,
    		Word<PSymbolInstance> prefix, SymbolicSuffix suffix, SDT sdt, PIV piv,
    		ParValuation pval,
    		int index,
    		SuffixValueGenerator sgen, ParameterGenerator pgen) {
    	
    	if (sdt instanceof SDTLeaf) {
    		assert (index == suffix.length());
    		
    		SDTLeaf sdtLeaf = (SDTLeaf)sdt;
    		words.put(prefix, sdtLeaf.isAccepting());
    	}
    	else {
        	for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
            	SuffixValueGenerator sgenClone = new SuffixValueGenerator();
            	ParameterGenerator pgenClone = new ParameterGenerator();
            	ParValuation pvalClone = new ParValuation();
            	sgenClone.set(sgen);
            	pgenClone.set(pgen);
            	pvalClone.putAll(pval);
        		
        		instantiate(words, prefix, suffix,
        				e.getValue(),
        				piv, pvalClone,
        				e.getKey(),
        				index, sgenClone, pgenClone);
        	}
    	}
    }
    
    private void instantiate(Map<Word<PSymbolInstance>, Boolean> words,
    		Word<PSymbolInstance> prefix, SymbolicSuffix suffix, SDT sdt, PIV piv,
    		ParValuation pval,
    		SDTGuard guard,
    		int index,
    		SuffixValueGenerator sgen,
    		ParameterGenerator pgen) {
    	
    	ParameterizedSymbol ps = suffix.getActions().getSymbol(index);
    	DataValue[] dvis = new DataValue[ps.getArity()];
    	for (int i = 0; i < ps.getArity(); i++) {
    		DataType type = ps.getPtypes()[i];
    		Theory teach = teachers.get(type);
    		
    		SuffixValue s = sgen.next(type);
    		Parameter p = new Parameter(type, s.getId());
    		
    		DataValue dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, new LinkedHashSet<>());
    		dvis[i] = dvi;
    		
    		pval.put(p, dvi);
    	}
    	
    	Word<PSymbolInstance> newPrefix = prefix.append(new PSymbolInstance(ps, dvis));
    	// pass on prefix and sdt
    	instantiate(words, newPrefix, suffix, sdt, piv, pval, index+1, sgen, pgen);
    }
    
    /**
     * This method computes the initial branching for an SDT. It re-uses
     * existing valuations where possible.
     *
     */
    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, Branching current,
            PIV piv, SymbolicDecisionTree... sdts) {
        
        MultiTheoryBranching oldBranching = (MultiTheoryBranching) current;
        
        Map<Parameter, Set<DataValue>> oldDvs = oldBranching.getDVs();
        
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
            if (sdts[i] instanceof SDTLeaf) {
                casted[i] = (SDTLeaf) sdts[i];
            } else {
                casted[i] = (SDT) sdts[i];
            }
        }
        
        ParValuation pval = new ParValuation();
        
        Node n;

        if (casted.length == 0) {
            n = createFreshNode(1, prefix, ps, piv, pval);
            return new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, casted);
        } else {
            n = createNode(1, prefix, ps, piv, pval, oldDvs, casted);
            MultiTheoryBranching fluff = new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, casted);
            return fluff;
        }
        
//        Map<Word<PSymbolInstance>, TransitionGuard> newBranches
//                = newBranching.getBranches();
//        PIV updatedPiv = new PIV();
//        updatedPiv.putAll(oldBranching.getPiv());
        
//        Branching updatedBranching = getInitialBranching(
//                oldBranching.getPrefix(), ps, piv, sdts);
//        
//        log.log(Level.FINEST, ".... where the new branches are: ");
////        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e
//                : newBranches.entrySet()) {
//            log.log(Level.FINEST, e.toString());
//        }  
        //return updatedBranching;
    }

}
