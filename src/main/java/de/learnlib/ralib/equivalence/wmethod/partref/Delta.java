package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.LinkedHashSet;
import java.util.Set;

import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;

public class Delta {

    public record RegProgress(Location loc, Set<Register> regs) {}

    private Set<RegProgress> regProgress;

    private Set<Location> separated;

    public Delta(Block block, Set<SymbolicState> preconditions) {
        regProgress = new LinkedHashSet<>();
        separated = new LinkedHashSet<>(block.getLocations());

        Set<SymbolicState> states = block.getStates();
        Set<Location> shared = new LinkedHashSet<>();
        for (SymbolicState prec : preconditions) {
            Location loc = prec.getLocation();
            for (SymbolicState state : states) {
                if (state.getLocation().equals(loc)) {
                    shared.add(loc);

                    Set<Register> regs = new LinkedHashSet<>(prec.getIdentifiedRegisters());
                    regs.removeAll(state.getIdentifiedRegisters());
                    regProgress.add(new RegProgress(loc, regs));
                }
            }
        }
        separated.removeAll(shared);
    }

    public boolean isSubsetOf(Delta other) {
        if (!other.regProgress.containsAll(regProgress)) {
            return false;
        }
        if (!other.separated.containsAll(separated)) {
            return false;
        }
        return true;
    }

    public boolean makesProgress() {
        for (RegProgress rp : regProgress) {
            if (!rp.regs.isEmpty()) {
                return true;
            }
        }
        return !separated.isEmpty();
    }
}
