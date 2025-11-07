package de.learnlib.ralib.ct;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.util.DataUtils;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.SDT;

public class CTPath {
	private final Map<SymbolicSuffix, SDT> sdts;
	private final MemorableSet memorable;

	private boolean ioMode;

	public CTPath(boolean ioMode) {
		this.sdts = new LinkedHashMap<>();
		this.memorable = new MemorableSet();
		this.ioMode = ioMode;
	}

	public void putSDT(SymbolicSuffix suffix, SDT sdt) {
		assert !sdts.containsKey(suffix);
		sdts.put(suffix, sdt);
		memorable.addAll(sdt.getDataValues());
	}

	public MemorableSet getMemorable() {
		return memorable;
	}

	public SDT getSDT(SymbolicSuffix suffix) {
		return sdts.get(suffix);
	}

	public Map<SymbolicSuffix, SDT> getSDTs() {
		return sdts;
	}

	public boolean isAccepting() {
		SDT s = sdts.get(RaStar.EMPTY_SUFFIX);
		return s.isAccepting();
	}

	public boolean isEquivalent(CTPath other, Bijection<DataValue> renaming, ConstraintSolver solver) {
		if (!typeSizesMatch(other)) {
			return false;
		}

		if (!memorable.equals(other.memorable.relabel(renaming))) {
			return false;
		}

		for (Map.Entry<SymbolicSuffix, SDT> e : sdts.entrySet()) {
			SDT sdt1 = e.getValue();
			SDT sdt2 = other.sdts.get(e.getKey());

			if (!sdt1.isEquivalent(sdt2, renaming)) {
				return false;
			}
		}
		return true;
	}

	protected boolean typeSizesMatch(CTPath other) {
		if (!DataUtils.typedSize(memorable).equals(DataUtils.typedSize(other.memorable))) {
			return false;
		}

		for (Map.Entry<SymbolicSuffix, SDT> e : sdts.entrySet()) {
			SDT sdt1 = e.getValue();
			SDT sdt2 = other.sdts.get(e.getKey());

			if (ioMode) {
				if (sdt1 == null && sdt2 == null) {
					continue;
					}

				if (sdt1 == null || sdt2 == null) {
					return false;
				}
			}

			if (!equalTypeSizes(sdt1.getDataValues(), sdt2.getDataValues())) {
				return false;
			}
		}
		return true;
	}

	private static boolean equalTypeSizes(Set<DataValue> s1, Set<DataValue> s2) {
		return DataUtils.typedSize(s1).equals(DataUtils.typedSize(s2));
	}

	public static CTPath computePath(TreeOracle oracle, Prefix prefix, List<SymbolicSuffix> suffixes, boolean ioMode) {
		CTPath r = new CTPath(ioMode);
		SDT sdt = prefix.getSDT(RaStar.EMPTY_SUFFIX);
		sdt = sdt == null ? oracle.treeQuery(prefix, RaStar.EMPTY_SUFFIX) : sdt;
		r.putSDT(RaStar.EMPTY_SUFFIX, sdt);
		for (SymbolicSuffix s : suffixes) {
			sdt = prefix.getSDT(s);
			if (sdt == null) {
				sdt = oracle.treeQuery(prefix, s);
			}

			if (r.getSDT(s) == null) {
				r.putSDT(s, sdt);
			}
		}

		return r;
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SymbolicSuffix, SDT> e : this.sdts.entrySet()) {
            sb.append("[").append(e.getKey()).append("->").append(e.getValue().toString().replaceAll("\\s+", " ")).append("] ");
        }
        return sb.toString();
    }
}
