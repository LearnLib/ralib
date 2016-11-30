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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.ConstantGuardExpression;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheorySDTLogicOracle implements SDTLogicOracle {

    private final ConstraintSolver solver;
    
    private final Constants consts;
    
    private static LearnLogger log = LearnLogger.getLogger(MultiTheorySDTLogicOracle.class);

    public MultiTheorySDTLogicOracle(Constants consts, ConstraintSolver solver) {
        this.solver = solver;
        this.consts = consts;
    }    
    
    @Override
    public boolean hasCounterexample(Word<PSymbolInstance> prefix, 
            SymbolicDecisionTree sdt1, PIV piv1, SymbolicDecisionTree sdt2, PIV piv2, 
            TransitionGuard guard, Word<PSymbolInstance> rep) {
        
        //Collection<SymbolicDataValue> join = piv1.values();
        
        log.finest("Searching for counterexample in SDTs");
        log.log(Level.FINEST, "SDT1: {0}", sdt1);
        log.log(Level.FINEST, "SDT2: {0}", sdt2);
        log.log(Level.FINEST, "Guard: {0}", guard);
        
        SDT _sdt1 = (SDT) sdt1;
        SDT _sdt2 = (SDT) sdt2;
        
        GuardExpression expr1 = _sdt1.getAcceptingPaths(consts);
        GuardExpression expr2 = _sdt2.getAcceptingPaths(consts);        
        GuardExpression exprG = guard.getCondition();

        VarMapping<SymbolicDataValue, SymbolicDataValue> gremap = 
                new VarMapping<>();
        for (SymbolicDataValue sv : exprG.getSymbolicDataValues()) {
            if (sv instanceof Parameter) {
                gremap.put(sv, new SuffixValue(sv.getType(), sv.getId()));
            }
        }
        
        exprG = exprG.relabel(gremap);
        
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(piv2, piv1);
        
        GuardExpression expr2r = expr2.relabel(remap);
        
        GuardExpression left = new Conjunction(
                exprG, expr1, new Negation(expr2r));
        
        GuardExpression right = new Conjunction(
                exprG, expr2r, new Negation(expr1));
        
        GuardExpression test = new Disjunction(left, right);

        boolean r = solver.isSatisfiable(test);
        log.log(Level.FINEST,"Res:" + r);
        return r;
    }

    @Override
    public boolean doesRefine(TransitionGuard refining, PIV pivRefining, 
            TransitionGuard refined, PIV pivRefined) {
        
        log.log(Level.FINEST, "refining: {0}", refining);
        log.log(Level.FINEST, "refined: {0}", refined);
        log.log(Level.FINEST, "pivRefining: {0}", pivRefining);
        log.log(Level.FINEST, "pivRefined: {0}", pivRefined);
        
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(pivRefined, pivRefining);
        
        GuardExpression exprRefining = refining.getCondition();
        GuardExpression exprRefined = 
                refined.getCondition().relabel(remap);
        
        // is there any case for which refining is true but refined is false?
        GuardExpression test = new Conjunction(
            exprRefining, new Negation(exprRefined));
        
        log.log(Level.FINEST,"MAP: " + remap);
        log.log(Level.FINEST,"TEST:" + test);        
                
        boolean r = solver.isSatisfiable(test);
        return !r;       
    }
    
    public boolean doesRefine(GuardExpression refining, PIV pivRefining, 
            GuardExpression refined, PIV pivRefined) {
        
        log.log(Level.FINEST, "refining: {0}", refining);
        log.log(Level.FINEST, "refined: {0}", refined);
        log.log(Level.FINEST, "pivRefining: {0}", pivRefining);
        log.log(Level.FINEST, "pivRefined: {0}", pivRefined);
        
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(pivRefined, pivRefining);
        
        GuardExpression exprRefining = refining;
        GuardExpression exprRefined = 
                refined.relabel(remap);
        
        // is there any case for which refining is true but refined is false?
        GuardExpression test = new Conjunction(
            exprRefining, new Negation(exprRefined));
        
        log.log(Level.FINEST,"MAP: " + remap);
        log.log(Level.FINEST,"TEST:" + test);        
                
        boolean r = solver.isSatisfiable(test);
        return !r;       
    }
    
    public boolean doesRefine(TransitionGuard refining, PIV pivRefining, 
            TransitionGuard refined, PIV pivRefined, Mapping<? extends SymbolicDataValue, DataValue<?>> contextMapping) {
    	GuardExpression refiningConjunction = this.augmentGuardWithContext(refining.getCondition(), contextMapping);
    	GuardExpression refinedConjunction = this.augmentGuardWithContext(refined.getCondition(), contextMapping);
    	
    	return this.doesRefine(refiningConjunction, pivRefining, refinedConjunction, pivRefined);
    }
    
    
    public boolean canBothBeSatisfied(TransitionGuard refining, PIV pivRefining, 
            TransitionGuard refined, PIV pivRefined) {
        boolean r = this.canBothBeSatisfied(refining.getCondition(), pivRefining, refined.getCondition(), pivRefined);
        return r;       
    }
    
    public boolean canBothBeSatisfied(GuardExpression refining, PIV pivRefining, 
            GuardExpression refined, PIV pivRefined) {
        
        log.log(Level.FINEST, "refining: {0}", refining);
        log.log(Level.FINEST, "refined: {0}", refined);
        log.log(Level.FINEST, "pivRefining: {0}", pivRefining);
        log.log(Level.FINEST, "pivRefined: {0}", pivRefined);
        
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(pivRefined, pivRefining);
        
        GuardExpression exprRefining = refining;
        GuardExpression exprRefined = 
                refined.relabel(remap);
        
        // is there any case for which refining is true but refined is false?
        GuardExpression test = new Conjunction(
            exprRefining, exprRefined);
        
        log.log(Level.FINEST,"MAP: " + remap);
        log.log(Level.FINEST,"TEST:" + test);        
                
        boolean r = solver.isSatisfiable(test);
        return r;       
    }
    

    public boolean canBothBeSatisfied(TransitionGuard refining, PIV pivRefining, 
            TransitionGuard refined, PIV pivRefined, Mapping<? extends SymbolicDataValue, DataValue<?>> contextValuation) {
    	GuardExpression refiningConjunction = this.augmentGuardWithContext(refining.getCondition(), contextValuation);
    	GuardExpression refinedConjunction = this.augmentGuardWithContext(refined.getCondition(), contextValuation);
    	
    	return canBothBeSatisfied(refiningConjunction, pivRefining, refinedConjunction, pivRefined);
    }
    
    
    private GuardExpression augmentGuardWithContext(GuardExpression guardExpresion, Mapping<? extends SymbolicDataValue, DataValue<?>> contextValuation) {
    	if (contextValuation.isEmpty())
    		return guardExpresion;
    	GuardExpression [] contextGuards = buildConstantExpressions(contextValuation);
    	GuardExpression [] allGuards = Arrays.copyOf(contextGuards, contextGuards.length + 1);
    	allGuards[contextGuards.length] = guardExpresion;
    	return new Conjunction(allGuards);
    }
    
    
    private GuardExpression [] buildConstantExpressions(Mapping<? extends SymbolicDataValue, DataValue<?>> contextMapping) {
    	List<GuardExpression> fixedValues = new ArrayList<GuardExpression> (contextMapping.size());
    	contextMapping.forEach((var,dv) 
    			-> fixedValues.add(new ConstantGuardExpression(var, dv)));
    	return fixedValues.toArray(new GuardExpression[]{});
    }
    
    public boolean areEquivalent(TransitionGuard refining, PIV pivRefining, 
            TransitionGuard refined, PIV pivRefined) {
        
        boolean ret = 
        		doesRefine(refining, pivRefining, refined, pivRefined) && doesRefine(refined, pivRefined, refining, pivRefining);
        return ret;       
    }
    
    private VarMapping<SymbolicDataValue, SymbolicDataValue> createRemapping(
            PIV from, PIV to) {
                
        // there should not be any register with id > n
        for (Register r : to.values()) {
            if (r.getId() > to.size()) {
                throw new IllegalStateException("there should not be any register with id > n: " + to);
            }
        }
        
        VarMapping<SymbolicDataValue, SymbolicDataValue> map = new VarMapping<>();
        
        int id = to.size() + 1;
        for (Entry<Parameter, Register> e : from) {
            Register rep = to.get(e.getKey());
            if (rep == null) {
                rep = new Register(e.getValue().getType(), id++);
            }
            map.put(e.getValue(), rep);
        }
        
        return map;
    }

    
    /* 
     * Generates a symbolic suffix by stamping all relations in the sdt onto the symbolic suffix. This doesn't work all the time,
     * particularly when the hyp contains a refinement not included in the sut: 
     * HYP: []-+
			 []-TRUE: s1
			        []-(s2=s1)
			         |    [Leaf-]
			         +-(s2!=s1)
			              [Leaf+]
			
			SUL: []-+
			  []-TRUE: s1
			        []-TRUE: s2
			              [Leaf+]
     */
    public GeneralizedSymbolicSuffix suffixForCounterexample(SymbolicDecisionTree sutSdt, Word<ParameterizedSymbol> actions) {
    	SDT sdt = (SDT) sutSdt;
    	
    	SDT relabeledSdt = relabelPrefixesWithSuffixes(sdt);
    	System.out.println("RELABELED SDT: " + relabeledSdt);
    	if (!sdt.isEquivalent(relabeledSdt, new VarMapping())) {
    		System.out.println("DIFF SDT: " + relabeledSdt);
    	}
    	List<List<SDTGuard>> accPaths = relabeledSdt.getPaths(Collections.emptyList(), true);
    	List<List<SDTGuard>> rejPaths = relabeledSdt.getPaths(Collections.emptyList(), false);
    	List<List<SDTGuard>> allPaths = new ArrayList<>(accPaths.size() + rejPaths.size());
    	allPaths.addAll(accPaths);
    	allPaths.addAll(rejPaths);
    	int pathSize = allPaths.get(0).size();
        EnumSet<DataRelation>[] prels = new EnumSet[pathSize];
        EnumSet<DataRelation>[][] srels = new EnumSet[pathSize][];
    	for (int i=0; i<pathSize; i++) {
    		final int idx = i;
    		Stream<SDTGuard> guardStream = allPaths.stream().map(path -> path.get(idx));
    		Set<AtomicGuardExpression> atomicGuards = allPaths
    				.stream().map(path -> path.get(idx))
    				.map(guard -> guard.toExpr()).distinct()
    				.map(gExp -> gExp.getAtoms()).flatMap(ats -> ats.stream())
    				.collect(Collectors.toSet());
    	      srels[i] = new EnumSet[i];
              prels[i] = prefixRelations(atomicGuards);
              suffixRelations(srels[i], atomicGuards);
            
    	}
    	
    	
    	
    	return new GeneralizedSymbolicSuffix(actions, prels, srels);
    }
    
    /**
     * Relabeles  sdts so that registers appearing in equality expressions are replaced by the suffixes to which
     * they bound in the context of these expressions. 
     */
    private SDT relabelPrefixesWithSuffixes(SDT sutSdt) {
    	if (sutSdt instanceof SDTLeaf)
    		return sutSdt;
    	Map<SDTGuard, SDT> children = sutSdt.getChildren();
    	Map<SDTGuard, SDT> newChildren = new LinkedHashMap<SDTGuard, SDT>();
    	for (Map.Entry<SDTGuard, SDT> entry : children.entrySet()) {
    		SDTGuard guard = entry.getKey();
    		SDT sdt = entry.getValue();
    		if (guard instanceof EqualityGuard) {
    			EqualityGuard equGuard = (EqualityGuard) guard;
    			SymbolicDataValue reg = equGuard.getRegister();
    			if (equGuard.isEqualityWithSDV() && (reg.isRegister() || reg.isConstant())) {
    				VarMapping<SymbolicDataValue, SymbolicDataValue> mapping = new VarMapping<>();
    				mapping.put(reg, equGuard.getParameter());
    				sdt = (SDT) sdt.relabel(mapping);
    			} 
    		}
    		SDT relabeledSdt = relabelPrefixesWithSuffixes(sdt);
    		newChildren.put(guard, relabeledSdt);
    	}
    	return new SDT(newChildren);
    }
    
    private EnumSet<DataRelation> getInitialBranchingRelations(SDT sutSdt, SDT hypSdt) {
    	if (sutSdt instanceof SDTLeaf || sutSdt.isEmpty() && hypSdt.isEmpty())
    		return EnumSet.noneOf(DataRelation.class);
    	else {
    		List<GuardExpression> sutGuards = sutSdt.getChildren().keySet()
    				.stream().map(guard -> guard.toExpr()).collect(Collectors.toList());
    		List<GuardExpression> hypGuards = hypSdt.getChildren().keySet()
    				.stream().map(guard -> guard.toExpr()).collect(Collectors.toList());
    		Set<GuardExpression> refinedGuards = new LinkedHashSet<>();
    		PIV empty = new PIV();
    		for (GuardExpression sguard : sutGuards) {
    			for (GuardExpression hguard : hypGuards) {
    				if (canBothBeSatisfied(sguard, empty, hguard, empty)) {
    					if (implies(sguard, hguard))
    						refinedGuards.add(sguard);
    					else
    						if (implies(hguard, sguard))
    							refinedGuards.add(hguard);
    						else {
    							refinedGuards.add(hguard);
    							refinedGuards.add(sguard);
    						}
    				}
    			}
    		}
    		
    		assert !solver.isSatisfiable(new Conjunction(refinedGuards.toArray(new GuardExpression[]{})));

    		final EnumSet<DataRelation> rels = EnumSet.noneOf(DataRelation.class);
    		refinedGuards.stream().map(gexp -> gexp.getAtoms())
    		.map(atoms -> prefixRelations(atoms)).forEach(atRels -> rels.addAll(atRels));
    		return rels;
    	}
    }
    
    @Override
    public GeneralizedSymbolicSuffix suffixForCounterexample(
            Word<PSymbolInstance> prefix, SymbolicDecisionTree hypSdt, PIV hypPiv, 
            SymbolicDecisionTree sutSdt, PIV sutPiv, 
            TransitionGuard guard, Word<ParameterizedSymbol> actions) {

        log.log(Level.FINEST,"suffixForCounterexample ------------------------------");
        log.log(Level.FINEST,"Prefix: " + prefix);
        log.log(Level.FINEST,"Guard: " + guard);
        log.log(Level.FINEST,"Actions: " + actions);
        log.log(Level.FINEST,"PIV1: " + hypPiv);
        log.log(Level.FINEST,"SDT1: " + hypSdt);
        log.log(Level.FINEST,"PIV2: " + sutPiv);
        log.log(Level.FINEST,"SDT2: " + sutSdt);        
        log.log(Level.FINEST,"------------------------------------------------------");        
        
        SDT _sdt1 = (SDT) hypSdt;
        SDT _sdt2 = (SDT) sutSdt;
        
        // get all the paths
        List<Conjunction> expr1_T = _sdt1.getPathsAsExpressions(consts, true);
        List<Conjunction> expr2_T = _sdt2.getPathsAsExpressions(consts, true);                
        List<Conjunction> expr1_F = _sdt1.getPathsAsExpressions(consts, false);
        List<Conjunction> expr2_F = _sdt2.getPathsAsExpressions(consts, false); 
        
        // get guard and relabel ...
        GuardExpression exprG = guard.getCondition();
        VarMapping<SymbolicDataValue, SymbolicDataValue> gremap = 
                new VarMapping<>();
        for (SymbolicDataValue sv : exprG.getSymbolicDataValues()) {
            if (sv instanceof Parameter) {
                gremap.put(sv, new SuffixValue(sv.getType(), sv.getId()));
            }
        }
        exprG = exprG.relabel(gremap);
        
        // remapping between sdts
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(sutPiv, hypPiv);
        
        // find disagreeing paths in the two sdts which are satisfiable under conjunction 
        // (one path refines the other)
        for (Conjunction e1 : expr1_T) {
            if (!solver.isSatisfiable(new Conjunction(exprG, e1))) {
                continue;
            }            
//            if (expr2_F.isEmpty()) {
//                // found counterexample slice
//                GuardExpression[] e2r = new GuardExpression[e1.getConjuncts().length];
//                Arrays.fill(e2r, TrueGuardExpression.TRUE);
//                return counterExampleFromSlice(e1, new Conjunction(e2r), actions);
//            }
            for (Conjunction e2 : expr2_F) {
                Conjunction e2r = e2.relabel(remap);
                if (solver.isSatisfiable(new Conjunction(exprG, e1, e2r))) {
                    // found counterexample slice
                    return counterExampleFromSlice(e1, e2r, actions);
                }
            }            
        }
        
        for (Conjunction e1 : expr1_F) {
            if (!solver.isSatisfiable(new Conjunction(exprG, e1))) {
                continue;
            }            
//            if (expr2_T.isEmpty()) {
//                // found counterexample slice
//                GuardExpression[] e2r = new GuardExpression[e1.getConjuncts().length];
//                Arrays.fill(e2r, TrueGuardExpression.TRUE);
//                return counterExampleFromSlice(e1, new Conjunction(e2r), actions);
//            }
            for (Conjunction e2 : expr2_T) {
                Conjunction e2r = e2.relabel(remap);
                if (solver.isSatisfiable(new Conjunction(exprG, e1, e2r))) {
                    // found counterexample slice
                    return counterExampleFromSlice(e1, e2r, actions);
                }
            }              
        }
               
        throw new IllegalStateException("Could not find CE slice");
    }

    private GeneralizedSymbolicSuffix counterExampleFromSlice(
            Conjunction e1, Conjunction e2, Word<ParameterizedSymbol> actions) {
        
        System.out.println("-----------------------------------------------");  
        System.out.println("Actions: " + actions);
        System.out.println("Path 1: " + e1);
        System.out.println("Path 2: " + e2);      
    
        EnumSet<DataRelation>[] prels = new EnumSet[e1.getConjuncts().length];
        EnumSet<DataRelation>[][] srels = new EnumSet[e1.getConjuncts().length][];
        
        int idx = 0;
        int base = 0;
        boolean first = true;
        
        for (int i=0; i< e1.getConjuncts().length; i++) {
            
            while (i >= base + actions.getSymbol(idx).getArity()) {
                base += actions.getSymbol(idx).getArity();
                idx++;
            }
            ParameterizedSymbol ps = actions.getSymbol(idx);
            
            GuardExpression c1 = e1.getConjuncts()[i];
            GuardExpression c2 = e2.getConjuncts()[i];

            Collection<AtomicGuardExpression> atoms1 = c1.getAtoms();
            Collection<AtomicGuardExpression> atoms2 = c2.getAtoms();     
            
            srels[i] = new EnumSet[i];
            
//            prels[i] = prefixRelations(allAtoms);
//            if (ps instanceof OutputSymbol) {
//                prels[i].add(DataRelation.DEFAULT);
//                prels[i].add(DataRelation.EQ);
//            }
//            suffixRelations(srels[i], allAtoms);
            if (implies(c1, c2) && !implies(c2, c1)) {
                // use c1                
                prels[i] = prefixRelations(atoms1);
                suffixRelations(srels[i], atoms1);
            } 
            else if (!implies(c1, c2) && implies(c2, c1)) {
                // use c2
                prels[i] = prefixRelations(atoms2);
                suffixRelations(srels[i], atoms2);
            }
            else  if (!implies(c1, c2) && !implies(c2, c1)) { 
                Collection<AtomicGuardExpression> allAtoms = new HashSet<AtomicGuardExpression>(atoms1);
                allAtoms.addAll(atoms2);
            	// use both
                prels[i] = prefixRelations(allAtoms);
                suffixRelations(srels[i], allAtoms);
            } else {
                // equivalent - use both or does not matter?
                prels[i] = prefixRelations(atoms1);
                suffixRelations(srels[i], atoms1);           
            }
        }
        prels[0] = EnumSet.of(DataRelation.ALL);
        GeneralizedSymbolicSuffix suffix = 
                new GeneralizedSymbolicSuffix(actions, prels, srels);
        
        System.out.println("New suffix: " + suffix);
        
        return suffix;
    }

    private boolean implies(GuardExpression left, GuardExpression right) {
        return !solver.isSatisfiable(new Conjunction(left, new Negation(right)));
    }
    
    private EnumSet<DataRelation> prefixRelations(
            Collection<AtomicGuardExpression> es) {
        
        EnumSet<DataRelation> ret = EnumSet.noneOf(DataRelation.class);
        for (AtomicGuardExpression e : es) {
            if (!(e.getLeft() instanceof SuffixValue) || 
                    !(e.getRight() instanceof SuffixValue)) {
                ret.addAll(toDR(e.getRelation()));
            }
        }
        
//        if (ret.isEmpty())
//        	ret.add(DataRelation.DEFAULT);
//        
        return ret;
    }

    private void suffixRelations(EnumSet<DataRelation>[] srels, 
            Collection<AtomicGuardExpression> es) {
        
        Arrays.fill(srels, EnumSet.noneOf(DataRelation.class));   
        for (AtomicGuardExpression e : es) {
            if (e.getLeft() instanceof SuffixValue && 
                    e.getRight() instanceof SuffixValue) {
                int idx = Math.min(e.getLeft().getId(), e.getRight().getId()) -1;
                
                srels[idx].addAll(toDR(e.getRelation()));
            }
        }
        
//        for (EnumSet<DataRelation> srel : srels) {
//        	if (srel.isEmpty())
//        		srel.add(DataRelation.DEFAULT);
//        }
    }
    
    private EnumSet<DataRelation> toDR(Relation rel) {
        switch (rel) {
            case EQUALS: return EnumSet.of(DataRelation.EQ);
            case LESSER: return EnumSet.of(DataRelation.LT);
            case GREATER: return EnumSet.of(DataRelation.GT);
            case LSREQUALS: return EnumSet.of(DataRelation.LT, DataRelation.EQ);
            case GREQUALS: return EnumSet.of(DataRelation.GT, DataRelation.EQ);            
            case NOT_EQUALS: return EnumSet.of(DataRelation.DEQ);
            default:
                throw new IllegalStateException("Unsupported Relation: " + rel);
        }
    }
    
}
