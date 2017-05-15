package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 * Generates an input by selecting first a transition then concretizing it in a smart way.
 */
public class RandomTransitionSelector extends InputSelector{

	private int regProb;
	private int hisProb;
	private int relatedProb;
	private int freshProb;

	public RandomTransitionSelector(Random rand, Map<DataType, Theory> teachers, Constants constants,
			double regProb, double hisProb, double relatedProb,
			ParameterizedSymbol[] inputs) {
		super(rand, teachers, constants, inputs);
		this.regProb = (int) (regProb * 100);
		this.hisProb = (int) (hisProb * 100);
		this.relatedProb = (int) (relatedProb * 100);
		this.freshProb = 100 - (this.regProb + this.hisProb + this.relatedProb);
	}

	protected PSymbolInstance nextInput(Word<PSymbolInstance> run, RegisterAutomaton hyp) {
		RALocation location = hyp.getLocation(run);
		List<Transition> transitions = hyp.getInputTransitions().stream()
				.filter(tr -> tr.getSource().equals(location)).collect(Collectors.toList());
		Transition trans = transitions.get(rand.nextInt(transitions.size()));
		PSymbolInstance input = concretizeInputTransition(trans, run, hyp);
		return input;
	}

	private PSymbolInstance concretizeInputTransition(Transition trans, Word<PSymbolInstance> run, RegisterAutomaton hyp) {
		Set<DataValue> values = DataWords.valSet(run);
		ParameterizedSymbol input = trans.getLabel();
		DataType[] types = input.getPtypes();
		VarValuation regVal = hyp.getRegisterValuation(run);
		ParValuation parVal = new ParValuation();
		SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
		PSymbolInstance cInput = computeRandomValuationForTrans(input, run, teachers, trans.getGuard(), regVal, parVal, this.constants, 0);
		return cInput;
	}


	private PSymbolInstance computeRandomValuationForTrans(ParameterizedSymbol ps, Word<PSymbolInstance> run, Map<DataType, Theory> teachers, 
			TransitionGuard transGuard, VarValuation regValuation, ParValuation currentValuation, Constants consts, int crtParam) {
		if (crtParam ==ps.getArity()) {
			transGuard.getCondition();
			if (transGuard.isSatisfied(regValuation, currentValuation, consts)) {
				return new PSymbolInstance(ps, currentValuation.values().toArray(new DataValue[]{}));
			} else {
				return null;
			}
			
		} else {
			DataType paramType = ps.getPtypes()[crtParam];
			Theory teacher = teachers.get(paramType);
			List<DataValue<Object>> historyValues = new ArrayList<>(DataWords.valSet(run, paramType));
			historyValues.addAll(currentValuation.values(paramType));
			List hisList = new ArrayList<>(historyValues);
			DataValue fVal = teacher.getFreshValue(hisList);
			List<DataValue<Object>> regValues = new ArrayList<>(regValuation.values(paramType));
			List<DataValue> relatedValues = new ArrayList<>(teacher.getAllNextValues(hisList));
			
			// making lists disjoint
			relatedValues.removeAll(historyValues);
			historyValues.removeAll(regValues);

			Parameter param = new SymbolicDataValue.Parameter(paramType, crtParam + 1);
			boolean triedFresh = false;
			ProbabilityManager pMgr = new ProbabilityManager();
			pMgr.addEvent(Event.DRAW_HISTORY, this.hisProb);
			pMgr.addEvent(Event.DRAW_REG, this.regProb);
			pMgr.addEvent(Event.DRAW_RELATED, this.relatedProb);
			pMgr.addEvent(Event.DRAW_FRESH, this.freshProb);
			// remove DRAW_FRESH event and uncomment to make the fresh probability constant (doesn't increase)
			while (true) {
				DataValue nextVal = null;
//				if ((rand.nextDouble() < 0.1 && !triedFresh) || pMgr.isEmpty()) {
//					triedFresh = true;
//					nextVal = fVal;
//				} else {
					Event draw = pMgr.drawEvent(this.rand);
					switch(draw) {
						case DRAW_REG:
							 if (!regValues.isEmpty()) 
									nextVal = regValues.remove(rand.nextInt(regValues.size()));
							 break;
						case DRAW_HISTORY:
							if (!historyValues.isEmpty()) 
								nextVal = historyValues.remove(rand.nextInt(historyValues.size()));
							break;
						case DRAW_RELATED:
							if (!relatedValues.isEmpty()) 
								nextVal = relatedValues.remove(rand.nextInt(relatedValues.size()));
							break;
						case DRAW_FRESH:
							if (!triedFresh) {
								triedFresh = true;
								nextVal = fVal;
							}	
					}
					if (nextVal == null) {
						pMgr.removeEvent(draw);
					}
//				}
				
				if (pMgr.isEmpty() && triedFresh)
					return null;
				
				if (nextVal != null) {
					ParValuation parValuation = new ParValuation(currentValuation);
					parValuation.put(param, nextVal);
					PSymbolInstance inst = computeRandomValuationForTrans( ps, run,teachers, transGuard, regValuation, parValuation, consts, crtParam+1);
					if (inst != null)
						return inst;
				}
			}
		}
	}

