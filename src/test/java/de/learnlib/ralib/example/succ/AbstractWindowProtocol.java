package de.learnlib.ralib.example.succ;

public abstract class AbstractWindowProtocol {
	private static Double DEFAULT_WIN = 1000.0;
	private Double win;
	private Double gen = 0.0;

	public AbstractWindowProtocol(Double windowSize) {
		this.win = windowSize;
	}
	
	public AbstractWindowProtocol() {
		this(DEFAULT_WIN);
	}
	

    protected Double newFresh() {
    	return gen = gen + 10000000;
    }
    

	public boolean succ(Double currentSeq, Double nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq == currentSeq + 1;
	}

	public boolean equ(Double currentSeq, Double nextSeq) {
		return nextSeq.equals(currentSeq);
	}

	public boolean inWin(Double currentSeq, Double nextSeq) {
		return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
	}
}
