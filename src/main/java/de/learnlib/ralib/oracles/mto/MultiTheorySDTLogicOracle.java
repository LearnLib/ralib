/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.oracles.mto;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.automata.guards.IfGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheorySDTLogicOracle implements SDTLogicOracle {

    private final ConstraintSolver solver;
    
    private final Constants consts;
    
    private static LearnLogger log = LearnLogger.getLogger(MultiTheorySDTLogicOracle.class);

    public MultiTheorySDTLogicOracle(Constants consts) {
        ConstraintSolverFactory fact = new ConstraintSolverFactory();
        this.solver = fact.createSolver("z3");
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
        IfGuard _guard = (IfGuard) guard;
        
        DataExpression<Boolean> expr1 = _sdt1.getAcceptingPaths(consts);
        DataExpression<Boolean> expr2 = _sdt2.getAcceptingPaths(consts);        
        DataExpression<Boolean> exprG = _guard.getCondition();

        VarMapping<SymbolicDataValue, SymbolicDataValue> gremap = 
                new VarMapping<>();
        for (SymbolicDataValue sv : exprG.getMapping().keySet()) {
            if (sv instanceof Parameter) {
                gremap.put(sv, new SuffixValue(sv.getType(), sv.getId()));
            }
        }
        
        exprG = exprG.relabel(gremap);
        
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(piv2, piv1);
        
        DataExpression<Boolean> expr2r = expr2.relabel(remap);
        
        DataExpression<Boolean> left = DataExpression.and(
                exprG, expr1, DataExpression.negate(expr2r));
        
        DataExpression<Boolean> right = DataExpression.and(
                exprG, expr2r, DataExpression.negate(expr1));
        
        DataExpression<Boolean> test = DataExpression.or(left, right);

        System.out.println("A1:  " + expr1);
        System.out.println("A2:  " + expr2);
        System.out.println("G:   " + exprG);
        System.out.println("MAP: " + remap);
        System.out.println("A2': " + expr2r);
        System.out.println("TEST:" + test);
        
        System.out.println("HAS CE: " + test.getExpression());
        Result r = solver.isSatisfiable(test.getExpression());
        log.log(Level.FINEST,"Res:" + r);
        if (r == Result.DONT_KNOW) {
            throw new IllegalStateException("could not solve constraint.");
        }
        return (r == Result.SAT);
    }

    @Override
    public boolean doesRefine(TransitionGuard refining, PIV pivRefining, 
            TransitionGuard refined, PIV pivRefined) {

        IfGuard _refining = (IfGuard) refining;
        IfGuard _refined = (IfGuard) refined;
        
        log.log(Level.FINEST, "refining: {0}", _refining);
        log.log(Level.FINEST, "refined: {0}", _refined);
        log.log(Level.FINEST, "pivRefining: {0}", pivRefining);
        log.log(Level.FINEST, "pivRefined: {0}", pivRefined);
        
        VarMapping<SymbolicDataValue, SymbolicDataValue> remap = 
                createRemapping(pivRefined, pivRefining);
        
        DataExpression<Boolean> exprRefining = _refining.getCondition();
        DataExpression<Boolean> exprRefined = 
                _refined.getCondition().relabel(remap);
        
        // is there any case for which refining is true but refined is false?
        DataExpression<Boolean> test = DataExpression.and(
            exprRefining, DataExpression.negate(exprRefined));
        
        log.log(Level.FINEST,"MAP: " + remap);
        log.log(Level.FINEST,"TEST:" + test);        
                
        Result r = solver.isSatisfiable(test.getExpression());
        System.out.println("DOES REFINE: " + test.getExpression() + " : " + r);
        log.log(Level.FINEST,"Res:" + r);
        if (r == Result.DONT_KNOW) {
            throw new IllegalStateException("could not solve constraint.");
        }               
        
        return (r == Result.UNSAT);       
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
    
}
