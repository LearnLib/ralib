package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import gov.nasa.jpf.constraints.api.Expression;

public interface ElementRestriction {
	
	public boolean containsElement(Expression<BigDecimal> element);
	
	public Set<Expression<BigDecimal>> getElements();
	
	public AbstractSuffixValueRestriction replaceElement(Expression<BigDecimal> replace, Expression<BigDecimal> by);
	
	public List<ElementRestriction> getRestrictions(Expression<BigDecimal> element);
	
	public AbstractSuffixValueRestriction cast();
}
