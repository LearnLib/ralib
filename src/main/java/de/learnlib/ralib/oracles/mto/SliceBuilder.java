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

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class SliceBuilder {

    private static class Pair<T> {

        final T e1;
        final T e2;

        public Pair(T e1, T e2) {
            this.e1 = e1;
            this.e2 = e2;
        }
    }

    private final Map<DataType, Theory> teachers;

    private final Constants constants;

    private final ConstraintSolver solver;

	private MultiTheorySDTLogicOracle mlo;

    public SliceBuilder(Map<DataType, Theory> teachers,
            Constants constants, ConstraintSolver solver) {

        this.teachers = teachers;
        this.constants = constants;
        this.solver = solver;
        this.mlo = new MultiTheorySDTLogicOracle(this.constants, this.solver);
    }

    public Slice sliceFromWord(
            Word<PSymbolInstance> prefix,
            Word<PSymbolInstance> suffix) {

        Slice slice = new Slice();

        DataValue[] concSuffixVals = DataWords.valsOf(suffix);
        Set<DataValue> concPrefixVals = DataWords.valSet(prefix);
        Map<DataValue, Register> regMap = new HashMap<>();
        Map<DataValue, SuffixValue> sMap = new HashMap<>();

        SuffixValueGenerator sgen = new SuffixValueGenerator();
        RegisterGenerator rgen = new RegisterGenerator();

        for (DataValue val : concSuffixVals) {
            DataType type = val.getType();
            Theory theory = teachers.get(type);
            SuffixValue sv = sgen.next(type);

            // process prefix values
            // boolean onlyDefault = true;
            for (DataValue pv : concPrefixVals) {
                if (!type.equals(pv.getType())) {
                    continue;
                }

                EnumSet<DataRelation> rels = (EnumSet<DataRelation>) theory.getRelations(Collections.singletonList(pv), val).get(0);

                assert !rels.isEmpty();

                if (!rels.equals(EnumSet.of(DataRelation.DEFAULT))) {
                    // onlyDefault = false;                    
                    Register r = regMap.get(pv);
                    if (r == null) {
                        r = rgen.next(type);
                        regMap.put(pv, r);
                    }
                    for (DataRelation rel : rels) {
                        slice.addPredicate(r, rel, sv);
                    }
                }
            }
            // process suffix values
            for (DataValue psv : sMap.keySet()) {
                if (!type.equals(psv.getType())) {
                    continue;
                }

                EnumSet<DataRelation> rels = (EnumSet<DataRelation>) 
                        theory.getRelations(Collections.singletonList(psv), val).get(0);

                assert !rels.isEmpty();

                if (!rels.equals(EnumSet.of(DataRelation.DEFAULT))) {
                    // onlyDefault = false;                    
                    SuffixValue leftsv = sMap.get(psv);
                    for (DataRelation rel : rels) {
                        slice.addPredicate(leftsv, rel, sv);
                    }
                }
            }

            if (!sMap.containsKey(val)) {
                sMap.put(val, sv);
            }
        }
        return slice;
    }

    public Slice sliceFromPaths(Conjunction p1, Conjunction p2) {

        Slice slice = new Slice();

        System.out.println(p1);
        System.out.println(p2);
        
        
        for (AtomicGuardExpression a : p1.getAtoms()) {
            assert (a.getRight() instanceof SuffixValue);
            for (DataRelation r : MultiTheorySDTLogicOracle.toDR(a, constants)) {
                if (r.equals(DataRelation.DEFAULT)) {
                    continue;
                }
                slice.addPredicate(a.getLeft(), r, (SuffixValue) a.getRight());
            }
        }
        for (AtomicGuardExpression a : p2.getAtoms()) {
            assert (a.getRight() instanceof SuffixValue);
            for (DataRelation r : MultiTheorySDTLogicOracle.toDR(a, constants)) {
                if (r.equals(DataRelation.DEFAULT)) {
                    continue;
                }
                slice.addPredicate(a.getLeft(), r, (SuffixValue) a.getRight());
            }
        }
        return slice;

    }
    
    public Slice sliceFromSDTs(SDT sdt1, SDT sdt2, Mapping<SymbolicDataValue, DataValue<?>> contextValuation, Word<ParameterizedSymbol> suffixActions) {
        
        List<Pair<Conjunction>> cand = new ArrayList<>();
        cand.addAll(getCandidates(sdt1, sdt2, contextValuation, true));
        cand.addAll(getCandidates(sdt1, sdt2, contextValuation, false));
        int minRank = 0;
        Slice minSlice = null;
        for (Pair<Conjunction> p : cand) {
            Slice act = sliceFromPaths(p.e1, p.e2);
            int rank = rankSlice(act, suffixActions);
            if (minSlice == null || rank < minRank) {
                minRank = rank;
                minSlice = act;
            }
        }
        assert minSlice != null;        
        return minSlice;
    }

    public Slice sliceFromTransitionAndSDT(
            Word<PSymbolInstance> ua, TransitionGuard guard,
            Parameter p, PIV pivU, PIV pivUA, SymbolicDecisionTree sdt,
            Word<ParameterizedSymbol> actions) {

        SDT _sdt = (SDT) sdt;
        Slice minSlice = null;
        int minRank = 0;

        List<Conjunction> paths = getPathsForMemorable(_sdt, pivUA.get(p));
        //paths.addAll(_sdt.getPathsAsExpressions(constants, true));
        //paths.addAll(_sdt.getPathsAsExpressions(constants, false));
        
        System.out.println("-----------------------------");
        System.out.println(pivUA.get(p));
        System.out.println(sdt);
                
        for (Conjunction c : paths) {
            Slice cur = sliceFromTransitionAndPath(ua, guard, p, pivU, pivUA, c);
            if (cur != null) {
                int rank = rankSlice(cur, actions.prepend(ua.lastSymbol().getBaseSymbol()));
                if (minSlice == null || rank < minRank) {
                    minRank = rank;
                    minSlice = cur;
                }
            }
        }
       // if (true) throw new IllegalStateException();

        assert minSlice != null;
        return minSlice;
    }

    public Slice sliceFromTransitionAndPath(
            Word<PSymbolInstance> ua, TransitionGuard guard,
            Parameter p, PIV pivU, PIV pivUA, Conjunction path) {

        System.out.println(path);
        System.out.println(guard);
        System.out.println("PIVUA:" + pivUA);
        System.out.println("PIVU:" + pivU);
        
        Register reg = pivUA.get(p);
        if (!path.getSymbolicDataValues().contains(reg)) {
            return null;
        }
        
        Word<PSymbolInstance> prefix = ua.prefix(ua.length() - 1);
        Word<PSymbolInstance> suffix = ua.suffix(1);
        int arityU = DataWords.paramLength(DataWords.actsOf(prefix));
        int arityA = DataWords.paramLength(DataWords.actsOf(suffix));
        RegisterGenerator rgen = new RegisterGenerator();
        Map<Register, Register> rMap = new HashMap<>();
        Slice slice = new Slice();

        for (AtomicGuardExpression a : guard.getCondition().getAtoms()) {
            assert a.getLeft() instanceof Register || a.getLeft() instanceof Constant;
             SymbolicDataValue sdv = null;
        	if (a.getLeft() instanceof Register) {
        		sdv = reg;
	            Register rOrig = (Register) a.getLeft();
	            Register rMapped = rMap.get(rOrig);
	            if (rMapped == null) {
	                rMapped = rgen.next(rOrig.getType());
	                rMap.put(rOrig, rMapped);
	            }
            } else if (a.getLeft() instanceof Constant) 
            	sdv = a.getLeft();
            else 
            	throw new DecoratedRuntimeException("Unexpected type").addDecoration("SDV", a);

            assert (a.getRight() instanceof Parameter);
            SuffixValue sv = new SuffixValue(
                    a.getRight().getType(), a.getRight().getId());

            for (DataRelation dr : MultiTheorySDTLogicOracle.toDR(a, constants)) {
                slice.addPredicate(sdv, dr, sv);
            }
        }

        Map<Register, Register> rMap2 = new HashMap<>();
        for (AtomicGuardExpression a : path.getAtoms()) {
            assert (a.getRight() instanceof SuffixValue);
                                    
            SuffixValue sv = new SuffixValue(
                    a.getRight().getType(), a.getRight().getId() + arityA);

//            System.out.println("right: " + a.getRight());
//            System.out.println("suffix: " + suffix);
//            System.out.println("arity: " + arityA);
//            System.out.println("sv: " + sv);
            
            SymbolicDataValue left;
            if (a.getLeft() instanceof Register) {
                Parameter pi = pivUA.getOneKey((Register) a.getLeft());
                // make suffix value
                if (pi.getId() > arityU) {
                    left = new SuffixValue(
                            a.getLeft().getType(), pi.getId() - arityU);
                } // make register
                else {
                    Register rUA = pivUA.get(p);
                    Register rU = pivU.get(p);
                    if (rU != null) {
                        left = rMap.get(rU);
                        if (left == null) {
                            left = rgen.next(p.getType());
                            rMap.put(rU, (Register) left);
                        }
                    } else {
                        left = rMap2.get(rUA);
                        if (left == null) {
                            left = rgen.next(p.getType());
                            rMap2.put(rUA, (Register) left);
                        }
                    }
                }

            } else if (a.getLeft() instanceof Constant) {
            	left = a.getLeft();
            } else {
                assert (a.getLeft() instanceof SuffixValue);
                left = new SuffixValue(
                        a.getLeft().getType(), a.getLeft().getId() + arityA);
            }

            for (DataRelation dr : MultiTheorySDTLogicOracle.toDR(a, constants)) {
                slice.addPredicate(left, dr, sv);
            }
        }
        
        System.out.println(slice);

        return slice;
    }

    private List<Pair<Conjunction>> getCandidates(
            SDT sdt1, SDT sdt2, Mapping<SymbolicDataValue, DataValue<?>> contextValuation, boolean accepting) {

        List<Pair<Conjunction>> ret = new ArrayList<>();

        List<Conjunction> paths1
                = sdt1.getPathsAsExpressions(constants, accepting);
        List<Conjunction> paths2
                = sdt2.getPathsAsExpressions(constants, !accepting);

        for (Conjunction c1 : paths1) {
            for (Conjunction c2 : paths2) {
            	// check if paths satisfiable ...
            	boolean r = this.mlo.canBothBeSatisfied(c1, new PIV(), c2, new PIV(), contextValuation);
                if (r) {
                    ret.add(new Pair<>(c1, c2));
                }
            }
        }
        return ret;
    }

    public int rankSlice(Slice slice, Word<ParameterizedSymbol> actions) {

        GeneralizedSymbolicSuffix suffix = 
                SymbolicSuffixBuilder.suffixFromSlice(actions, slice);
        
        System.out.println("RANK " + suffix.rank() + " FOR " + suffix);       
        return suffix.rank();
    }

    
    private List<Conjunction> getPathsForMemorable(SDT sdt, Register r) {
        List<Conjunction> cand = new ArrayList<>();
        
        for (Conjunction c : sdt.getPathsAsExpressions(constants, true)) {
            if (isMemorablePath(c, r, true, sdt)) {
                cand.add(c);
            }
        }
        for (Conjunction c : sdt.getPathsAsExpressions(constants, false)) {
            if (isMemorablePath(c, r, false, sdt)) {
                cand.add(c);
            }
        }        
        return cand;
    }
    
    private boolean isMemorablePath(Conjunction c, Register r, boolean accepting, SDT sdt) {
        boolean relevant = false;
        List<GuardExpression> ges = new ArrayList<>();        
        for (AtomicGuardExpression e : c.getAtoms()) {
            if (e.getSymbolicDataValues().contains(r) && 
                    e.getRelation() != Relation.NOT_EQUALS &&
                    e.getRelation() != Relation.NOT_SUCC &&
                    e.getRelation() != Relation.NOT_IN_WIN &&
                    e.getRelation() != Relation.GREATER) { // NOT SURE ABOUT THIS LAST ONE ...
                
                relevant = true;
                ges.add(new Negation(e));
            } else {
                ges.add(e);
            }
        }
        // Default case or irrelevant branch ...
        if (!relevant) {
            return false;
        }

        List<Conjunction> otherPaths = sdt.getPathsAsExpressions(constants, !accepting);
        
        Conjunction path = new Conjunction(ges.toArray(new GuardExpression[] {}));
        Disjunction other = new Disjunction(otherPaths.toArray(new GuardExpression[] {}));
        
        Conjunction test = new Conjunction(path, other);
        return solver.isSatisfiable(test);
    }

}
