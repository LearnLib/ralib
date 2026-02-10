package de.learnlib.ralib.theory;

public interface RestrictionContainer {
	
	public boolean contains(AbstractSuffixValueRestriction restr);
	
	public boolean containsUnmapped();
	
	public AbstractSuffixValueRestriction replace(AbstractSuffixValueRestriction replace, AbstractSuffixValueRestriction with);
	
	public AbstractSuffixValueRestriction cast();
}
