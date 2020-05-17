package de.learnlib.ralib.example.priority;

import de.learnlib.ralib.utils.ConcreteSULWrapper;

public class PriorityQueueConcreteSUL extends ConcreteSULWrapper<PQWrapper>{
	private static Integer CAPACITY = 3;
	
	public PriorityQueueConcreteSUL() {
		super(PQWrapper.class, 
			() -> {
				PQWrapper sut =  new PQWrapper(CAPACITY);
				return sut;
			});
	}
}
