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

import java.util.*;

import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

/**
 *
 * @author falk
 */
public sealed interface SDTGuard permits SDTGuard.DisequalityGuard, SDTGuard.EqualityGuard, SDTGuard.IntervalGuard, SDTGuard.SDTAndGuard, SDTGuard.SDTOrGuard, SDTGuard.SDTTrueGuard {

    record SDTTrueGuard(SymbolicDataValue.SuffixValue parameter) implements SDTGuard {
        @Override
        public String toString() {
            return "TRUE: " + parameter;
        }

        @Override
        public SuffixValue getParameter() {return this.parameter; }

        @Override
        public Set<SDTGuardElement> getRegisters() { return Set.of(); }
    }

    record EqualityGuard(SymbolicDataValue.SuffixValue parameter, SDTGuardElement register) implements SDTGuard {
        @Override
        public String toString() {
            return "(" + parameter + "=" + register + ")";
        }

        @Override
        public SuffixValue getParameter() {return this.parameter; }

        @Override
        public Set<SDTGuardElement> getRegisters() { return Set.of(register); }
    }

    record DisequalityGuard(SymbolicDataValue.SuffixValue parameter, SDTGuardElement register) implements SDTGuard {
        @Override
        public String toString() {
            return "(" + parameter + "!=" +register + ")";
        }

        @Override
        public SuffixValue getParameter() {return this.parameter; }

        @Override
        public Set<SDTGuardElement> getRegisters() { return Set.of(register); }
    }

    record IntervalGuard(SymbolicDataValue.SuffixValue parameter,
                         SDTGuardElement leftLimit, SDTGuardElement rightLimit,
                         boolean leftClosed, boolean rightClosed) implements SDTGuard {

        public IntervalGuard(SuffixValue param, SDTGuardElement leftLimit, SDTGuardElement rightLimit) {
            this(param, leftLimit, rightLimit, false, false);
        }

        @Override
        public String toString() {
            if (leftLimit == null) {
                return "(" + this.getParameter().toString() + (rightClosed ? "<=" : "<") + this.rightLimit.toString() + ")";
            }
            if (rightLimit == null) {
                return "(" + this.getParameter().toString() + (leftClosed ? ">=" : ">") + this.leftLimit.toString() + ")";
            }
            return "(" + leftLimit.toString() +
                    (leftClosed ? "<=" : "<") +
                    this.getParameter().toString() +
                    (rightClosed ? "<=" : "<") +
                    this.rightLimit.toString() +
                    ")";
        }

        @Override
        public SuffixValue getParameter() {return this.parameter; }

        @Override
        public Set<SDTGuardElement> getRegisters() {
            Set<SDTGuardElement> regs = new LinkedHashSet<>();
            if (leftLimit != null) regs.add(leftLimit);
            if (rightLimit != null) regs.add(rightLimit);
            return regs;
        }

        public boolean isSmallerGuard() {
            return leftLimit == null;
        }

        public boolean isBiggerGuard() {
            return rightLimit == null;
        }

        public boolean isIntervalGuard() { return (leftLimit != null && rightLimit != null); }

        public boolean isLeftClosed() {
            return leftClosed;
        }

        public boolean isRightClosed() {
            return rightClosed;
        }

        public static IntervalGuard lessGuard(SuffixValue param, SDTGuardElement r) {
            return new IntervalGuard(param, null, r, false, false);
        }

        public static IntervalGuard lessOrEqualGuard(SuffixValue param, SDTGuardElement r) {
            return new IntervalGuard(param, null, r, false, true);
        }

        public static IntervalGuard greaterGuard(SuffixValue param, SDTGuardElement r) {
            return new IntervalGuard(param, r, null, false, false);
        }

        public static IntervalGuard greaterOrEqualGuard(SuffixValue param, SDTGuardElement r) {
            return new IntervalGuard(param, r, null, true, false);
        }
    }

    record SDTAndGuard(SymbolicDataValue.SuffixValue parameter, List<SDTGuard> conjuncts) implements SDTGuard {
        @Override
        public String toString() {
            String p = "ANDCOMPOUND: " + parameter;
            if (conjuncts.isEmpty()) {
                return p + "empty";
            }
            return p + conjuncts;
        }

        @Override
        public SuffixValue getParameter() {return this.parameter; }

        @Override
        public Set<SDTGuardElement> getRegisters() {
            Set<SDTGuardElement> ret = new HashSet<>();
            conjuncts.stream().forEach( x -> ret.addAll(x.getRegisters() ));
            return ret;
        }

