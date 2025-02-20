package de.learnlib.ralib.automata;

import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.PSymbolInstance;

public class RARun {

	private final RALocation[] locations;
	private final VarValuation[] valuations;
	private final PSymbolInstance[] transitions;

	public RARun(RALocation[] locations, VarValuation[] valuations, PSymbolInstance[] transitions) {
		this.locations = locations;
		this.valuations = valuations;
		this.transitions = transitions;
	}

	public RALocation getLocation(int i) {
		return locations[i];
	}

	public VarValuation getValuation(int i) {
		return valuations[i];
	}

	public PSymbolInstance getTransition(int i) {
		return transitions[i-1];
	}

	@Override
	public String toString() {
		if (locations.length == 0) {
			return "ε";
		}
		String str = "<" + locations[0] + ", " + valuations[0] + ">";
		for (int i = 0; i < locations.length - 1; i++) {
			str = str +
					" -- " +
					transitions[i] +
					" -- <" +
					locations[i+1] +
					", " +
					valuations[i+1] +
					">";
		}
		return str;
	}
}
