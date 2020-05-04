package de.learnlib.ralib.example.sumcineq;

public abstract class AbstractWindowProtocol {
	private static Double DEFAULT_WIN = 1000.0;
	Double win;
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
    

    /**
     * Both numbers are not null AND nextSeq is succ of currentSeq.
     */
	public boolean succ(Double currentSeq, Double nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq == currentSeq + 1;
	}

	/**
     * Both numbers are not null AND nextSeq is equal to currentSeq.
     */
	public boolean equ(Double currentSeq, Double nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq.equals(currentSeq);
	}


	/**
     * Both numbers are not null AND nextSeq is in the range (1+currentSeq, win+currentSeq)
     */
	public boolean inWin(Double currentSeq, Double nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
	}
}
