package de.learnlib.ralib.data.util;

import java.util.Iterator;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;

public class RemappingIterator implements Iterable<Bijection>, Iterator<Bijection> {

	private final PermutationIterator permit;
	private final Register[] replace;
	private final Register[] by;

	private int[] next;

	public RemappingIterator(Set<Register> replace, Set<Register> by) {
		assert by.size() >= replace.size();
		this.replace = replace.toArray(new Register[replace.size()]);
		this.by = by.toArray(new Register[by.size()]);
		permit = new PermutationIterator(by.size());

		next = replace.size() > 0 ? advance(this.replace, this.by, permit) : new int[0];
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
//	public VarMapping<Register, Register> next() {
	public Bijection next() {
		assert next != null : "No more permutations";
		VarMapping<Register, Register> vars = new VarMapping<>();
		for (int i = 0; i < replace.length; i++) {
			int index = next[i];
			vars.put(replace[i], by[index]);
		}

		next = advance(replace, by, permit);

		return new Bijection(vars);
	}

	@Override
	public Iterator<Bijection> iterator() {
		return this;
	}

	private static boolean isValidPermutation(Register[] replace, Register[] by, int[] permutation) {
		for (int i = 0; i < permutation.length; i++) {
			int index = permutation[i];
			if (!replace[i].getDataType().equals(by[index].getDataType())) {
				return false;
			}
		}
		return true;
	}

	private static int[] advance(Register[] replace, Register[] by, PermutationIterator permit) {
		int[] next = null;
		while (permit.hasNext()) {
			next = permit.next();
			if (isValidPermutation(replace, by, next)) {
				break;
			}
		}
		return next;
	}
}
