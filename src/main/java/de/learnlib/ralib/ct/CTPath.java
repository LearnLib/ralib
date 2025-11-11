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

/**
 * This data structure stores the SDTs from tree queries for a prefix along a path
 * in a {@link ClassificationTree}. It contains much of the same functionality as
 * {@link Row}, but adapted for use with classification trees.
 *
 * @author fredrik
 * @author falk
 * @see Row
 */
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

	/**
	 * Checks whether two paths are equivalent under {@code renaming}.
	 *
	 * @param other
	 * @param renaming
	 * @param solver
	 * @return {@code true} if the SDTs of {@code this} are equivalent to those of {@code other} under {@code renaming}
	 */
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

	/**
	 * Checks whether the SDTs of {@code this} and {@code other} have the same number of
	 * data value types.
	 *
	 * @param other
	 * @return {@code true} if the number of types match for the SDTs of {@code this} and {@code other}
	 */
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

	/**
	 * Computes the path for {@code prefix} by computing SDTs for each suffix in {@code suffixes}.
	 * The SDTs already contained within the path {@code prefix} will be copied to the new path.
	 * Remaining SDTs are computed via tree queries.
	 *
	 * @param oracle the oracle to use for tree queries
	 * @param prefix the prefix for which the new path is to be computed
	 * @param suffixes the suffixes along the path
	 * @param ioMode {@code true} if the language being learned is an IO language
	 * @return a {@code CTPath} containing SDTs for each suffix in {@code suffixes}
	 */
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