	public enum Event {
		DRAW_REG,
		DRAW_RELATED,
		DRAW_HISTORY,
		DRAW_FRESH;
	}

	class ProbabilityManager {
		private EnumMap<Event, Integer>  eventProbs;
		public ProbabilityManager( EnumMap<Event, Integer> eventProbs) {
			this.eventProbs = eventProbs;
		}
		
		public ProbabilityManager() {
			this(new EnumMap<Event, Integer>(Event.class));
		}
		
		public ProbabilityManager clone() {
			return new ProbabilityManager(new EnumMap<Event, Integer> (eventProbs));
		}
		
		public Event drawEvent(Random random) {
			Integer pick = (int)(100 * random.nextDouble()) + 1;
			Integer sum = 0;
			for (Entry<Event, Integer> eventEntry : eventProbs.entrySet()) {
				Integer eventProb = eventEntry.getValue();
				if (pick >= sum && pick <= sum + eventProb) {
					return eventEntry.getKey();
				}
				sum += eventProb;
			}
			throw new DecoratedRuntimeException("Could not draw").addDecoration("Probability Vector", this.eventProbs);
		}
		
		/**
		 * Add an event with a probability. The probability can be between 1 and 100. In case events 
		 * with non-zero probability are impossible, their associated probability is removed and the
		 * probabilities for all other events are redistributed uniformly.
		 */
		public ProbabilityManager addEvent(Event event, Integer prob) {
			eventProbs.put(event, prob);
			return this;
		}
		
		public ProbabilityManager removeEvent(Event event) {
			Integer eventProb = eventProbs.get(event);
			eventProbs.remove(event);
			Integer remainingEventProb = 100 - eventProb;
			redistributeProb(remainingEventProb);
			return this;
		}
		
		private void redistributeProb(Integer sumOfAllEventProbs) {
			if (eventProbs.isEmpty())
				return; 
			int index = 0;
			Integer sum = 0;
			for (Entry<Event, Integer> eventEntry : eventProbs.entrySet()) {
				// we use floor so if probabilities are shifted due to rounding trimming, they are done so upwards
				Integer newProb = Math.floorDiv(100 * eventEntry.getValue(), sumOfAllEventProbs);
				sum = newProb + sum;
				// we want the sum to always be 100
				if (index == eventProbs.size() -1) {
					Integer leftOver = 100 - sum;
					newProb += leftOver;
				}
				eventEntry.setValue(newProb);
				index ++;
			}
			
		}
		
		public boolean matches(ProbabilityManager probManager) {
			if (this.eventProbs.size() != probManager.eventProbs.size()) {
				return false;
			}
			for (Event event : this.eventProbs.keySet()) {
				if (!this.eventProbs.get(event).equals(probManager.eventProbs.get(event))) {
					return false;
				}
			}
			return true;
		}
		
		public String toString() {
			return "Probability Vector: " + this.eventProbs;
		}
	 	
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((eventProbs == null) ? 0 : eventProbs.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ProbabilityManager other = (ProbabilityManager) obj;
			if (eventProbs == null) {
				if (other.eventProbs != null)
					return false;
			} else if (!eventProbs.equals(other.eventProbs))
				return false;
			return true;
		}
		
		public boolean isEmpty() {
			return eventProbs.isEmpty();
		}
	}

}

