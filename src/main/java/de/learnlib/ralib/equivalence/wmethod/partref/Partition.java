package de.learnlib.ralib.equivalence.wmethod.partref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.Location;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;

public class Partition {

    private final List<Leaf> blocks;

    private final Map<Integer, Block> representativeBlocks;

    private final Set<Location> fullyIdentifiedLocations;

    public Partition(Collection<Leaf> blocks, Set<Location> locations) {
        this.blocks = new ArrayList<>(blocks);
        representativeBlocks = new LinkedHashMap<>();
        fullyIdentifiedLocations = new LinkedHashSet<>();
        for (Location loc : locations) {
            Block repr = null;
            int regs = -1;
            for (Block block : blocks) {
                for (SymbolicState s : block.getStates()) {
                    if (s.getLocation().equals(loc)) {
                        int identified = s.getIdentifiedRegisters().size();
                        if (identified > regs) {
                            repr = block;
                            regs = identified;
                        }
                        if (identified == loc.getRegisters().size()) {
                            fullyIdentifiedLocations.add(loc);
                        }
                    }
                }
            }
            representativeBlocks.put(loc.getRaLocation().getId(), repr);
        }
    }

    public List<Leaf> getBlocks() {
        return blocks;
    }

    public Block getRepresentativeBlock(RALocation loc) {
        return representativeBlocks.get(loc.getId());
    }

    public Block getRepresentativeBlock(Location loc) {
        return getRepresentativeBlock(loc.getRaLocation());
    }

    public Set<Location> getFullyIdentifiedLocations() {
        return fullyIdentifiedLocations;
    }

    public Set<RALocation> getFullyIdentifiedRALocations() {
        return fullyIdentifiedLocations.stream().map(l -> l.getRaLocation()).collect(Collectors.toSet());
    }

    public Constraint getConstraint(RALocation raloc) {
        Block block = representativeBlocks.get(raloc.getId());
        for (SymbolicState s : block.getStates()) {
            if (s.getLocation().getRaLocation().getId() == raloc.getId()) {
                return s.getConstraints();
            }
        }
        return null;
    }
}
