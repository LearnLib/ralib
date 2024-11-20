package de.learnlib.ralib.oracles.mto;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.learnlib.ralib.theory.SDTGuard;

/**
 * Wrapper around an SDT, which labels each node in the SDT with an integer identifier
 * The purpose of LabeledSDT is to facilitate pruning of branches in an SDT
 *
 * @author fredrik
 *
 */
public class LabeledSDT {

	private final int label;

	private final SDT sdt;

	private final Map<SDTGuard, LabeledSDT> children = new LinkedHashMap<>();

	public LabeledSDT(int label, SDT sdt) {
		this.label = label;
		this.sdt = sdt;

		int currentLabel = label;
		if (!(sdt instanceof SDTLeaf)) {
			for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
				LabeledSDT child = new LabeledSDT(currentLabel+1, e.getValue());
				currentLabel = child.getMaxLabel();
				children.put(e.getKey(), child);
			}
		}
	}

	private LabeledSDT(LabeledSDT other, int prunedLabel) {
		label = other.label;
		sdt = other.sdt;

		int currentLabel = label+1;
		if (!(sdt instanceof SDTLeaf)) {
			for (Map.Entry<SDTGuard, LabeledSDT> e : other.children.entrySet()) {
				LabeledSDT child = new LabeledSDT(e.getValue(), prunedLabel);
				if (currentLabel != prunedLabel) {
					children.put(e.getKey(), child);
				}
				currentLabel = e.getValue().getMaxLabel() + 1;
			}
		}
	}

	public int getMaxLabel() {
		int max = label;
		for (LabeledSDT child : children.values()) {
			int m = child.getMaxLabel();
			max = m > max ? m : max;
		}
		return max;
	}

	public int getMinLabel() {
		return getLabel();
	}

	public int getLabel() {
		return label;
	}

	public Set<Integer> getChildIndices() {
		Set<Integer> indices = new LinkedHashSet<>();
		for (LabeledSDT child : children.values()) {
			indices.add(child.label);
		}
		return indices;
	}

	public Map<SDTGuard, LabeledSDT> getChildren() {
		return children;
	}

	public SDT getSDT() {
		return sdt;
	}

	public LabeledSDT getParent(int l) {
		for (LabeledSDT child : children.values()) {
			if (child.getLabel() == l)
				return this;
		}
		for (LabeledSDT child : children.values()) {
			LabeledSDT ret = child.getParent(l);
			if (ret != null)
				return ret;
		}
		return null;
	}

	public SDT toUnlabeled() {
		if (sdt instanceof SDTLeaf)
			return sdt;
		Map<SDTGuard, SDT> sdtChildren = new LinkedHashMap<>();
		for (Map.Entry<SDTGuard, LabeledSDT> e : children.entrySet()) {
			sdtChildren.put(e.getKey(), e.getValue().toUnlabeled());
		}
		return new SDT(sdtChildren);
	}

	public LabeledSDT getNode(int l) {
		if (l == label)
			return this;
		else {
			for (LabeledSDT child : children.values()) {
				LabeledSDT lsdt = child.getNode(l);
				if (lsdt != null)
					return lsdt;
			}
		}
		return null;
	}

	public SDTGuard getGuard(int l) {
		for (Map.Entry<SDTGuard, LabeledSDT> e : children.entrySet()) {
			if (e.getValue().getLabel() == l)
				return e.getKey();
			SDTGuard g = e.getValue().getGuard(l);
			if (g != null)
				return g;
		}
		return null;
	}

	public static LabeledSDT pruneBranch(LabeledSDT lsdt, int label) {
		if (label == lsdt.getLabel())
			return null;
		return new LabeledSDT(lsdt, label);
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String regs = Arrays.toString(sdt.getRegisters().toArray());
        sb.append(regs).append("-+\n");
        toString(sb, spaces(regs.length()));
        return sb.toString();
    }

    void toString(StringBuilder sb, String indentation) {
    	if (sdt instanceof SDTLeaf) {
    		sb.append(indentation).append("[Leaf").append(sdt.isAccepting() ? "+" : "-").append("]").append("\n");
    	} else {
	        LabeledSDT idioticSdt = this;
	        sb.append(indentation).append("[]");
	        final int childCount = idioticSdt.children.size();
	        int count = 1;
	        for (Entry<SDTGuard, LabeledSDT> e : idioticSdt.children.entrySet()) {
	            SDTGuard g = e.getKey();
	            String gString = g.toString();
	            String nextIndent;
	            if (count == childCount) {
	                nextIndent = indentation + "      ";
	            } else {
	                nextIndent = indentation + " |    ";
	            }

	            if (count > 1) {
	                sb.append(indentation).append(" +");
	            }
	            sb.append("- ").append(e.getValue().label).append(":").append(gString).append("\n");
	            e.getValue().toString(sb, nextIndent);

	            count++;
	        }
    	}
    }

    private String spaces(int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

}
