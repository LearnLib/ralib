package de.learnlib.ralib.example.sumc.equality;

import java.util.ArrayList;
import java.util.List;

public class SumCFIFOExample {
	private List<Integer> fifo;
	private int capacity;
	private int sumConst;
	
	public SumCFIFOExample(int capacity, int sumConst) {
		this.fifo = new ArrayList<>(capacity);
		this.sumConst = sumConst;
		this.capacity = capacity;
	}
	
	
	public boolean offer(Integer value) {
		if (fifo.size() < capacity) {
			if (fifo.isEmpty()) {
				fifo.add(value);
			} else {
				fifo.add(fifo.get(fifo.size()-1) + sumConst);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public Integer poll() {
		if (fifo.isEmpty()) {
			return null;
		} else {
			return fifo.remove(fifo.size()-1); 
		}
	}
}
