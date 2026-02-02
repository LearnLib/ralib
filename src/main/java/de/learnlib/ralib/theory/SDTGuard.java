/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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
                         SDTGuardElement smallerElement, SDTGuardElement greaterElement,
                         boolean smallerEqual, boolean greaterEqual) implements SDTGuard {

        public IntervalGuard(SuffixValue param, SDTGuardElement smallerElement, SDTGuardElement greaterElement) {
            this(param, smallerElement, greaterElement, false, false);
        }

        @Override
        public String toString() {
            if (smallerElement == null) {
            	// no smaller element, parameter is less than the greater (e.g., s1 < r1)
                return "(" + this.getParameter().toString() + (greaterEqual ? "<=" : "<") + this.greaterElement.toString() + ")";
            }
            if (greaterElement == null) {
            	// no greater element, parameter is greater than the smaller (e.g., s1 > r1)
                return "(" + this.getParameter().toString() + (smallerEqual ? ">=" : ">") + this.smallerElement.toString() + ")";
            }
            // interval (e.g., r1 < s1 < r2)
            return "(" + smallerElement.toString() +
                    (smallerEqual ? "<=" : "<") +
                    this.getParameter().toString() +
                    (greaterEqual ? "<=" : "<") +
                    this.greaterElement.toString() +
                    ")";
        }

        @Override
        public SuffixValue getParameter() {return this.parameter; }

        @Override
        public Set<SDTGuardElement> getRegisters() {
            Set<SDTGuardElement> regs = new LinkedHashSet<>();
            if (smallerElement != null) regs.add(smallerElement);
            if (greaterElement != null) regs.add(greaterElement);
            return regs;
        }

        public boolean isSmallerGuard() {
            return smallerElement == null;
        }

        public boolean isBiggerGuard() {
            return greaterElement == null;
        }

        public boolean isIntervalGuard() { return (smallerElement != null && greaterElement != null); }

        public boolean isLeftClosed() {
            return smallerEqual;
        }

        public boolean isRightClosed() {
            return greaterEqual;
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

        // And guards rely on a set representation for equals and
        // hash code as we want to ignore the order of the conjuncts

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

        // Or guards rely on a set representation for equals and
        // hash code as we want to ignore the order of the disjuncts

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
        return switch (in) {
            case SDTGuard.EqualityGuard g -> {
                if (g.parameter.equals(dv)) comparands.add(g.register);
                if (g.register.equals(dv)) comparands.add(g.parameter);
                yield comparands;
            }
            case SDTGuard.DisequalityGuard g -> {
                if (g.parameter.equals(dv)) comparands.add(g.register);
                if (g.register.equals(dv)) comparands.add(g.parameter);
                yield comparands;
            }
            case SDTGuard.IntervalGuard g -> {
                if (dv.equals(g.smallerElement) || dv.equals(g.greaterElement)) comparands.add(g.parameter);
                if (dv.equals(g.parameter)) {
                	if (g.smallerElement != null) comparands.add(g.smallerElement);
                	if (g.greaterElement != null) comparands.add(g.greaterElement);
                }
                yield comparands;
            }
            case SDTGuard.SDTAndGuard g -> {
                g.conjuncts.forEach((x) -> comparands.addAll(getComparands(x, dv)));
                yield comparands;
            }
            case SDTGuard.SDTOrGuard g -> {
                g.disjuncts.forEach((x) -> comparands.addAll(getComparands(x, dv)));
                yield comparands;
            }
            case SDTGuard.SDTTrueGuard g ->
                comparands;
        };
    }

    private static <T extends SDTGuardElement> T newValueIfExists(SDTRelabeling relabelling, T oldValue) {
        if (oldValue == null || SDTGuardElement.isConstant(oldValue)) return oldValue;
        T newValue = (T) relabelling.get(oldValue);
        return newValue != null ? newValue : oldValue;
    }

    static SDTGuard relabel(SDTGuard in, SDTRelabeling remap) {
        return switch (in) {
            case SDTGuard.EqualityGuard g ->
                new SDTGuard.EqualityGuard(newValueIfExists(remap, g.parameter),
                        newValueIfExists(remap, g.register));
            case SDTGuard.DisequalityGuard g ->
                new SDTGuard.DisequalityGuard(newValueIfExists(remap, g.parameter),
                        newValueIfExists(remap, g.register));
            case SDTGuard.IntervalGuard g ->
                new SDTGuard.IntervalGuard(newValueIfExists(remap, g.parameter),
                        newValueIfExists(remap, g.smallerElement), newValueIfExists(remap, g.greaterElement), g.smallerEqual, g.greaterEqual);
            case SDTGuard.SDTAndGuard g ->
                new SDTGuard.SDTAndGuard(newValueIfExists(remap, g.parameter),
                        g.conjuncts.stream().map(ig -> relabel(ig, remap)).toList());
            case SDTGuard.SDTOrGuard g ->
                new SDTGuard.SDTOrGuard(newValueIfExists(remap, g.parameter),
                        g.disjuncts.stream().map(ig -> relabel(ig, remap)).toList());
            case SDTGuard.SDTTrueGuard g ->
                new SDTGuard.SDTTrueGuard(newValueIfExists(remap, g.parameter));
        };
    }

    static Expression<Boolean> toExpr(SDTGuard in) {
        return switch (in) {
            case SDTGuard.EqualityGuard g ->
                new NumericBooleanExpression(g.register.asExpression(), NumericComparator.EQ, g.parameter);
            case SDTGuard.DisequalityGuard g ->
                new NumericBooleanExpression(g.register.asExpression(), NumericComparator.NE, g.parameter);
            case SDTGuard.IntervalGuard g -> {
                if (g.smallerElement == null)
                    yield new NumericBooleanExpression(g.parameter,
                        g.greaterEqual ? NumericComparator.LE : NumericComparator.LT, g.greaterElement.asExpression());
                if (g.greaterElement == null)
                    yield new NumericBooleanExpression(g.parameter,
                        g.smallerEqual ? NumericComparator.GE : NumericComparator.GT, g.smallerElement.asExpression());
                Expression<Boolean> smaller = new NumericBooleanExpression(g.parameter,
                        g.smallerEqual ? NumericComparator.GE : NumericComparator.GT, g.smallerElement.asExpression());
                Expression<Boolean> bigger = new NumericBooleanExpression(g.parameter,
                        g.greaterEqual ? NumericComparator.LE : NumericComparator.LT, g.greaterElement.asExpression());
                yield ExpressionUtil.and(smaller, bigger);
            }
            case SDTGuard.SDTAndGuard g -> {
                List<Expression<Boolean>> andList = g.conjuncts.stream().map( x -> toExpr(x)).toList();
                if (andList.isEmpty()) yield ExpressionUtil.TRUE;
                if (andList.size() == 1) yield andList.get(0);
                yield ExpressionUtil.and(andList.toArray(new Expression[]{}));
            }
            case SDTGuard.SDTOrGuard g -> {
                List<Expression<Boolean>> orList = g.disjuncts.stream().map( x -> toExpr(x)).toList();
                if (orList.isEmpty()) yield ExpressionUtil.TRUE;
                if (orList.size() == 1) yield orList.get(0);
                yield ExpressionUtil.or(orList.toArray(new Expression[]{}));
            }
            case SDTGuard.SDTTrueGuard g ->
                ExpressionUtil.TRUE;
        };
    }

    static SDTGuard toDeqGuard(SDTGuard in) {
        return switch (in) {
            case SDTGuard.EqualityGuard g ->
                new SDTGuard.DisequalityGuard(g.parameter, g.register);
            case SDTGuard.DisequalityGuard g ->
                new SDTGuard.EqualityGuard(g.parameter, g.register);
            case SDTGuard.IntervalGuard g -> {
                // FIXME: copied from old implementation but does not seem to make sense
                assert !g.isIntervalGuard();
                SDTGuardElement r = g.isSmallerGuard() ? g.greaterElement : g.smallerElement;
                yield new DisequalityGuard(g.parameter,r);
            }
            case SDTGuard.SDTAndGuard g ->
                throw new RuntimeException("not refactored yet");
            case SDTGuard.SDTOrGuard g ->
                throw new RuntimeException("not refactored yet");
            case SDTGuard.SDTTrueGuard g ->
                throw new RuntimeException("not refactored yet");
        };
    }
}
