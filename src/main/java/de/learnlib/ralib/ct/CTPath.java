package de.learnlib.ralib.ct;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SDTGuardElement;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.DataUtils;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.ElementRestriction;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.TrueRestriction;
import de.learnlib.ralib.theory.equality.EqualityRestriction;
import de.learnlib.ralib.words.PSymbolInstance;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

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
	private final List<SymbolicSuffix> suffixes;

	private boolean ioMode;

	public CTPath(boolean ioMode) {
		this.sdts = new LinkedHashMap<>();
		this.memorable = new MemorableSet();
		this.ioMode = ioMode;
		this.suffixes = new ArrayList<>();
	}

	public void putSDT(SymbolicSuffix suffix, SDT sdt) {
		assert !sdts.containsKey(suffix);
		sdts.put(suffix, sdt);
		memorable.addAll(sdt.getDataValues());
		suffixes.add(suffix);
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
	
	public SymbolicSuffix getPrior(SymbolicSuffix suffix) {
		int index = suffixes.indexOf(suffix);
		if (index < 0) {
			throw new IllegalArgumentException("No occurrence of " + suffix);
		}
		if (index == 0) {
			return RaStar.EMPTY_SUFFIX;
		}
		return suffixes.get(index - 1);
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
//		Bijection<DataValue> renaming = new Bijection<>();
		SymbolicSuffix prevSuffix = RaStar.EMPTY_SUFFIX;
		for (SymbolicSuffix s : suffixes) {
			if (s.equals(RaStar.EMPTY_SUFFIX)) {
				continue;
			}
			Bijection<DataValue> renaming = prefix.getBijection(prevSuffix);
			SymbolicSuffix sRelabeled = s.relabel(renaming.inverse().toVarMapping());
			PSymbolInstance action = prefix.size() > 0 ? prefix.lastSymbol() : null;
			assert noUnmapped(action, sRelabeled, r.getMemorable()) : "Equality with unmapped data value";
			sdt = prefix.getSDT(s);
			if (sdt == null) {
				sdt = oracle.treeQuery(prefix, sRelabeled);
			}

			if (r.getSDT(s) == null) {
				r.putSDT(s, sdt);
			}
			prevSuffix = s;
		}

		return r;
	}

	private static boolean noUnmapped(PSymbolInstance action, SymbolicSuffix suffix, Set<DataValue> memorable) {
		List<DataValue> actionVals = action == null ? new ArrayList<>() : Arrays.asList(action.getParameterValues());
		for (AbstractSuffixValueRestriction r : suffix.getRestrictions().values()) {
			if (r instanceof ElementRestriction er) {
				for (Expression<BigDecimal> e : er.getElements()) {
					if (e instanceof DataValue d && !memorable.contains(d) && !actionVals.contains(d)) {
						return false;
					}
				}
			}
		}
		return true;
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
