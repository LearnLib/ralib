package de.learnlib.ralib.example.repeater;

public class Repeater {
	public static final int MAX_REPEATS = 2;
	public static final int CAPACITY = -1;

	private final int max_repeats;
	private int repeats = 0;
	private final int capacity;
	private int inputs = 0;
	private Integer latest = null;

	public Repeater() {
		max_repeats = MAX_REPEATS;
		capacity = CAPACITY;
	}

	public Repeater(int max) {
		max_repeats = max;
		capacity = CAPACITY;
	}
	
	public Repeater(int max, int capacity) {
		max_repeats = max;
		this.capacity = capacity;
	}

	public Integer repeat(Integer p) {
		inputs++;
		if (capacity >= 0 && inputs > capacity) {
			return null;
		}
		if (p.equals(latest)) {
			repeats++;
			return max_repeats > 0 && repeats > max_repeats
					? null
					: Integer.valueOf(p);
		}
		latest = Integer.valueOf(p);
		repeats = 1;
		return Integer.valueOf(p);
	}
}
