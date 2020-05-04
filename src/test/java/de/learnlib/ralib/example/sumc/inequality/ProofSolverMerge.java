package de.learnlib.ralib.example.sumc.inequality;

/**
 * 
 * An example which shows why syntactical comparison of SDTs in "canonical forms" fails to properly handle cases involving Sum Constants.
 * Equivalent SDTs are seen as not-equivalent due to the labeling scheme.
 *
 */
public class ProofSolverMerge {
	private Integer reg = null;

	public boolean in(Integer reg) {
		if (this.reg == null)
		this.reg = reg;
		return true;
	}
	
	public boolean test(Integer p1, Integer p2, Integer p3) {
		boolean res = this.reg != null && p1.equals(this.reg+1) && p3.equals(p2 + 1);
		return res;
	}
}
