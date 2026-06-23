package de.learnlib.ralib.example.palindrome;

import java.util.ArrayList;
import java.util.List;

public class Palindrome {
	public static final int DEFAULT_MAX = 3;

	private final int max;

	private List<Integer> vals;

	public Palindrome(int max) {
		this.max = max;
		vals = new ArrayList<>();
	}

	public Palindrome() {
		this(DEFAULT_MAX);
	}

	public void reset() {
		vals.clear();
	}

	public boolean in(int d) {
		vals.add(d);
		if (vals.size() > max) {
			return false;
		}
		return isPalindrome();
	}

	private boolean isPalindrome() {
		int left = 0;
		int right = vals.size() - 1;
		while (left < right) {
			if (!vals.get(left).equals(vals.get(right))) {
				return false;
			}
			left++;
			right--;
		}
		return true;
	}
}
