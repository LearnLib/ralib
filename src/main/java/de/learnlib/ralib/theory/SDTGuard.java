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
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import java.util.List;
import java.util.Set;

/**
 *
 * @author falk
 */
public abstract class SDTGuard {

    //TODO: this should probably be a special sdtparameter
    protected final SuffixValue parameter;

    public abstract List<SDTGuard> unwrap();

    public SuffixValue getParameter() {
        return this.parameter;
    }

    public SDTGuard(SuffixValue param) {

        this.parameter = param;

    }

    public SDTGuard(SDTGuard other) {
    	this.parameter = (SuffixValue) other.parameter.copy();
    }

    public TransitionGuard toTG() {
        return new TransitionGuard(this.toExpr());
    }

    public abstract GuardExpression toExpr();

    public abstract SDTGuard relabel(VarMapping relabelling);

    public abstract Set<SDTGuard> mergeWith(SDTGuard other, List<SymbolicDataValue> regPotential);

    public abstract SDTGuard copy();

//    private Set<SDTGuard> mergeIfWith(SDTIfGuard thisIf, SDTIfGuard otherIf) {
//        Set<SDTGuard> ifGuard = new LinkedHashSet<>();
//        ifGuard.add(otherIf);
//        return mergeIfWith(thisIf, ifGuard);
//    }
//
//    private Set<SDTGuard> mergeIfWith(SDTIfGuard thisIf, Set<SDTGuard> otherOr) {
////        System.out.println("mergeIfWith Set: thisIf " + thisIf + ", otherOr " + otherOr);
//        Set<SDTGuard> otherGuards = new LinkedHashSet<>();
//        otherGuards.addAll(otherOr);
//        if (otherGuards.contains(thisIf.toDeqGuard())) {
////            System.out.println("contradiction");
//            otherGuards.remove(thisIf.toDeqGuard());
//            // disequality + equality = true
//            if (!((thisIf instanceof EqualityGuard) || thisIf instanceof DisequalityGuard)) {
////                System.out.println("neither is eq or deq");
//                otherGuards.add(new DisequalityGuard(
//                        thisIf.getParameter(), thisIf.getRegister()));
//            }
//        } else {
//            otherGuards.add(thisIf);
//        }
////        System.out.println("otherGuards " + otherGuards);
//        return otherGuards;
//    }
//
//    private Set<SDTGuard> mergeIfWith(SDTIfGuard thisIf, SDTAndGuard otherAnd) {
//        Set<SDTGuard> ifGuard = new LinkedHashSet<>();
//        ifGuard.add(thisIf);
//        return mergeAndWith(otherAnd, ifGuard);
//    }
//
////    private Set<SDTGuard> mergeAndWith(SDTAndGuard thisAnd, SDTAndGuard otherAnd) {
////        Set<SDTGuard> andGuard = new LinkedHashSet<>();
////        andGuard.add(otherAnd);
////        return mergeAndWith(thisAnd, andGuard);
////    }
////
////    private Set<SDTGuard> mergeAndWith(SDTAndGuard thisAnd, SDTIfGuard otherIf) {
////        return mergeIfWith(otherIf, thisAnd);
////    }
//    private Set<SDTGuard> mergeAndWith(SDTAndGuard thisAnd, Set<SDTGuard> _merged) {
//        //System.out.println(thisAnd + " merges with " + _merged);
//        Set<SDTIfGuard> ifs = new LinkedHashSet<>();
//        List<SDTGuard> thisGuards = thisAnd.getGuards();
//        Set<SDTGuard> merged = new LinkedHashSet<>();
//        merged.addAll(_merged);
//        for (SDTGuard x : thisGuards) {
//            assert x instanceof SDTIfGuard;
//            SDTIfGuard ifX = (SDTIfGuard) x;
//            if (merged.contains(ifX.toDeqGuard())) {
//                merged.remove(ifX.toDeqGuard());
//                if (!((ifX instanceof EqualityGuard) || ifX instanceof DisequalityGuard)) {
//                    merged.add(new DisequalityGuard(ifX.getParameter(), ifX.getRegister()));
//                }
//            } else {
//                ifs.add(ifX);
//            }
//        }
//        if (ifs.size() == 1) {
//            merged.addAll(ifs);
//        } else if (ifs.size() > 1) {
//            merged.add(new SDTAndGuard(thisAnd.parameter, ifs.toArray(new SDTIfGuard[]{})));
//
//        }
//        //System.out.println("result: " + merged);
//        return merged;
//    }

//    private Set<SDTGuard> mergeOrWith(SDTOrGuard thisOr, SDTIfGuard otherIf) {
//        return mergeIfWith(otherIf, thisOr.guardSet);
//    }
//
//    private Set<SDTGuard> mergeOrWith(SDTOrGuard thisOr, SDTAndGuard otherAnd) {
//        return mergeAndWith(otherAnd, thisOr.guardSet);
//    }
    //public abstract SDTGuard mergeWith(Set<SDTGuard> others);

}
