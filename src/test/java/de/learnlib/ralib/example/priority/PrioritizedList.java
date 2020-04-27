package de.learnlib.ralib.example.priority;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrioritizedList extends java.util.ArrayList {
    
	private int capacity;
	private int[] order;
    
	/**
	 * 
	 * @param capacity
	 * @param order - array whose length should be equal to the capacity. It should contain numbers from 0 (incl) to capacity (excl). First item stores the index in list  with higher prio, second item, second highest pro... 
	 */
    public PrioritizedList(int capacity, int [] order) {
    	this.capacity = capacity;
    	this.order = order;
    	assert(capacity == order.length);
    	assert(Arrays.stream(order).boxed().collect(Collectors.toSet())
    			.equals(IntStream.range(0, capacity).boxed().collect(Collectors.toSet())));
    }

    public boolean offer(Object e) {
        return (this.size() < this.capacity) ? super.add(e) : false;
    }
    
    public Object poll() {
    	for (int i=0; i<order.length; i++) {
    		if (order[i] < this.size()) {
    			return super.remove(order[i]);
    		}
    	}
    	return null;
    }
}
