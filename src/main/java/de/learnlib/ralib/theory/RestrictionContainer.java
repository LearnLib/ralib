package de.learnlib.ralib.theory;

public interface RestrictionContainer {

	/**
	 * @param restr
	 * @return {@code true} if and only if {@code this} contains {@code restr}
	 */
	public boolean contains(AbstractSuffixValueRestriction restr);

	/**
	 * @return {@code true} if and only if {@code this} contains an {@code UnmappedEqualityRestriction}
	 */
	public boolean containsUnmapped();

	/**
	 * @param replace
	 * @param by
	 * @return {@code this} with all instances of {@code replace} replaced by {@code by}
	 */
	public AbstractSuffixValueRestriction replace(AbstractSuffixValueRestriction replace, AbstractSuffixValueRestriction by);

	/**
	 * Case {@code this} to an {@code AbstractSuffixValueRestriction}
	 *
	 * @return
	 */
	public AbstractSuffixValueRestriction cast();
}
