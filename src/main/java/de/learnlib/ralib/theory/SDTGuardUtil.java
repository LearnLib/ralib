package de.learnlib.ralib.theory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.IntervalGuard;

public class SDTGuardUtil {

    public static Set<SDTGuard> mergeWith(SDTGuard thisGuard, SDTGuard other,
                                          List<SymbolicDataValue> regPotential) {

        if (thisGuard instanceof IntervalGuard) {
            IntervalGuard intervalGuard = (IntervalGuard) thisGuard;
            Set<SDTGuard> guards = new LinkedHashSet<>();
            if (other instanceof IntervalGuard) {
                guards.addAll(intervalGuard.mergeIntervals((IntervalGuard) other));
            } else if (other instanceof DisequalityGuard dGuard) {
                if ((intervalGuard.isBiggerGuard() && intervalGuard.leftLimit.equals(dGuard.getRegister()))
                        || (intervalGuard.isSmallerGuard() && intervalGuard.rightLimit.equals(dGuard.getRegister()))) {

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

        if (thisGuard instanceof DisequalityGuard) {
            DisequalityGuard disequalityGuard = (DisequalityGuard) thisGuard;
            Set<SDTGuard> guards = new LinkedHashSet<>();
            if (other instanceof EqualityGuard) {
                if (!(other.equals(disequalityGuard.toDeqGuard()))) {
                    guards.add(disequalityGuard);
                    guards.add(other);
                }
            }
            else if (other instanceof DisequalityGuard) {
                guards.add(disequalityGuard);
                guards.add(other);
            }
            else {
                guards.addAll(mergeWith(other, disequalityGuard,  regPotential));
            }
            return guards;
        }

        if (thisGuard instanceof EqualityGuard) {
            EqualityGuard equalityGuard = (EqualityGuard) thisGuard;
            Set<SDTGuard> guards = new LinkedHashSet<>();
            if (other instanceof DisequalityGuard) {
                if (!(other.equals(equalityGuard.toDeqGuard()))) {
                    guards.add(equalityGuard);
                    guards.add(other);
                }
            } else if (other instanceof EqualityGuard) {
                if (!(equalityGuard.equals(other))) {
                    guards.add(other);
                }
                guards.add(equalityGuard);
            } else if (other instanceof SDTOrGuard) {
                for (SDTGuard s : ((SDTOrGuard)other).getGuards()) {
                    guards.addAll(mergeWith(equalityGuard, s, regPotential));
                }
            }else {
                //System.out.println("attempt to merge " + this + " with " + other);
                guards.addAll(mergeWith(other, equalityGuard, regPotential));

            }
            return guards;
        }

        if (thisGuard instanceof SDTOrGuard) {
            return mergeWith(other, thisGuard, regPotential);
        }

        throw new RuntimeException("this should not happen");
    }

}
