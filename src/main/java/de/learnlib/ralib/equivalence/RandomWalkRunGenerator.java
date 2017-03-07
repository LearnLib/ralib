package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;

public class RandomWalkRunGenerator {
	private Random random;
	private int maxSize;
	private double resetProb;

	public RandomWalkRunGenerator(int maxSize, double resetProb, Random random) {
		this.random = random;
		this.maxSize = maxSize;
		this.resetProb = resetProb;
	}
	
	public List<Transition> generateRun(RegisterAutomaton ra) {
//		RALocation crtLoc = ra.getInitialLocation();
//		List<Transition> run = new ArrayList<Transition>();
//		
//		while(run.size() < maxSize && random.nextDouble() > resetProb) {
//			Transition[] trans = crtLoc.getOut().toArray(new Transition []{});
//			int nextTransInd = this.random.nextInt(trans.length);
//			Transition transTaken = trans[nextTransInd];
//			run.add(transTaken);
//			crtLoc = transTaken.getDestination();
//		}
		
		return null;
	}
}
