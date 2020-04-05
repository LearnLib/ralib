package de.learnlib.ralib.theory;

/**
 * Every theory should provide a logic by which guards can be conjoined or disjoined. This logic
 * should be used consistently whenever this operation is performed, like for example, when 
 * updating the branching for a component or when merging. 
 */
public interface SDTGuardLogic {

	
	/**
	 * Makes a conjunction between two guards. Whenever possible, simplifies the result guard by
	 * flattening ANDGuards, merging two finer guards into a coarser guard...
	 */
	public SDTGuard conjunction(SDTGuard guard1, SDTGuard guard2);

	/**
	 * Makes a disjunction between two guards. Whenever possible, simplifies the result guard by
	 * flattening ORGuards, merging two finer guards into a coarser guard...
	 */
	public SDTGuard disjunction(SDTGuard guard1, SDTGuard guard2);
}
