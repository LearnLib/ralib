package de.learnlib.ralib.example.succ;

public class SimpleTCPExample extends AbstractWindowProtocol {

	private Double seqNum = null;

	public boolean IConnect(Double initSeq) {
		boolean ret = false;
		if (seqNum == null) {
			this.seqNum = initSeq;
			ret = true;
		}
		return ret;
	}

	public boolean IMSG(Double nextSeq) {
		boolean ret = false;
		if (seqNum != null) {
			if (super.succ(seqNum, nextSeq)) {
				seqNum = nextSeq;
				ret = true;
			} else {
				if (super.inWin(seqNum, nextSeq)) {
					ret = false;
				} else {
					ret = false;
					seqNum = null;
				}
			}
		}

		return ret;
	}
}
