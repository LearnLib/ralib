package de.learnlib.ralib.example.sumcineq;

public abstract class AbstractIntWindowProtocol {
	private static Integer DEFAULT_WIN = 1000;
	Integer win;
	private Integer gen = 0;

	public AbstractIntWindowProtocol(Integer windowSize) {
		this.win = windowSize;
	}
	
	public AbstractIntWindowProtocol() {
		this(DEFAULT_WIN);
	}
	

    protected Integer newFresh() {
    	return gen = gen + this.win * 1000;
    }
    

    /**
     * Both numbers are not null AND nextSeq is succ of currentSeq.
     */
	public boolean succ(Integer currentSeq, Integer nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq == currentSeq + 1;
	}

	/**
     * Both numbers are not null AND nextSeq is equal to currentSeq.
     */
	public boolean equ(Integer currentSeq, Integer nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq.equals(currentSeq);
	}


	/**
     * Both numbers are not null AND nextSeq is in the range (1+currentSeq, win+currentSeq)
     */
	public boolean inWin(Integer currentSeq, Integer nextSeq) {
		if (currentSeq == null || nextSeq == null)
			return false;
		return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
	}
}
