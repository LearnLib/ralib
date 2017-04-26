package de.learnlib.ralib.tools;

import de.learnlib.ralib.sul.DataWordSUL;

public interface ParallelizableSULFactory extends SULFactory {
	public default boolean isParallelizable() {
		return true;
	}

	public default DataWordSUL [] newIndependentSULs(int numInstances) {
		DataWordSUL [] suls = new DataWordSUL[numInstances];
		for (int i=0; i<numInstances; i++) {
			suls[i] = this.newSUL();
		}
		return suls;
	}
}
