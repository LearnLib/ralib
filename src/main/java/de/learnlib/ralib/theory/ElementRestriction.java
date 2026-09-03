package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import gov.nasa.jpf.constraints.api.Expression;

public interface ElementRestriction {

	/**
	 * @param element
	 * @return {@code true} if and only if {@code this} applies a constraint on {@code element}
	 */
	public boolean containsElement(Expression<BigDecimal> element);

	/**
	 * @return the set of elements on which {@code this} applies a constraint
	 */
	public Set<Expression<BigDecimal>> getElements();

	/**
	 * Replace any constraint on {@code replace} by an equivalent constraint on {@code by}
	 *
	 * @param replace
	 * @param by
	 * @return
	 */
	public AbstractSuffixValueRestriction replaceElement(Expression<BigDecimal> replace, Expression<BigDecimal> by);

	/**
	 * Return a list of restrictions contained in {@code this} which impose constraint on {@code element}.
	 * If {@code this} does not contain multiple restrictions, return a singleton list containing
	 * {@code this} if {@code this} imposes a constraint on {@code element}.
	 * Otherwise return an empty list.
	 *
	 * @param element
	 * @return
	 */
	public List<ElementRestriction> getRestrictions(Expression<BigDecimal> element);

	/**
	 * Case {@code this} to an {@code AbstractSuffixValueRestriction}
	 *
	 * @return
	 */
	public AbstractSuffixValueRestriction cast();
}
