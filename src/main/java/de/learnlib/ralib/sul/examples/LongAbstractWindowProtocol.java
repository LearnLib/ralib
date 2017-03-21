package de.learnlib.ralib.sul.examples;

public abstract class LongAbstractWindowProtocol {
	private static Long DEFAULT_WIN = 1000L;
	Long win;
	private Long gen = 0L;

	public LongAbstractWindowProtocol(Long windowSize) {
		this.win = windowSize;
	}
	
	public LongAbstractWindowProtocol() {
		this(DEFAULT_WIN);
	}
	

    protected Long newFresh() {
    	return gen = gen + 10000000;
    }
    

    /**
     * Both numbers are not null AND nextSeq is succ of currentSeq.
     */
	public boolean succ(Long currentSeq, Long nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq == currentSeq + 1;
	}

	/**
     * Both numbers are not null AND nextSeq is equal to currentSeq.
     */
	public boolean equ(Long currentSeq, Long nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq.equals(currentSeq);
	}


	/**
     * Both numbers are not null AND nextSeq is in the range (1+currentSeq, win+currentSeq)
     */
	public boolean inWin(Long currentSeq, Long nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
	}
}
