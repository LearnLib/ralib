package de.learnlib.ralib.theory;

import java.util.*;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

public class SDTGuardUtil {

    // this is only used by inequality theory and very old code that does not make
    // sense to me. Not even sure what merge means in this case?
    @Deprecated
    public static Set<SDTGuard> mergeWith(SDTGuard thisGuard, SDTGuard other,
                                          List<SymbolicDataValue> regPotential) {

        if (thisGuard instanceof SDTGuard.IntervalGuard intervalGuard) {
            Set<SDTGuard> guards = new LinkedHashSet<>();
            if (other instanceof SDTGuard.IntervalGuard) {
                guards.addAll(IntervalGuardUtil.mergeIntervals(intervalGuard, (SDTGuard.IntervalGuard) other));
            } else if (other instanceof SDTGuard.DisequalityGuard dGuard) {
                if ((intervalGuard.isBiggerGuard() && intervalGuard.leftLimit().equals(dGuard.register()))
                        || (intervalGuard.isSmallerGuard() && intervalGuard.rightLimit().equals(dGuard.register()))) {

                    guards.add(other);
                }
                else {
                    guards.add(intervalGuard);
                    guards.add(other);
                }
                // special case for equality guards
            } else {
                guards.add(intervalGuard);
                guards.add(other);
            }
            return guards;
        }

        if (thisGuard instanceof SDTGuard.DisequalityGuard disequalityGuard) {
            Set<SDTGuard> guards = new LinkedHashSet<>();
            if (other instanceof SDTGuard.EqualityGuard) {
                if (!(other.equals(SDTGuard.toDeqGuard(disequalityGuard)))) {
                    guards.add(disequalityGuard);
                    guards.add(other);
                }
            }
            else if (other instanceof SDTGuard.DisequalityGuard) {
                guards.add(disequalityGuard);
                guards.add(other);
            }
            else {
                guards.addAll(mergeWith(other, disequalityGuard,  regPotential));
            }
            return guards;
        }

        if (thisGuard instanceof SDTGuard.EqualityGuard equalityGuard) {
            Set<SDTGuard> guards = new LinkedHashSet<>();
            if (other instanceof SDTGuard.DisequalityGuard) {
                if (!(other.equals(SDTGuard.toDeqGuard(equalityGuard)))) {
                    guards.add(equalityGuard);
                    guards.add(other);
                }
            } else if (other instanceof SDTGuard.EqualityGuard) {
                if (!(equalityGuard.equals(other))) {
                    guards.add(other);
                }
                guards.add(equalityGuard);
            } else if (other instanceof SDTGuard.SDTOrGuard orGuard) {
                for (SDTGuard s : orGuard.disjuncts()) {
                    guards.addAll(mergeWith(equalityGuard, s, regPotential));
                }
            }else {
                //System.out.println("attempt to merge " + this + " with " + other);
                guards.addAll(mergeWith(other, equalityGuard, regPotential));

            }
            return guards;
        }

        if (thisGuard instanceof SDTGuard.SDTOrGuard) {
            return mergeWith(other, thisGuard, regPotential);
        }

        throw new RuntimeException("this should not happen");
    }
}
