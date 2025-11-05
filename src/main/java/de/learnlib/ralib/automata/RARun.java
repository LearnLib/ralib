package de.learnlib.ralib.automata;

import java.util.ArrayList;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.words.PSymbolInstance;

public class RARun {

	private final RALocation[] locations;
	private final RegisterValuation[] valuations;
	private final PSymbolInstance[] transitions;

	public RARun(RALocation[] locations, RegisterValuation[] valuations, PSymbolInstance[] transitions) {
		this.locations = locations;
		this.valuations = valuations;
		this.transitions = transitions;
	}

	public RALocation getLocation(int i) {
		return locations[i];
	}

	public RegisterValuation getValuation(int i) {
		return valuations[i];
	}

	public PSymbolInstance getTransition(int i) {
		return transitions[i-1];
	}
	
	public DataValue[] getDataValues(int i) {
		ArrayList<DataValue> vals = new ArrayList<>();
		for (int id = 0; id < i; id++) {
			for (DataValue dv : transitions[id].getParameterValues()) {
				vals.add(dv);
			}
		}
		return vals.toArray(new DataValue[vals.size()]);
	}

	@Override
	public String toString() {
		if (locations.length == 0) {
			return "ε";
		}

		String str = "<" + locations[0] + ", " + valuations[0] + ">";
		for (int i = 1; i < locations.length; i++) {
			str = str +
					" -- " +
					transitions[i-1] +
					" -- <" +
					locations[i] +
					", " +
					valuations[i] +
					">";
		}

		return str;
	}
}
