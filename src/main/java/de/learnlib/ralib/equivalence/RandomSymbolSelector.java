package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;


/**
 * First selects a symbol randomly, then concretizes it using smart methods.
 */
public class RandomSymbolSelector extends InputSelector{ 

	private double drawRegister;
	private double drawHistory;
	private double drawRelated;
	private boolean uniform;

	public RandomSymbolSelector(Random rand, Map<DataType, Theory> teachers, Constants constants,
			boolean uniform, double regProb, double hisProb, double relatedProb,
			ParameterizedSymbol[] inputs) {
		super(rand, teachers, constants, inputs);
		this.drawRegister = regProb;
		this.drawHistory = hisProb;
		this.drawRelated = relatedProb;
		this.uniform = uniform;
	}

	
	protected PSymbolInstance nextInput(Word<PSymbolInstance> run, RegisterAutomaton hyp) {
		this.hyp = hyp;
		ParameterizedSymbol ps = nextSymbol(run);
		RALocation location = this.hyp.getLocation(run);
		if (location == null)
			return null;
		List<Transition> transitions = this.hyp.getInputTransitions().stream()
				.filter(tr -> tr.getSource().equals(location)).collect(Collectors.toList());
		Transition trans = transitions.get(rand.nextInt(transitions.size()));
		trans.getGuard().getCondition();
		PSymbolInstance psi = nextDataValues(run, ps, rand);
		return psi;
	}
	
	private PSymbolInstance nextDataValues(Word<PSymbolInstance> run, ParameterizedSymbol ps, Random rand) {

		DataValue[] vals = new DataValue[ps.getArity()];

		int i = 0;
		for (DataType t : ps.getPtypes()) {
			Theory teacher = teachers.get(t);
			// TODO: generics hack?
			// TODO: add constants?
			Set<DataValue<Object>> oldSet = DataWords.valSet(run, t);
			// an adjustment for TCP
			oldSet.removeAll(this.constants.values());
			// oldSet.addAll(this.constants.getValues(t));
			// for (int j = 0; j < i; j++) {
			// if (vals[j].getType().equals(t)) {
			// oldSet.add(vals[j]);
			// }
			// }
			List<DataValue<Object>> old = new ArrayList<>(oldSet);
			List<DataValue<Object>> regs = getRegisterValuesForType(run, t);
			Double draw = rand.nextDouble();
			if (draw <= drawRegister && !regs.isEmpty()) {
				vals[i] = pick(regs);
			}

			List<DataValue<Object>> history = new ArrayList<>(oldSet);
			history.removeAll(regs);
			if (draw > drawRegister && draw <= drawHistory + drawRegister && !history.isEmpty()) {
				vals[i] = pick(history);
			}

			List<DataValue<Object>> related = new ArrayList<>(oldSet);
			related = new ArrayList<>(teacher.getAllNextValues(related));
			if (draw > drawRegister + drawHistory && draw <= drawRegister + drawHistory + drawRelated
					&& !related.isEmpty())
				vals[i] = pick(related);

			if (vals[i] == null)
				vals[i] = teacher.getFreshValue(old);

			i++;
		}
		return new PSymbolInstance(ps, vals);
	}

	private <T> DataValue<T> pick(List<DataValue<T>> list) {
		return list.get(rand.nextInt(list.size()));
	}

	private List<DataValue<Object>> getRegisterValuesForType(Word<PSymbolInstance> run, DataType t) {
		return hyp.getRegisterValuation(run).values().stream().filter(reg -> reg.getType().equals(t))
				.map(dv -> (DataValue<Object>) dv).collect(Collectors.toList());
	}

	private ParameterizedSymbol nextSymbol(Word<PSymbolInstance> run) {
		ParameterizedSymbol ps = null;
		Map<DataType, Integer> tCount = new LinkedHashMap<>();
		if (uniform) {
			ps = inputs[rand.nextInt(inputs.length)];
		} else {
			int MAX_WEIGHT = 0;
			int[] weights = new int[inputs.length];
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 1;
				for (DataType t : inputs[i].getPtypes()) {
					Integer old = tCount.get(t);
					if (old == null) {
						// TODO: what about constants?
						old = 0;
					}
					weights[i] *= (old + 1);
					tCount.put(t, ++old);
				}
				MAX_WEIGHT += weights[i];
			}

			int idx = rand.nextInt(MAX_WEIGHT) + 1;
			int sum = 0;
			for (int i = 0; i < inputs.length; i++) {
				sum += weights[i];
				if (idx <= sum) {
					ps = inputs[i];
					break;
				}
			}
		}
		return ps;
	}
}