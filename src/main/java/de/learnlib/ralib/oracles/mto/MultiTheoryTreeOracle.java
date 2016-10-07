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
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryBranching.Node;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTOrGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

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
    
    private Map<SDTGuard, SDTGuard> mapGuardsMod(
            Set<SDTGuard> unmapped, Parameter param) {

        Set<SDTGuard> guards = new LinkedHashSet<>();
        guards.addAll(unmapped);

        Map<SDTGuard, SDTGuard> refines = new LinkedHashMap<>();
        MultiTheorySDTLogicOracle mlo
                = new MultiTheorySDTLogicOracle(constants, solver);
        for (SDTGuard g: guards) {
            SDTGuard finer = g;
            SDTGuard coarser = new SDTTrueGuard(
                    new SuffixValue(param.getType(), param.getId()));
;
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
            allChildren.putAll(currChildren);
            allChildren.putAll(nxtChildren);
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
                SDT cSdt = currChildren.get(guard);
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
            n = createNodeSp(1, prefix, ps, piv, pval, oldDvs, casted);
            MultiTheoryBranching fluff = new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, casted);
            return fluff;
        }
    }

    private Node createNodeSp(int i, Word<PSymbolInstance> prefix,
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

            List<Map<SDTGuard, SDT>> allChildren = Stream.of(sdts).map(sdt -> sdt.getChildren()).collect(Collectors.toList());
            Map<SDTGuard, Set<SDTGuard>> mergedGuards = getNewGuards(allChildren, 
            		guard -> teach.instantiate(prefix, ps, piv, pval, constants, guard, p, new LinkedHashSet<>()) != null);
            Map<SDTGuard, List<SDT>> nextSDTs = getChildren(allChildren);
            
            for (Map.Entry<SDTGuard, Set<SDTGuard>> mergedGuardEntry : mergedGuards.entrySet()) {
            	SDTGuard guard = mergedGuardEntry.getKey();
            	Set<SDTGuard> oldGuards = mergedGuardEntry.getValue();
            	DataValue dvi = null;
            	if (oldDvs.containsKey(p)) {
                     dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, oldDvs.get(p));
                } else {
                      dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, new LinkedHashSet<>());
                }
            	
            	SDT [] nextLevelSDTs = oldGuards.stream().map(g -> nextSDTs.get(g)).
            			flatMap(g -> g.stream()).distinct().toArray(SDT []::new);
            	
            	 ParValuation otherPval = new ParValuation();
                 otherPval.putAll(pval);
                     otherPval.put(p, dvi);

                nextMap.put(dvi, createNode(i + 1, prefix, ps, piv,
                          otherPval, nextLevelSDTs));
                guardMap.put(dvi, guard);
            }
            
            log.log(Level.FINEST, "guardMap: " + guardMap.toString());
            log.log(Level.FINEST, "nextMap: " + nextMap.toString());
            assert !nextMap.isEmpty();
            assert !guardMap.isEmpty();
            return new Node(p, nextMap, guardMap);
        }
    }
    
    
    // produces a mapping from refined sdt guards to the top level sdt guards from which they are built. Multiple top level sdt guards 
    // can be combined to form a refined guard, hence each refined guard maps to a list of top level sdt guards.
    private Map<SDTGuard, Set<SDTGuard>> getNewGuards(List<Map<SDTGuard, SDT>> sdtChildren, Predicate<SDTGuard> instantiationTest) {
    	List<Set<SDTGuard>> guardGroups = sdtChildren.stream().map(sdtChild -> sdtChild.keySet()).collect(Collectors.toList());
    	Map<SDTGuard, Set<SDTGuard>> mergedGroup =   new LinkedHashMap<>();
    	MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(constants, solver);
    	for (Set<SDTGuard> nextGuardGroup : guardGroups) {
    		mergedGroup = combineGroups(mergedGroup, nextGuardGroup, mlo, instantiationTest);
    	}
    	return mergedGroup;
    }
    
    
    
    private Map<SDTGuard, Set<SDTGuard>> combineGroups(Map<SDTGuard, Set<SDTGuard>> mergedHead , Set<SDTGuard> nextGroup,  MultiTheorySDTLogicOracle mlo, Predicate<SDTGuard> instantiationPred) {
    	Map<SDTGuard, Set<SDTGuard>> mergedGroup = new LinkedHashMap<>();
    	if (mergedHead.isEmpty()) {
    		nextGroup.forEach(next -> {
    			mergedGroup.put(next, Sets.newHashSet(next));	
    		});
    		return mergedGroup;
    	}
    	for (SDTGuard next : nextGroup) {
    		for (SDTGuard head : mergedHead.keySet()) {
    			List<SDTGuard> oldGuards = Arrays.asList(head, next);
    			Set<SDTGuard> newGuards = new LinkedHashSet<>(); 
    			if (head.equals(next)) 
    				mergedGroup.put(head, mergedHead.get(head));
    			else {
    				if (mergedGroup.values().stream().anyMatch(list -> list.containsAll(oldGuards))) {
    					// if it was already covered, skip
    					continue;
    				}
    				
    				newGuards = merge(head, next, instantiationPred);
    				
    				newGuards.forEach(newGuard -> {
    					mergedGroup.putIfAbsent(newGuard, new LinkedHashSet<>());
    					mergedGroup.get(newGuard).addAll(oldGuards);
    				});
    			}
    		}
    	}
    	return mergedGroup;
    }
    
    private Set<SDTGuard> merge(SDTGuard head, SDTGuard next, Predicate<SDTGuard> instPred) {
    	Set<SDTGuard> newGuards = new LinkedHashSet<>(); 
    	if (head instanceof SDTTrueGuard) {
			newGuards.add(next);
		} else if (next instanceof SDTTrueGuard) {
			newGuards.add(head);
		} else 
			if (canBeMerged(head, next, instPred)) {
				if (refines(next, head, instPred)) {
					if (refines(head, next, instPred)) {
						newGuards.add(next); // always add next, as it might be more compact
					} else {
						newGuards.add(head);
						newGuards.add(new SDTAndGuard(head.getParameter(), next, head.negate()));
					}
				} else 
					if (refines(next, head, instPred)) {
						newGuards.add(next);
						newGuards.add(new SDTAndGuard(head.getParameter(), head, next.negate()));
					} else {
						newGuards.add(new SDTAndGuard(head.getParameter(), head, next.negate()));
						newGuards.add(new SDTAndGuard(head.getParameter(), head.negate(), next));
					}
		}
    	
    	return newGuards;
    }
    
    private boolean canBeMerged(SDTGuard a, SDTGuard b, Predicate<SDTGuard> instantiationPred) {
    	return instantiationPred.test(new SDTAndGuard(a.getParameter(), a, b));
    }
    
    private boolean refines(SDTGuard a, SDTGuard b, Predicate<SDTGuard> instantiationPred) {
    	return !instantiationPred.test(new SDTAndGuard( a.getParameter(), a, b.negate()));
    }
    

	// produces a mapping from top level SDT guards to the next level SDTs. Since the same guard can appear 
    // in multiple SDTs, the guard maps to a list of SDTs
    private Map<SDTGuard, List<SDT>> getChildren(List<Map<SDTGuard, SDT>> sdtChildren) {
    	Map<SDTGuard, List<SDT>> children = new LinkedHashMap<>();
    	for (Map<SDTGuard, SDT> child : sdtChildren) {
    		child.forEach((guard, nextSdt) -> {
    			children.putIfAbsent(guard, new ArrayList<>());
    			children.get(guard).add(nextSdt);
    			});
    	}
    	
    	return children;
    }

}
