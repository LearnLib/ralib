package de.learnlib.ralib.example.container;

public class ContainerSUL {

	public static final int ERROR = 0;

	private Integer val = null;

	public void put(Integer val) {
		this.val = val;
	}

	public Integer get() {
		return val == null ? ERROR : val;
	}
}
