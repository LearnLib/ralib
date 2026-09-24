package de.learnlib.ralib.equivalence.wmethod.partref.constraints;

import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.SymbolicDataValue;

public class DSU {

    private final Map<SymbolicDataValue, SymbolicDataValue> parent = new LinkedHashMap<>();
    private final Map<SymbolicDataValue, Integer> rank = new LinkedHashMap<>();

    public void add(SymbolicDataValue x) {
        if (!parent.containsKey(x)) {
            parent.put(x, x);
            rank.put(x, 0);
        }
    }

    public SymbolicDataValue find(SymbolicDataValue x) {
        add(x);

        if (!parent.get(x).equals(x)) {
            parent.put(x, find(parent.get(x)));
        }

        return parent.get(x);
    }

    public void union(SymbolicDataValue a, SymbolicDataValue b) {
        a = find(a);
        b = find(b);

        if (a.equals(b)) {
            return;
        }

        int rankA = rank.get(a);
        int rankB = rank.get(b);

        if (rankA < rankB) {
            parent.put(a, b);
        } else if (rankA > rankB) {
            parent.put(b, a);
        } else {
            parent.put(b, a);
            rank.put(a, rankA + 1);
        }
    }

    public boolean same(SymbolicDataValue a, SymbolicDataValue b) {
        return find(a).equals(find(b));
    }
}
