package de.learnlib.ralib.example.repeater;

public class Repeater {
	public static final int MAX_REPEATS = 2;

	private final int max_repeats;
	private int repeats = 0;
	private Integer latest = null;

	public Repeater() {
		max_repeats = MAX_REPEATS;
	}

	public Repeater(int max) {
		max_repeats = max;
	}

	public Integer repeat(Integer p) {
		if (p.equals(latest)) {
			repeats++;
			return repeats > max_repeats
					? null
					: Integer.valueOf(p);
		}
		latest = Integer.valueOf(p);
		repeats = 1;
		return Integer.valueOf(p);
	}
}
