package de.learnlib.ralib.sul.examples;

public abstract class IntAbstractWindowProtocol {
	private static Integer DEFAULT_WIN = 1000;
	private Integer win;
	private Integer gen = 0;

	public IntAbstractWindowProtocol(Integer windowSize) {
		this.win = windowSize;
	}
	
	public IntAbstractWindowProtocol() {
		this(DEFAULT_WIN);
	}
	

    protected Integer newFresh() {
    	return gen = gen + 1000000;
    }
    

	public boolean succ(Integer currentSeq, Integer nextSeq) {
		return nextSeq == currentSeq + 1;
	}

	public boolean equ(Integer currentSeq, Integer nextSeq) {
		return nextSeq.equals(currentSeq);
	}

	public boolean inWin(Integer currentSeq, Integer nextSeq) {
		return nextSeq > currentSeq + 1 && nextSeq < currentSeq + win;
	}
}
