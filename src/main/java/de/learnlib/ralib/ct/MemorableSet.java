package de.learnlib.ralib.ct;

import java.util.LinkedHashSet;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;

public class MemorableSet extends LinkedHashSet<DataValue> {
	public MemorableSet relabel(Bijection<DataValue> renaming) {
		MemorableSet renamed = new MemorableSet();
		for (DataValue dv : this) {
			DataValue ndv = renaming.get(dv);
			renamed.add((ndv == null) ? dv : ndv);
		}
		return renamed;
	}
}
