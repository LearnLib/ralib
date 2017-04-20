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
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class IORandomWalkTraceGenerator implements TraceGenerator{
	
	private RegisterAutomaton hyp;
	private double stopProbability;
	private boolean uniform;
	private double drawHistory;
	private double drawRegister;
	private double drawRelated;
	private Constants constants;
	private int maxDepth;
	private Map<DataType, Theory> teachers;
	private ParameterizedSymbol[] inputs;
	protected Random rand;
	private ParameterizedSymbol error;
	private SimulatorSUL target;

	public IORandomWalkTraceGenerator(Random rand,boolean uniform, double stopProbability,
			double regProb, double hisProb, double relatedProb,  int maxDepth, Constants constants,
			Map<DataType, Theory> teachers, ParameterizedSymbol... inputs) {

		this.rand = rand;
		this.inputs = inputs;
		this.uniform = uniform;
		this.stopProbability = stopProbability;
		this.drawHistory = hisProb;
		this.drawRegister = regProb;
		this.drawRelated = relatedProb;
		this.constants = constants;
		this.maxDepth = maxDepth;
		this.teachers = teachers;
		this.error = null;
	}

	public Word<PSymbolInstance> generateTrace(RegisterAutomaton hyp) {
		Word<PSymbolInstance> run = this.randomTraceFromPrefix(Word.epsilon(), hyp);		
		return run;
	}
	

	public void setError(ParameterizedSymbol error) {
		this.error = error;
	}
	
	/**
	 * Generates a trace from the given location prefix. Does not include the prefix in the generated run. 
	 */
	protected Word<PSymbolInstance> randomTraceFromPrefix( Word<PSymbolInstance> inLocPrefix, RegisterAutomaton hyp) {
		int depth = 0;
		this.hyp = hyp;
		this.target = new SimulatorSUL(hyp, teachers, constants);
		target.pre();
		
		for (int i=0; i<inLocPrefix.length(); i=i+2) {
			PSymbolInstance out = target.step(inLocPrefix.getSymbol(i));
			assert out.equals(inLocPrefix.getSymbol(i+1));
		}
		Word<PSymbolInstance> trace = inLocPrefix;
		PSymbolInstance out;
		do {
			PSymbolInstance next = nextInput(trace, rand);
			assert next!=null;
			depth++;
			out = null;
			trace = trace.append(next);
			out = target.step(next);
			trace = trace.append(out);

		} while (rand.nextDouble() > stopProbability && depth < maxDepth && !out.getBaseSymbol().equals(error));
		target.post();
		return trace.suffix(trace.size()-inLocPrefix.size());
	}
	
	
	protected PSymbolInstance nextInput(Word<PSymbolInstance> run, Random rand) {
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
