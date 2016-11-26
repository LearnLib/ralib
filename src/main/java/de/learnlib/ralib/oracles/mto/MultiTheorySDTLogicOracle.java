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
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

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

    @Override
    public GeneralizedSymbolicSuffix suffixForCounterexample(
            Word<PSymbolInstance> prefix, SymbolicDecisionTree sdt1, PIV piv1, 
            SymbolicDecisionTree sdt2, PIV piv2, 
            TransitionGuard guard, Word<ParameterizedSymbol> actions) {

        log.log(Level.FINEST,"suffixForCounterexample ------------------------------");
        log.log(Level.FINEST,"Prefix: " + prefix);
        log.log(Level.FINEST,"Guard: " + guard);
        log.log(Level.FINEST,"Actions: " + actions);
        log.log(Level.FINEST,"PIV1: " + piv1);
        log.log(Level.FINEST,"SDT1: " + sdt1);
        log.log(Level.FINEST,"PIV2: " + piv2);
        log.log(Level.FINEST,"SDT2: " + sdt2);        
        log.log(Level.FINEST,"------------------------------------------------------");        
        
        SDT _sdt1 = (SDT) sdt1;
        SDT _sdt2 = (SDT) sdt2;
        
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
                createRemapping(piv2, piv1);
        
        for (Conjunction e1 : expr1_T) {
            if (!solver.isSatisfiable(new Conjunction(exprG, e1))) {
                continue;
            }            
            if (expr2_F.isEmpty()) {
                // found counterexample slice
                GuardExpression[] e2r = new GuardExpression[e1.getConjuncts().length];
                Arrays.fill(e2r, TrueGuardExpression.TRUE);
                return counterExampleFromSlice(e1, new Conjunction(e2r), actions);
            }
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
            if (expr2_T.isEmpty()) {
                // found counterexample slice
                GuardExpression[] e2r = new GuardExpression[e1.getConjuncts().length];
                Arrays.fill(e2r, TrueGuardExpression.TRUE);
                return counterExampleFromSlice(e1, new Conjunction(e2r), actions);
            }
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
            
            prels[i] = prefixRelations(atoms2);
            if (ps instanceof OutputSymbol) {
                prels[i].add(DataRelation.DEFAULT);
                prels[i].add(DataRelation.EQ);
            }
            suffixRelations(srels[i], atoms2);
                
            /*
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
            else {
                // equivalent - use both or does not matter?
                prels[i] = prefixRelations(atoms1);
                suffixRelations(srels[i], atoms1);           
            }
            */
        }
        
        
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
        
        EnumSet<DataRelation> ret = EnumSet.of(DataRelation.DEFAULT);
        for (AtomicGuardExpression e : es) {
            if (!(e.getLeft() instanceof SuffixValue) || 
                    !(e.getRight() instanceof SuffixValue)) {
                ret.add(toDR(e.getRelation()));
            }
        }
        return ret;
    }

    private void suffixRelations(EnumSet<DataRelation>[] srels, 
            Collection<AtomicGuardExpression> es) {
        
        Arrays.fill(srels, EnumSet.of(DataRelation.DEFAULT));        
        for (AtomicGuardExpression e : es) {
            if (e.getLeft() instanceof SuffixValue && 
                    e.getRight() instanceof SuffixValue) {
                int idx = Math.min(e.getLeft().getId(), e.getRight().getId()) -1;
                
                srels[idx].add(toDR(e.getRelation()));
                srels[idx].remove(DataRelation.DEFAULT);
            }
        }
    }
    
    private DataRelation toDR(Relation rel) {
        switch (rel) {
            case EQUALS: return DataRelation.EQ;
            case LESSER: return DataRelation.LT;
            case GREATER: return DataRelation.GT;
            case LSREQUALS: return DataRelation.LTE;
            case GREQUALS: return DataRelation.GTE;            
            case NOT_EQUALS: return DataRelation.DEFAULT;
            default:
                throw new IllegalStateException("Unsupported Relation: " + rel);
        }
    }
    
}
