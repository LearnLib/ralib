package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class QueryStatistics {
	public static final int TESTING = 0;
	public static final int CE_OPTIMIZE = 1;
	public static final int CE_ANALYSIS = 2;
	public static final int CE_PROCESSING = 3;
	public static final int OTHER = 4;
	private static final String[] PHASES = {"Testing", "CE Optimization", "CE Analysis", "Processing / Refinement", "Other"};
	private static final int MEASUREMENTS = 5;

	private final QueryCounter queryCounter;
	private final DataWordSUL learningSUL;
	private final DataWordSUL testingSUL;
	private final Measurements[] phaseMeasurements = new Measurements[MEASUREMENTS];
	private final Measurements measurements;
	private int phase = OTHER;
	private long queryCountLastUpdate = 0;
	private long inputCountLastUpdate = 0;
	public final Map<SymbolicWord, Integer> treeQueryWords = new LinkedHashMap<SymbolicWord, Integer>();
	public final Set<Word<PSymbolInstance>> ces = new LinkedHashSet<Word<PSymbolInstance>>();

	public QueryStatistics(Measurements measurements, QueryCounter queryCounter) {
		this.queryCounter = queryCounter;
		learningSUL = null;
		testingSUL = null;
		this.measurements = measurements;
		initMeasurements();
	}

	public QueryStatistics(Measurements measurements, DataWordSUL sul) {
		this.queryCounter = null;
		learningSUL = sul;
		testingSUL = null;
		this.measurements = measurements;
		initMeasurements();
	}

	public QueryStatistics(Measurements measurements, DataWordSUL learningSUL, DataWordSUL testingSUL) {
		queryCounter = null;
		this.learningSUL = learningSUL;
		this.testingSUL = testingSUL;
		this.measurements = measurements;
		initMeasurements();
	}

	private void initMeasurements() {
		for (int i = 0; i < MEASUREMENTS; i++)
			phaseMeasurements[i] = new Measurements();
	}

	public void reset() {
		for (int i = 0; i < MEASUREMENTS; i++)
			phaseMeasurements[i].reset();
		measurements.reset();
	}

	public void setPhase(int phase) {
		updateMeasurements();
		this.phase = phase;
	}

	public void updateMeasurements() {
		phaseMeasurements[phase].treeQueries = phaseMeasurements[phase].treeQueries + measurements.treeQueries;
		if (phase == TESTING && testingSUL != null) {
			updateTests();
		}
		else if (learningSUL != null) {
			phaseMeasurements[phase].inputs = phaseMeasurements[phase].inputs + learningSUL.getInputs() - inputCountLastUpdate;
			phaseMeasurements[phase].resets = phaseMeasurements[phase].resets + learningSUL.getResets() - queryCountLastUpdate;
			inputCountLastUpdate = learningSUL.getInputs();
			queryCountLastUpdate = learningSUL.getResets();
		}
		else if (queryCounter != null) {
			phaseMeasurements[phase].resets = phaseMeasurements[phase].resets + queryCounter.getQueryCount() - queryCountLastUpdate;
			queryCountLastUpdate = queryCounter.getQueryCount();
		}
		ces.addAll(measurements.ces);
		treeQueryWords.putAll(measurements.treeQueryWords);
		measurements.reset();
	}

	public Measurements getMeasurements(int phase) {
		return phaseMeasurements[phase];
	}

	public long getMemQueries() {
		long queries = measurements.resets;
		for (int i = 0; i < MEASUREMENTS; i++) {
			queries = queries + phaseMeasurements[i].resets;
		}
		return queries;
	}

	public long treeMemQueries() {
		long queries = measurements.treeQueries;
		for (int i = 0; i < MEASUREMENTS; i++) {
			queries = queries + phaseMeasurements[i].treeQueries;
		}
		return queries;
	}

	public Set<Word<PSymbolInstance>> getCEs() {
		return ces;
	}

	public void updateTests() {
		if (testingSUL != null) {
			phaseMeasurements[phase].inputs = testingSUL.getInputs();
			phaseMeasurements[phase].resets = testingSUL.getResets();
		}
	}

	public void analyzingCounterExample() {
		setPhase(CE_ANALYSIS);
	}

	public void processingCounterExample() {
		setPhase(CE_PROCESSING);
	}

	public void hypothesisConstructed() {
		setPhase(TESTING);
	}

	public void analyzeCE(Word<PSymbolInstance> ce) {
		ces.add(ce);
	}

        @Override
	public String toString() {
		String str = "--- Statistics ---\n";
		int sum = 0;
		int max = 0;
		for (Word<PSymbolInstance> ce : ces) {
			int len = ce.length();
			sum = sum + len;
			if (len > max)
				max = len;
		}
		int n = ces.size();
		str = str + "Counterexamples: " + n + "\n"
		          + "CE max length: " + max + "\n"
		          + "CE avg length: " + (n > 0 ? sum / n : 0) + "\n";
		long totTQ = 0;
		long totR = 0;
		long totI = 0;
		for (int i = 0; i < MEASUREMENTS; i++) {
			str = str + PHASES[i] + ": " + phaseMeasurements[i].toString() + "\n";
			totTQ = totTQ + phaseMeasurements[i].treeQueries;
			totR = totR + phaseMeasurements[i].resets;
			totI = totI + phaseMeasurements[i].inputs;
		}
		long totTQNoTesting = totTQ - phaseMeasurements[TESTING].treeQueries - phaseMeasurements[CE_OPTIMIZE].treeQueries;
		long totRNoTesting = totR - phaseMeasurements[TESTING].resets - phaseMeasurements[CE_OPTIMIZE].resets;
		long totINoTesting = totI - phaseMeasurements[TESTING].inputs - phaseMeasurements[CE_OPTIMIZE].inputs;
		str = str + "Total excl testing: {TQ: " + totTQNoTesting + ", Resets: " + totRNoTesting + ", Inputs: " + totINoTesting + "}\n";
		return str + "Total: " + "{TQ: " + totTQ + ", Resets: " + totR + ", Inputs: " + totI + "}";
	}
}
