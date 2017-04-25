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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.ConstantGuardExpression;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.FalseGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.automata.guards.SumCAtomicGuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.ParamSignature;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
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
        
        GuardExpression expr1 = _sdt1.getAcceptingPaths();
        GuardExpression expr2 = _sdt2.getAcceptingPaths();      
        GuardExpression exprG = guard.getCondition();
        boolean acceptSat = satisfiable(expr1, piv1, expr2, piv2, exprG);
        GuardExpression expr1R =  _sdt1.getRejectingPaths();
        GuardExpression expr2R = _sdt2.getRejectingPaths();  
        boolean rejSat = satisfiable(expr1R, piv1, expr2R, piv2, exprG);
        return acceptSat | rejSat;
    }
    
    
    //TODO the context Mappings should be replaced by ParValuation/Word<PSymbol> prefix (for prefix... ) which are more accurate in terms of defining the context
    public boolean areEquivalent(SymbolicDecisionTree sdt1, PIV piv1, GuardExpression guard1, SymbolicDecisionTree sdt2, PIV piv2, GuardExpression guard2,
    		Mapping<SymbolicDataValue, DataValue<?>> contextMapping) {
        SDT _sdt1 = (SDT) sdt1;
        SDT _sdt2 = (SDT) sdt2;
		GuardExpression acc1 = _sdt1.getAcceptingPaths();
		GuardExpression acc2 = _sdt2.getAcceptingPaths();
		if (acc1 instanceof FalseGuardExpression)
			return acc2 instanceof FalseGuardExpression;
		else if (acc2 instanceof FalseGuardExpression)
			return acc1 instanceof FalseGuardExpression;
		GuardExpression rej1 = _sdt1.getRejectingPaths();
		GuardExpression rej2 = _sdt2.getRejectingPaths();
		
		GuardExpression[] contextConjuncts = this.buildContextExpressions(contextMapping);
		GuardExpression common = new Conjunction(contextConjuncts);
		
		VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
	                createRemapping(piv1, piv2);
		
		GuardExpression acc2r = acc2.relabel(remap);
		GuardExpression rej2r = rej2.relabel(remap);
		GuardExpression guard2r = guard2.relabel(remap);
		
		GuardExpression eqTest = new Disjunction(
				new Conjunction(acc1, new Negation(acc2r)),
				new Conjunction(new Negation(acc1), acc2r),
				new Conjunction(rej1, new Negation(rej2r)),
				new Conjunction(new Negation(rej1), rej2r)
				);
		
		GuardExpression eqTestGuard1 = new Conjunction(common, eqTest, guard1);
		GuardExpression eqTestGuard2 = new Conjunction(common, eqTest, guard2r);

		boolean sat1 = this.solver.isSatisfiable(eqTestGuard1);
		boolean sat2 = this.solver.isSatisfiable(eqTestGuard2);
		
		boolean isEquivalent = !sat1 || !sat2;
		return isEquivalent;
    }
    
    private boolean satisfiable(GuardExpression expr1, PIV piv1, GuardExpression expr2, PIV piv2, GuardExpression exprG) {
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
         return r;
    }

    @Override
    public boolean doesRefine(TransitionGuard refining, PIV pivRefining, 
            TransitionGuard refined, PIV pivRefined) {
        boolean doesRefine = this.doesRefine(refining.getCondition(), pivRefining, refined.getCondition(), pivRefined);
        return doesRefine;
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
    
    public boolean doesRefine(GuardExpression refining, PIV pivRefining, 
            GuardExpression refined, PIV pivRefined, Mapping<SymbolicDataValue, DataValue<?>> contextMapping) {
    	GuardExpression refiningConjunction = this.augmentGuardWithContext(refining, contextMapping);
    	GuardExpression refinedConjunction = this.augmentGuardWithContext(refined, contextMapping);
    	
    	return this.doesRefine(refiningConjunction, pivRefining, refinedConjunction, pivRefined);
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
    
    public boolean canBothBeSatisfied(GuardExpression refining, PIV pivRefining, 
            GuardExpression refined, PIV pivRefined, Mapping<SymbolicDataValue, DataValue<?>> contextValuation) {
    	GuardExpression refiningConjunction = this.augmentGuardWithContext(refining, contextValuation);
    	GuardExpression refinedConjunction = this.augmentGuardWithContext(refined, contextValuation);
    	
    	return canBothBeSatisfied(refiningConjunction, pivRefining, refinedConjunction, pivRefined);
    }
    
    
    private GuardExpression augmentGuardWithContext(GuardExpression guardExpresion, Mapping<SymbolicDataValue, DataValue<?>> contextValuation) {
    	if (contextValuation.isEmpty())
    		return guardExpresion;
    	GuardExpression [] contextGuards = buildContextExpressions(contextValuation);
    	GuardExpression [] allGuards = Arrays.copyOf(contextGuards, contextGuards.length + 1);
    	allGuards[contextGuards.length] = guardExpresion;
    	return new Conjunction(allGuards);
    }
    
    
    private GuardExpression [] buildContextExpressions(Mapping<SymbolicDataValue, DataValue<?>> contextMapping) {
    	List<GuardExpression> fixedValues = new ArrayList<GuardExpression> (contextMapping.size());
    	contextMapping.forEach((var,dv) 
    			-> fixedValues.add(new ConstantGuardExpression(var, dv)));
    	return fixedValues.toArray(new GuardExpression[]{});
    }
    
    
    public static VarMapping<SymbolicDataValue, SymbolicDataValue> createRemapping(
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

    
    // sdt1 is hyp, sdt2 is sut 
    public GeneralizedSymbolicSuffix suffixForCounterexample(
            Word<PSymbolInstance> prefix, SymbolicDecisionTree sdt1, PIV sdt1Piv, 
            SymbolicDecisionTree sdt2, PIV sdt2Piv, 
            TransitionGuard guard, Word<ParameterizedSymbol> actions) {

        log.log(Level.FINEST,"suffixForCounterexample ------------------------------");
        log.log(Level.FINEST,"Prefix: " + prefix);
        log.log(Level.FINEST,"Guard: " + guard);
        log.log(Level.FINEST,"Actions: " + actions);
        log.log(Level.FINEST,"PIV1: " + sdt1Piv);
        log.log(Level.FINEST,"SDT1: " + sdt1);
        log.log(Level.FINEST,"PIV2: " + sdt2Piv);
        log.log(Level.FINEST,"SDT2: " + sdt2);        
        log.log(Level.FINEST,"------------------------------------------------------");        
        
        SDT _sdt1 = (SDT) sdt1;
        SDT _sdt2 = (SDT) sdt2;
        
        // remapping between sdts
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(sdt2Piv, sdt1Piv);

        SDT _sdt2r = (SDT) _sdt2.relabel(remap);
        PIV piv = sdt2Piv.relabel(remap);
        Map<SymbolicDataValue, ParamSignature> prefixMap = this.computePrefixSourceMap(prefix, piv);
        

        //_sdt1 = replaceSuffixesWithPrefixes(_sdt1); 
        //_sdt2r = replaceSuffixesWithPrefixes(_sdt2r);
        
        // get all the paths
        List<List<SDTGuard>> expr1_T = _sdt1.getPaths(Collections.emptyList(), true);
        List<List<SDTGuard>> expr2_T = _sdt2r.getPaths(Collections.emptyList(), true);                
        List<List<SDTGuard>> expr1_F = _sdt1.getPaths(Collections.emptyList(), false);
        List<List<SDTGuard>> expr2_F = _sdt2r.getPaths(Collections.emptyList(), false); 
      
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
        
        // find disagreeing paths in the two sdts which are satisfiable under conjunction 
        // (one path refines the other)
        for (List<SDTGuard> e1 : expr1_T) {
        	Conjunction e1Conj = SDT.toPathExpression(e1);
            if (!solver.isSatisfiable(new Conjunction(exprG, e1Conj))) {
                continue;
            }            
//            if (expr2_F.isEmpty()) {
//                // found counterexample slice
//                GuardExpression[] e2r = new GuardExpression[e1.getConjuncts().length];
//                Arrays.fill(e2r, TrueGuardExpression.TRUE);
//                return counterExampleFromSlice(e1, new Conjunction(e2r), actions);
//            }
            for (List<SDTGuard> e2 : expr2_F) {
            	Conjunction e2Conj = SDT.toPathExpression(e2);
                if (solver.isSatisfiable(new Conjunction(exprG, e1Conj, e2Conj))) {
                    // found counterexample slice
                    return counterExampleFromSlice(e1, e2, _sdt1, _sdt2r, actions, prefixMap);
                }
            }            
        }
        
        for (List<SDTGuard> e1 : expr1_F) {
        	Conjunction e1Conj = SDT.toPathExpression(e1);
            if (!solver.isSatisfiable(new Conjunction(exprG, e1Conj))) {
                continue;
            }            
//            if (expr2_F.isEmpty()) {
//                // found counterexample slice
//                GuardExpression[] e2r = new GuardExpression[e1.getConjuncts().length];
//                Arrays.fill(e2r, TrueGuardExpression.TRUE);
//                return counterExampleFromSlice(e1, new Conjunction(e2r), actions);
//            }
            for (List<SDTGuard> e2 : expr2_T) {
            	Conjunction e2Conj = SDT.toPathExpression(e2);
                if (solver.isSatisfiable(new Conjunction(exprG, e1Conj, e2Conj))) {
                    // found counterexample slice
                    return counterExampleFromSlice(e1, e2, _sdt1, _sdt2r, actions, prefixMap);
                }
            }            
        }
               
        throw new IllegalStateException("Could not find CE slice");
    }
    
    private Map<SymbolicDataValue, ParamSignature> computePrefixSourceMap(Word<PSymbolInstance> prefix, PIV piv) {
    	Map<SymbolicDataValue, ParamSignature> regToSig = new LinkedHashMap<SymbolicDataValue, ParamSignature>();
    	SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
    	for(PSymbolInstance sym : prefix) {
    		int actIndex = 0;
    		for (DataValue dv : sym.getParameterValues()) {
    			Register reg = piv.get(pgen.next(dv.getType()));
    			if (reg != null)
    			//assert reg != null;
    				regToSig.put(reg, new ParamSignature(sym.getBaseSymbol(), actIndex));
    			actIndex ++;
    		}
    	}
    	return regToSig;
    }
    
    private GeneralizedSymbolicSuffix counterExampleFromSlice(
            List<SDTGuard> e1Guards, List<SDTGuard> e2Guards, SDT sdt1, SDT sdt2, Word<ParameterizedSymbol> actions, Map<SymbolicDataValue, ParamSignature> prefixMap) {
        
        System.out.println("-----------------------------------------------");  
        System.out.println("Actions: " + actions);
        System.out.println("Path 1: " + e1Guards);
        System.out.println("Path 2: " + e2Guards);      
    
        EnumSet<DataRelation>[] prels = new EnumSet[e1Guards.size()];
        EnumSet<DataRelation>[][] srels = new EnumSet[e1Guards.size()][];
        Set<ParamSignature>[] psource = new Set[e1Guards.size()];
        
        int idx = 0;
        int base = 0;
        Conjunction e1 = SDT.toPathExpression(e1Guards);
        Conjunction e2 = SDT.toPathExpression(e2Guards);
        
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
            
            Collection<AtomicGuardExpression> usedAtoms;
            
            LinkedHashSet<AtomicGuardExpression> allAtoms;
			if (implies(c1, c2) && !implies(c2, c1)) 
                // use c1                
            	usedAtoms = 
            	//getBranchingAtomsAtPath(e1Guards.subList(0, i), sdt1); 
            	atoms1;
            else if (!implies(c1, c2) && implies(c2, c1)) 
                // use c2
            	usedAtoms =  
            			//getBranchingAtomsAtPath(e2Guards.subList(0, i), sdt2);
            	atoms2;
            else  if (!implies(c1, c2) && !implies(c2, c1)) {
            	// use all atoms in the branches of c1 and c2
            	allAtoms = new LinkedHashSet<>(atoms1);
            	allAtoms.addAll(atoms2);
				
                usedAtoms = allAtoms;
                		//getBranchingAtomsAtPath(e1Guards.subList(0, i), e2Guards.subList(0, i), sdt1, sdt2);
        	} else 
                // equivalent - use both or does not matter?
            	usedAtoms = atoms1;
                
			// seems to preserve succint-ness
			usedAtoms = getBranchingAtomsAtLevel(i, sdt1, sdt2);
            prels[i] = prefixRelations(usedAtoms);
            suffixRelations(srels[i], usedAtoms);
            psource[i] = this.prefixSource(usedAtoms, prefixMap);
        }
        
        GeneralizedSymbolicSuffix suffix = 
                new GeneralizedSymbolicSuffix(actions, prels, srels, psource);
        
        System.out.println("New suffix: " + suffix);
        
        return suffix;
    }
    
    private Collection<AtomicGuardExpression> getBranchingAtomsAtLevel(int level,  SDT sdt, SDT sdt2) {
    	Collection<AtomicGuardExpression> atoms = new LinkedHashSet<>();
    	atoms.addAll(getBranchingAtomsAtLevel(level, sdt, 0));
    	atoms.addAll(getBranchingAtomsAtLevel(level, sdt2, 0));
    	return atoms;
    }
    
    private Collection<AtomicGuardExpression> getBranchingAtomsAtLevel(int level,  SDT sdt, int crtLevel) {
    	if (sdt instanceof SDTLeaf)
    		return Collections.emptySet();
    	Map<SDTGuard, SDT> children = sdt.getChildren();
    	if (crtLevel == level) {
    		return SDT.toPathExpression(children.keySet()).getAtoms();
    	} else {
    		Collection<AtomicGuardExpression> atoms = new LinkedHashSet<>();
        	for (Map.Entry<SDTGuard, SDT> entry : children.entrySet()) {
        		atoms.addAll(getBranchingAtomsAtLevel(level, entry.getValue(), crtLevel + 1));
        	}
        	return atoms;
    	}
    }
    
    private boolean implies(GuardExpression left, GuardExpression right) {
        return !solver.isSatisfiable(new Conjunction(left, new Negation(right)));
    }
    
    private EnumSet<DataRelation> prefixRelations(
            Collection<AtomicGuardExpression> es) {
        
        EnumSet<DataRelation> ret = EnumSet.noneOf(DataRelation.class);
        for (AtomicGuardExpression e : es) {
        	//TODO Constants count here as registers? 
            if (!(e.getLeft() instanceof SuffixValue) || 
                    !(e.getRight() instanceof SuffixValue)) {
                ret.addAll(toDR(e, consts));
            }
        }

        return ret;
    }
    
    private Set<ParamSignature> prefixSource(
            Collection<AtomicGuardExpression> es, Map<SymbolicDataValue, ParamSignature> sourceMap) {
        Set<ParamSignature> sigs = new LinkedHashSet<>();
        for (AtomicGuardExpression e : es) {
        	//TODO Constants count here as registers? 
        	if (sourceMap.containsKey(e.getLeft()))
        		sigs.add(sourceMap.get(e.getLeft()));
        	if (sourceMap.containsKey(e.getRight()))
        		sigs.add(sourceMap.get(e.getRight()));
        }

        return sigs;
    }

    private void suffixRelations(EnumSet<DataRelation>[] srels, 
            Collection<AtomicGuardExpression> es) {
        
    	for (int i = 0; i < srels.length; i ++)
    		srels[i] = EnumSet.noneOf(DataRelation.class);

    	for (AtomicGuardExpression e : es) {
            if (e.getLeft() instanceof SuffixValue && 
                    e.getRight() instanceof SuffixValue) {
                int idx = Math.min(e.getLeft().getId(), e.getRight().getId()) -1;
                assert e.getLeft().getId() < e.getRight().getId();
                srels[idx].addAll(toDR(e, consts));
            }
        }
    }
    

    static EnumSet<DataRelation> toDR(AtomicGuardExpression atom, Constants consts) {
         switch (atom.getRelation()) {
             case EQUALS: 
             	if (atom instanceof SumCAtomicGuardExpression) {
             		DataValue cst = ((SumCAtomicGuardExpression) atom).getLeftConst();
             		int index = consts.getSumCs(cst.getType()).indexOf(cst);
             		if (index == 0)
             			return EnumSet.of(DataRelation.EQ_SUMC1);
             		if (index == 1)
             			return EnumSet.of(DataRelation.EQ_SUMC2);
             		throw new DecoratedRuntimeException("No relations for more than 2 sumc s")
             		.addDecoration("index", index);
             	} 
             	else return EnumSet.of(DataRelation.EQ);
             case GREATER: 
              	if (atom instanceof SumCAtomicGuardExpression) {
             		int index = getSumCIndex((SumCAtomicGuardExpression) atom, consts);
             		if (index == 0)
             			return EnumSet.of(DataRelation.LT_SUMC1);
             		if (index == 1)
             			return EnumSet.of(DataRelation.LT_SUMC2);
             		throw new DecoratedRuntimeException("No relations for more than 2 sumc s")
             		.addDecoration("index", index);
             	} else
             		return EnumSet.of(DataRelation.LT);
             case GREQUALS: 
             	if (atom instanceof SumCAtomicGuardExpression) {
             		int index = getSumCIndex((SumCAtomicGuardExpression) atom, consts);
             		if (index == 0)
             			return EnumSet.of(DataRelation.LT_SUMC1, DataRelation.EQ_SUMC1);
             		if (index == 1)
             			return EnumSet.of(DataRelation.LT_SUMC2, DataRelation.EQ_SUMC2);
             		throw new DecoratedRuntimeException("No relations for more than 2 sumc s")
             		.addDecoration("index", index);
             	} else
             		return EnumSet.of(DataRelation.LT, DataRelation.EQ);
             case LESSER: return EnumSet.of(DataRelation.DEFAULT);
             case LSREQUALS: return EnumSet.of(DataRelation.DEFAULT, DataRelation.EQ);            
             case NOT_EQUALS: 
             	if (atom instanceof SumCAtomicGuardExpression) {
             		int index = getSumCIndex((SumCAtomicGuardExpression) atom, consts);
             		if (index == 0)
             			return EnumSet.of(DataRelation.DEQ_SUMC1);
             		if (index == 1)
             			return EnumSet.of(DataRelation.DEQ_SUMC2);
             		throw new DecoratedRuntimeException("No relations for more than 2 sumc s")
             		.addDecoration("index", index);
             	} else
             		return EnumSet.of(DataRelation.DEQ);
             default:
                 throw new IllegalStateException("Unsupported Relation: " + atom.getRelation());
         }
     }
    
    static int getSumCIndex(SumCAtomicGuardExpression atom, Constants consts) {
    	DataValue cst = ((SumCAtomicGuardExpression) atom).getRightConst();
    	if (cst == null)
    		cst = ((SumCAtomicGuardExpression) atom).getLeftConst();
    	System.out.println(atom + " " + cst + " " + atom);
		int index = consts.getSumCs(cst.getType()).indexOf(cst);
		return index;
    }

    
        
    
    /* METHODS that could be used (if not, should be removed)
     */

    private Collection<AtomicGuardExpression> getBranchingAtomsAtPath(List<SDTGuard> path,  SDT sdt) {
    	Set<SDTGuard> pathBranching = sdt.getBranchingAtPath(path);
    	List<SDTGuard> guards = new ArrayList<>(pathBranching);
    	Conjunction falsePath = SDT.toPathExpression(guards);
    	Collection<AtomicGuardExpression> allAtoms = falsePath.getAtoms();
    	return allAtoms;
    }
    

    /**
     * Relabeles  sdts so that registers appearing in equality expressions are replaced by the suffixes to which
     * they bound in the context of these expressions. 
     */
    public SDT relabelPrefixesWithSuffixes(SDT sutSdt) {
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
    
    private SDT replaceSuffixesWithPrefixes(SDT sutSdt) {
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
    			if (reg.isRegister() || reg.isConstant()) {
    				Replacement repl = new Replacement();
    				repl.put(equGuard.getParameter(), equGuard.getExpression());
    				sdt = (SDT) sdt.replace(repl);
    			} 
    		}
    		SDT relabeledSdt = replaceSuffixesWithPrefixes(sdt);
    		newChildren.put(guard, relabeledSdt);
    	}
    	return new SDT(newChildren);
    }
}
