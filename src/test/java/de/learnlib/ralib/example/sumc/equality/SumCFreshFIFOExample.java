package de.learnlib.ralib.example.sumc.equality;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SumCFreshFIFOExample {
	private List<Integer> fifo;
	private int capacity;
	private int sumConst;
	private Random rand;
	
	public SumCFreshFIFOExample(int capacity, int sumConst) {
		this.fifo = new ArrayList<>(capacity);
		this.sumConst = sumConst;
		this.capacity = capacity;
		this.rand = new Random(0);
	}
	
	
	public Integer offer() {
		if (fifo.size() < capacity) {
			int value;
			if (fifo.isEmpty()) {
				value = rand.nextInt(1000) + 1; 
			} else {
				value = fifo.get(fifo.size()-1) + sumConst;
			}
			fifo.add(value);
			return value;
		} else {
			return null;
		}
	}
	
	public boolean poll(Integer value) {
		if (fifo.isEmpty()) {
			return false;
		} else {
			if (fifo.get(fifo.size()-1).equals(value)) {
				fifo.remove(fifo.size()-1);
				return true;
			} else {
				return false;
			}
		}
	}
}
