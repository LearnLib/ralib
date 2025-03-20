/*
 * Copyright (C) 2014 falk.
 *
 * g library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * g library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with g library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.theory;

import java.util.LinkedHashSet;
import java.util.Set;

import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SymbolicDataValue;

/**
 *
 * @author falk
 */
final public class IntervalGuardUtil  {

    // FIXME: g method should have a better name
    public static SDTGuard.EqualityGuard toEqGuard(SDTGuard.IntervalGuard g) {
        assert !g.isIntervalGuard();
        SDTGuardElement r = null;
        if (g.isSmallerGuard()) {
            r = g.rightLimit();
        }
        else {
            r = g.leftLimit();
        }
        return new SDTGuard.EqualityGuard(g.parameter(),r);
    }

    // FIXME: g method should have a better name
    public SDTGuard.DisequalityGuard toDeqGuard(SDTGuard.IntervalGuard g) {
        assert !g.isIntervalGuard();
        SDTGuardElement r = null;
        if (g.isSmallerGuard()) {
            r = g.rightLimit();
        }
        else {
            r = g.leftLimit();
        }
        return new SDTGuard.DisequalityGuard(g.parameter(),r);
    }

    // FIXME: g method does not make any sense
    public static SDTGuard.IntervalGuard flip(SDTGuard.IntervalGuard g) {
        return new SDTGuard.IntervalGuard(g.parameter(), g.rightLimit(), g.leftLimit());
    }


    // merge bigger with something
    private static Set<SDTGuard> bMergeIntervals(SDTGuard.IntervalGuard g, SDTGuard.IntervalGuard other) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        SDTGuardElement l = g.leftLimit();
        if (other.isBiggerGuard()) {
            //          System.out.println("other " + other + " is bigger");
            guards.add(g);
            guards.add(other);
        } else if (other.isSmallerGuard()) {
//            System.out.println("other " + other + " is smaller");
//            System.out.println("see if " + l + " equals " + other.rightLimit() + "?");
            if (l.equals(other.rightLimit())) {
//                System.out.println("yes, adding disequalityguard");
                guards.add(new SDTGuard.DisequalityGuard(g.parameter(), l));
            } else {
//                System.out.println("no, merging into interval guard");
//                guards.add(new IntervalGuard(g.parameter, l, other.rightLimit()));
                guards.add(g);
                guards.add(other);
            }
        } else {
//            System.out.println("other " + other + " is interv");

            if (l.equals(other.rightLimit())) {
                guards.add(new SDTGuard.IntervalGuard(g.parameter(), other.leftLimit(), null));
                guards.add(new SDTGuard.DisequalityGuard(g.parameter(), l));
            } else if (l.equals(other.leftLimit())) {
                guards.add(g);
            } else {
                guards.add(g);
                guards.add(other);
            }

        }
        return guards;
    }

    // merge smaller with something
    private static Set<SDTGuard> sMergeIntervals(SDTGuard.IntervalGuard g, SDTGuard.IntervalGuard other) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        SDTGuardElement r = g.rightLimit();
        if (other.isBiggerGuard()) {
            return bMergeIntervals(other, g);
        } else if (other.isSmallerGuard()) {
            guards.add(g);
            guards.add(other);
        } else {
            if (r.equals(other.leftLimit())) {
                guards.add(new SDTGuard.IntervalGuard(g.parameter(), null, other.rightLimit()));
                guards.add(new SDTGuard.DisequalityGuard(g.parameter(), r));
            } else if (r.equals(other.rightLimit())) {
                guards.add(g);
            } else {
                guards.add(g);
                guards.add(other);
            }
        }
        return guards;
    }

    // merge interval with something
    private static Set<SDTGuard> iMergeIntervals(SDTGuard.IntervalGuard g, SDTGuard.IntervalGuard other) {
        Set<SDTGuard> guards = new LinkedHashSet<>();
        SDTGuardElement l = g.leftLimit();
        SDTGuardElement r = g.rightLimit();
        if (other.isBiggerGuard()) {
            return bMergeIntervals(other, g);
        } else if (other.isSmallerGuard()) {
            return sMergeIntervals(other, g);
        } else {
            SDTGuardElement oL = other.leftLimit();
            SDTGuardElement oR = other.rightLimit();
            if (l.equals(oR)) {
                if (r.equals(oL)) {
                    guards.add(new SDTGuard.DisequalityGuard(g.parameter(), l));
                    guards.add(new SDTGuard.DisequalityGuard(g.parameter(), r));
                } else {
                    guards.add(new SDTGuard.IntervalGuard(g.parameter(), oL, r));
                    guards.add(new SDTGuard.DisequalityGuard(g.parameter(), l));
                }
            } else {
                if (r.equals(oL)) {
                    guards.add(new SDTGuard.IntervalGuard(g.parameter(), l, oR));
                    guards.add(new SDTGuard.DisequalityGuard(g.parameter(), r));
                } else {
                    guards.add(g);
                    guards.add(other);
                }
            }
        }
        return guards;
    }

    public static Set<SDTGuard> mergeIntervals(SDTGuard.IntervalGuard g, SDTGuard.IntervalGuard other) {
//        System.out.println("other i-guard: " + other);

        // FIXME: shouldn't bMergeIntervals and sMergeIntervals be symmetric?

        if (g.isBiggerGuard()) {
//            System.out.println(g + " is bigger, left limit is: " + g.leftLimit);
            Set<SDTGuard> gs = bMergeIntervals(g, other);
//            System.out.println("returningB: " + gs);
            return gs;
        }
        if (g.isSmallerGuard()) {
//            System.out.println(g + " is smaller, right limit is: " + g.rightLimit);
            Set<SDTGuard> gs = sMergeIntervals(g, other);
//            System.out.println("returningS: " + gs);
            return gs;
        }

//        System.out.println("is interv");
        return iMergeIntervals(g, other);

    }




}
