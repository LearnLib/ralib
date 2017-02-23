/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.learning;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.automatalib.words.Word;

/**
 * Analyzes Counterexamples in a binary search
 * as described in SEFM 2014.
 * 
 * @author falk
 */
public class CounterexampleStatistics {

    private final TreeOracle sulOracle;
    
    private final TreeOracle hypOracle;
    
    private final Hypothesis hypothesis;

    private final SDTLogicOracle sdtOracle;

    private final Map<Word<PSymbolInstance>, Component> components;
    
    private final Constants consts;

    private final Map<DataType, Theory> teachers;
    
    private static enum IndexResult {HAS_CE_AND_REFINES, HAS_CE_NO_REFINE, NO_CE};

    private static final LearnLogger log = LearnLogger.getLogger(CounterexampleStatistics.class);
    
    CounterexampleStatistics(Map<DataType, Theory> teachers,
            TreeOracle sulOracle, TreeOracle hypOracle, 
            Hypothesis hypothesis, SDTLogicOracle sdtOracle, 
            Map<Word<PSymbolInstance>, Component> components, Constants consts) {
        
        this.teachers = teachers;
        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.components = components;
        this.consts = consts;
    }
    
    Word<PSymbolInstance> analyzeCounterexample(Word<PSymbolInstance> ce) {
        
        System.out.println("================================================ CE REPORT ================================================");
        System.out.println("");
        System.out.println("Counterexample: " + ce);
        System.out.println("------------------------------------------------------");                
        List<Transition> tseq = new ArrayList<>();
        hypothesis.getTransitions(ce, tseq);
        for (Transition t : tseq) {
            System.out.println(t);
        }        
        System.out.println("------------------------------------------------------");
        if (false) {
            Word<PSymbolInstance> tCe = findWordFor(tseq);        
            System.out.println("TCE: " + tCe);
            Word<PSymbolInstance> eps = Word.<PSymbolInstance>epsilon();
            SymbolicSuffix symSuffix = new SymbolicSuffix(tCe, eps);

            TreeQueryResult resHyp = hypOracle.treeQuery(tCe, symSuffix);
            TreeQueryResult resSul = sulOracle.treeQuery(tCe, symSuffix);        

            SDTLeaf l1 = (SDTLeaf) resHyp.getSdt();
            SDTLeaf l2 = (SDTLeaf) resSul.getSdt();

            System.out.println("TCE is CE: " + (l1.isAccepting() ^ l2.isAccepting()));
            if (l1.isAccepting() ^ l2.isAccepting()) {
                ce = tCe;
            }
        }
        if (true) return ce;
        
        IndexResult[] ir = new IndexResult[ce.length()];
        for (int idx=0; idx<ce.length(); idx++) {
            ir[idx] = computeIndex(ce, idx);
        }
        
        System.out.println("Index Results:" + Arrays.toString(ir));
        
        System.out.println("================================================ CE REPORT ================================================");
        
        return ce;
    } 
    
    private IndexResult computeIndex(Word<PSymbolInstance> ce, int idx) {
                
        Word<PSymbolInstance> prefix = ce.prefix(idx);
        System.out.println(idx + "  " + prefix);        
        Word<PSymbolInstance> location = hypothesis.transformAccessSequence(prefix);
        Word<PSymbolInstance> transition = hypothesis.transformTransitionSequence(
            ce.prefix(idx+1));         
        
        Word<PSymbolInstance> suffix = ce.suffix(ce.length() -idx);        
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, consts);
        
        TreeQueryResult resHyp = hypOracle.treeQuery(location, symSuffix);
        TreeQueryResult resSul = sulOracle.treeQuery(location, symSuffix);
        
        System.out.println("------------------------------------------------------");
        System.out.println("Computing index: " + idx);
        System.out.println("Prefix: " + prefix);
        System.out.println("SymSuffix: " + symSuffix);
        System.out.println("Location: " + location);
        System.out.println("Transition: " + transition);
        System.out.println("PIV HYP: " + resHyp.getPiv());
        System.out.println("SDT HYP: " + resHyp.getSdt());
        System.out.println("PIV SYS: " + resSul.getPiv());
        System.out.println("SDT SYS: " + resSul.getSdt());        
        
        Component c = components.get(location);
        ParameterizedSymbol act = transition.lastSymbol().getBaseSymbol();
        TransitionGuard g = c.getBranching(act).getBranches().get(transition);
        
        boolean hasCE = sdtOracle.hasCounterexample(location, 
                resHyp.getSdt(), resHyp.getPiv(), //new PIV(location, resHyp.getParsInVars()), 
                resSul.getSdt(), resSul.getPiv(), //new PIV(location, resSul.getParsInVars()), 
                g, transition);
        
        if (!hasCE) {
            return IndexResult.NO_CE;
        }
        
        // PIV pivSul = new PIV(location, resSul.getParsInVars());
        PIV pivSul = resSul.getPiv();
        PIV pivHyp = c.getPrimeRow().getParsInVars();
        boolean sulHasMoreRegs = !pivHyp.keySet().containsAll(pivSul.keySet());                
        boolean hypRefinesTransition = 
                hypRefinesTransitions(location, act, resSul.getSdt(), pivSul);
        