        // FIXME: And guards rely on a set representation for equals and hash code, that seems wrong

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SDTAndGuard that = (SDTAndGuard) o;
            return Objects.equals(parameter, that.parameter) &&
                    Objects.equals(new HashSet<>(conjuncts), new HashSet<>(that.conjuncts));
        }

        @Override
        public int hashCode() {
            return Objects.hash(parameter, new HashSet<>(conjuncts));
        }
    }

    record SDTOrGuard(SymbolicDataValue.SuffixValue parameter, List<SDTGuard> disjuncts) implements SDTGuard {
        @Override
        public String toString() {
            String p = "ORCOMPOUND: " + parameter;
            if (disjuncts.isEmpty()) {
                return p + "empty";
            }
            return p + disjuncts;
        }

        @Override
        public SuffixValue getParameter() {return this.parameter; }

        @Override
        public Set<SDTGuardElement> getRegisters() {
            Set<SDTGuardElement> ret = new HashSet<>();
            disjuncts.stream().forEach( x -> ret.addAll(x.getRegisters() ));
            return ret;
        }

        @Override
        public List<SDTGuard> disjuncts() {
            return disjuncts;
        }

        // FIXME: Or guards rely on a set representation for equals and hash code, that seems wrong

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SDTOrGuard that = (SDTOrGuard) o;
            return Objects.equals(parameter, that.parameter) &&
                    Objects.equals(new HashSet<>(disjuncts), new HashSet<>(that.disjuncts));
        }

        @Override
        public int hashCode() {
            return Objects.hash(parameter, new HashSet<>(disjuncts));
        }
    }

    SuffixValue getParameter();

    Set<SDTGuardElement> getRegisters();

    static Set<SDTGuardElement> getComparands(SDTGuard in, SDTGuardElement dv) {
        Set<SDTGuardElement> comparands = new LinkedHashSet<>();
        switch (in) {
            case SDTGuard.EqualityGuard g:
                if (g.parameter.equals(dv)) comparands.add(g.register);
                if (g.register.equals(dv)) comparands.add(g.parameter);
                return comparands;
            case SDTGuard.DisequalityGuard g:
                if (g.parameter.equals(dv)) comparands.add(g.register);
                if (g.register.equals(dv)) comparands.add(g.parameter);
                return comparands;
            case SDTGuard.IntervalGuard g:
                // FIXME: this was copied from original class but does not seem to make any sense
                if (dv.equals(g.leftLimit)) comparands.add(g.rightLimit);
                if (dv.equals(g.rightLimit)) comparands.add(g.leftLimit);
                return comparands;
            case SDTGuard.SDTAndGuard g:
                g.conjuncts.forEach((x) -> comparands.addAll(getComparands(x, dv)));
                return comparands;
            case SDTGuard.SDTOrGuard g:
                g.disjuncts.forEach((x) -> comparands.addAll(getComparands(x, dv)));
                return comparands;
            case SDTGuard.SDTTrueGuard g:
                return comparands;
            default:	// needed only for Java 17?
                throw new RuntimeException("should not be reachable");
        }
    }

    // TODO: previously parameters and registers were copied but that is not necessary?
    static SDTGuard copy(SDTGuard in) {
        switch (in) {
            case SDTGuard.EqualityGuard g:
                return new SDTGuard.EqualityGuard(g.parameter, g.register);
            case SDTGuard.DisequalityGuard g:
                return new SDTGuard.DisequalityGuard(g.parameter, g.register);
            case SDTGuard.IntervalGuard g:
                return new SDTGuard.IntervalGuard(g.parameter, g.leftLimit, g.rightLimit, g.leftClosed, g.rightClosed);
            case SDTGuard.SDTAndGuard g:
                return new SDTGuard.SDTAndGuard(g.parameter,
                        g.conjuncts.stream().map( x -> copy(x)).toList());
            case SDTGuard.SDTOrGuard g:
                return new SDTGuard.SDTOrGuard(g.parameter,
                        g.disjuncts.stream().map( x -> copy(x)).toList());
            case SDTGuard.SDTTrueGuard g:
                return new SDTGuard.SDTTrueGuard(g.parameter);
            default:	// needed only for Java 17?
                throw new RuntimeException("should not be reachable");
        }
    }

    private static <T extends SDTGuardElement> T newValueIfExists(SDTRelabeling relabelling, T oldValue) {
        if (oldValue == null || SDTGuardElement.isConstant(oldValue)) return oldValue;
        T newValue = (T) relabelling.get(oldValue);
        return newValue != null ? newValue : oldValue;
    }

    static SDTGuard relabel(SDTGuard in, SDTRelabeling remap) {
        switch (in) {
            case SDTGuard.EqualityGuard g:
                return new SDTGuard.EqualityGuard(newValueIfExists(remap, g.parameter),
                        newValueIfExists(remap, g.register));
            case SDTGuard.DisequalityGuard g:
                return new SDTGuard.DisequalityGuard(newValueIfExists(remap, g.parameter),
                        newValueIfExists(remap, g.register));
            case SDTGuard.IntervalGuard g:
                return new SDTGuard.IntervalGuard(newValueIfExists(remap, g.parameter),
                        newValueIfExists(remap, g.leftLimit), newValueIfExists(remap, g.rightLimit), g.leftClosed, g.rightClosed);
            case SDTGuard.SDTAndGuard g:
                return new SDTGuard.SDTAndGuard(newValueIfExists(remap, g.parameter),
                        g.conjuncts.stream().map(ig -> relabel(ig, remap)).toList());
            case SDTGuard.SDTOrGuard g:
                return new SDTGuard.SDTOrGuard(newValueIfExists(remap, g.parameter),
                        g.disjuncts.stream().map(ig -> relabel(ig, remap)).toList());
            case SDTGuard.SDTTrueGuard g:
                return new SDTGuard.SDTTrueGuard(newValueIfExists(remap, g.parameter));
            default:	// needed only for Java 17?
                throw new RuntimeException("should not be reachable");
        }
    }

    static Expression<Boolean> toExpr(SDTGuard in) {
        switch (in) {
            case SDTGuard.EqualityGuard g:
                return new NumericBooleanExpression(g.register.asExpression(), NumericComparator.EQ, g.parameter);
            case SDTGuard.DisequalityGuard g:
                return new NumericBooleanExpression(g.register.asExpression(), NumericComparator.NE, g.parameter);
            case SDTGuard.IntervalGuard g:
                if (g.leftLimit == null)  return new NumericBooleanExpression(g.parameter,
                        g.rightClosed ? NumericComparator.LE : NumericComparator.LT, g.rightLimit.asExpression());
                if (g.rightLimit == null) return new NumericBooleanExpression(g.parameter,
                        g.leftClosed ? NumericComparator.GE : NumericComparator.GT, g.leftLimit.asExpression());
                Expression<Boolean> smaller = new NumericBooleanExpression(g.parameter,
                        g.leftClosed ? NumericComparator.LE : NumericComparator.LT, g.rightLimit.asExpression());
                Expression<Boolean> bigger = new NumericBooleanExpression(g.parameter,
                        g.rightClosed ? NumericComparator.GE : NumericComparator.GT, g.leftLimit.asExpression());
                return ExpressionUtil.and(smaller, bigger);
            case SDTGuard.SDTAndGuard g:
                List<Expression<Boolean>> andList = g.conjuncts.stream().map( x -> toExpr(x)).toList();
                if (andList.isEmpty()) return ExpressionUtil.TRUE;
                if (andList.size() == 1) return andList.get(0);
                return ExpressionUtil.and(andList.toArray(new Expression[]{}));
            case SDTGuard.SDTOrGuard g:
                List<Expression<Boolean>> orList = g.disjuncts.stream().map( x -> toExpr(x)).toList();
                if (orList.isEmpty()) return ExpressionUtil.TRUE;
                if (orList.size() == 1) return orList.get(0);
                return ExpressionUtil.or(orList.toArray(new Expression[]{}));
            case SDTGuard.SDTTrueGuard g:
                return ExpressionUtil.TRUE;
            default:	// needed only for Java 17?
                throw new RuntimeException("should not be reachable");
        }
    }

    static SDTGuard toDeqGuard(SDTGuard in) {
        switch (in) {
            case SDTGuard.EqualityGuard g:
                return new SDTGuard.DisequalityGuard(g.parameter, g.register);
            case SDTGuard.DisequalityGuard g:
                return new SDTGuard.EqualityGuard(g.parameter, g.register);
            case SDTGuard.IntervalGuard g:
                // FIXME: copied from old implementation but does not seem to make sense
                assert !g.isIntervalGuard();
                SDTGuardElement r = g.isSmallerGuard() ? g.rightLimit : g.leftLimit;
                return new DisequalityGuard(g.parameter,r);
            case SDTGuard.SDTAndGuard g:
                throw new RuntimeException("not refactored yet");
            case SDTGuard.SDTOrGuard g:
                throw new RuntimeException("not refactored yet");
            case SDTGuard.SDTTrueGuard g:
                throw new RuntimeException("not refactored yet");
            default:	// needed only for Java 17?
                throw new RuntimeException("should not be reachable");
        }
    }
}
