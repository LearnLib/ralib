package de.learnlib.ralib.sul.examples;

public class DualSeqNumExample extends AbstractWindowProtocol{
	private Double seq1;
	private Double seq2;
	
	public boolean IINIT(Double seq1, Double seq2) {
		if (this.seq1 == null) {
			this.seq1 = seq1;
			this.seq2 = seq2;
			return true;
		}
		
		return false;
	}
	
	public boolean INEXT(Double next) {
		if (this.seq1 != null) {
			if (succ(this.seq1, next)) {
				this.seq1 = next;
				return true;
			}
			
			if (succ(this.seq2, next)) {
				this.seq2 = next;
				return true;
			}
		}
		this.seq1 = this.seq2 = null;
		return false;
	}
	
	
}