        System.out.println("sulHasMoreRegs: " + sulHasMoreRegs);
        System.out.println("hypRefinesTransition: " + hypRefinesTransition);
        System.out.println("------------------------------------------------------");
        
        return (sulHasMoreRegs || !hypRefinesTransition) ? 
                IndexResult.HAS_CE_AND_REFINES : IndexResult.HAS_CE_NO_REFINE;        
    }
    
    private boolean hypRefinesTransitions(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol action, SymbolicDecisionTree sdtSUL, PIV pivSUL) {
        
        Branching branchSul = sulOracle.getInitialBranching(prefix, action, pivSUL, sdtSUL);
        Component c = components.get(prefix);
        Branching branchHyp = c.getBranching(action);
        
//        System.out.println("Branching Hyp:");
//        for (Entry<Word<PSymbolInstance>, TransitionGuard> e : branchHyp.getBranches().entrySet()) {
//            System.out.println(e.getKey() + " -> " + e.getValue());
//        }
//        System.out.println("Branching Sys:");
//        for (Entry<Word<PSymbolInstance>, TransitionGuard> e : branchSul.getBranches().entrySet()) {
//            System.out.println(e.getKey() + " -> " + e.getValue());
//        }
        
        for (TransitionGuard guardHyp : branchHyp.getBranches().values()) {
            boolean refines = false;
            for (TransitionGuard guardSul : branchSul.getBranches().values()) {
                if (sdtOracle.doesRefine(guardHyp, c.getPrimeRow().getParsInVars(), 
                        guardSul, pivSUL)) {
                    refines = true;
                    break;
                }
            }
            
            if (!refines) {
                return false;
            }
        }
        
        return true;
    }
    
    private Word<PSymbolInstance> findWordFor(List<Transition> tseq) {
        
        Word<ParameterizedSymbol> acts = Word.<ParameterizedSymbol>epsilon();
        Map<Integer, DataValue> vals = new HashMap<>();
        
        VarValuation regs = hypothesis.getInitialRegisters();
        
        int dIdx = 1;
        for (Transition t : tseq) {
            acts = acts.append(t.getLabel());            
            ParValuation pval = new ParValuation();
            SymbolicDataValueGenerator.ParameterGenerator pgen = 
                    new SymbolicDataValueGenerator.ParameterGenerator();
                        
            Map<Integer, Register> map = findValuesForTransition(t);
            for (DataType type : t.getLabel().getPtypes()) {
                DataValue v;
                Parameter p = pgen.next(type);
                if (map.containsKey(p.getId())) {
                    v = regs.get(map.get(p.getId()));
                }
                else {
                    Theory theory = teachers.get(type);
                    List<DataValue> used = new ArrayList<>();
                    used.addAll(vals.values());
                    v = theory.getFreshValue(used);
                }
                
                pval.put(p, v);
                vals.put(dIdx, v);
                dIdx++;
            }
            
            regs = t.execute(regs, pval, consts);            
        }
        
        return DataWords.instantiate(acts, vals);
    }
    
    private Map<Integer, Register> findValuesForTransition(Transition t) {
        return (t instanceof OutputTransition) ?
                findValuesForOutputTransition( (OutputTransition)t ) : 
                findValuesForInputTransition( t );
    }

    private Map<Integer, Register> findValuesForInputTransition(Transition t) {
        Map<Integer, Register> ret = new HashMap<>();        
        getEqualities(t.getGuard().getCondition(), ret);       
        return ret;
    }

    private Map<Integer, Register> findValuesForOutputTransition(OutputTransition t) {
        Map<Integer, Register> ret = new HashMap<>();                
        for (Entry<Parameter, SymbolicDataValue> e :
                t.getOutput().getOutput().entrySet()) {
            
            ret.put(e.getKey().getId(), (Register) e.getValue());
        }        
        return ret;
    }
    
    private void getEqualities(GuardExpression expr, Map<Integer, Register> map) {
        if (expr instanceof Conjunction) {
            Conjunction c = (Conjunction) expr;
            for (GuardExpression e : c.getConjuncts()) {
                getEqualities(e, map);
            }
        }
        else if (expr instanceof AtomicGuardExpression) {
            AtomicGuardExpression e = (AtomicGuardExpression) expr;
            switch (e.getRelation()) {
                case EQUALS:
                    if (e.getLeft() instanceof Parameter) {
                        map.put(e.getLeft().getId(), (Register)e.getRight());
                    }
                    else {
                        map.put(e.getRight().getId(), (Register)e.getLeft());
                    }
                case NOT_EQUALS:
                    return;
                default:
                    throw new IllegalStateException("not implemented yet.");
            }           
        }
        else if (expr instanceof Disjunction) {
            Disjunction d = (Disjunction) expr;
            // FIXME: picking the first is a heuristic!!!
            for (GuardExpression e : d.getDisjuncts()) {
                getEqualities(e, map);
                return;
            }
        }
        else if (expr instanceof TrueGuardExpression) {
            return;
        }
        else {
            throw new IllegalStateException("not implemented yet.");
        }
    }
}
