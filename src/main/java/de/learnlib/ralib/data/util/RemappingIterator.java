package de.learnlib.ralib.data.util;

import java.util.Collection;
import java.util.Iterator;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.TypedValue;

public class RemappingIterator<T extends TypedValue> implements Iterable<Bijection<T>>, Iterator<Bijection<T>> {

	private final PermutationIterator permit;
	private final TypedValue[] replace;
	private final TypedValue[] by;

	private int[] next;

	public RemappingIterator(Collection<T> replace, Collection<T> by) {
		assert by.size() >= replace.size();
		this.replace = replace.toArray(new TypedValue[] {});
		this.by = by.toArray(new TypedValue[] {});
		permit = new PermutationIterator(by.size());

		next = replace.size() > 0 ? advance(this.replace, this.by, permit) : new int[0];
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public Bijection<T> next() {
		assert next != null : "No more permutations";
		if (replace.length == 0) {
			next = null;
			return new Bijection<>();
		}
		Mapping<T, T> vars = new Mapping<>();
		for (int i = 0; i < replace.length; i++) {
			int index = next[i];
			vars.put( (T) replace[i], (T) by[index]);
		}

		next = advance(replace, by, permit);

		return new Bijection<>(vars);
	}

	@Override
	public Iterator<Bijection<T>> iterator() {
		return this;
	}

	private static boolean isValidPermutation(TypedValue[] replace, TypedValue[] by, int[] permutation) {
		for (int i = 0; i < permutation.length; i++) {
			int index = permutation[i];
			if (!replace[i].getDataType().equals(by[index].getDataType())) {
				return false;
			}
		}
		return true;
	}

	private static int[] advance(TypedValue[] replace, TypedValue[] by, PermutationIterator permit) {
		int[] next = null;
		boolean isValid = false;
		while (permit.hasNext()) {
			next = permit.next();
			if (isValidPermutation(replace, by, next)) {
				isValid = true;
				break;
			}
		}
		return isValid ? next : null;
	}
}
