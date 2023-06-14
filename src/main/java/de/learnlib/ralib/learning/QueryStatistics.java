package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class QueryStatistics {
	public static final int TESTING = 0;
	public static final int CE_OPTIMIZE = 1;
	public static final int CE_ANALYSIS = 2;
	public static final int CE_PROCESSING = 3;
	public static final int OTHER = 4;
	public static final String[] PHASES = {"Testing", "CE Optimization", "CE Analysis", "Processing / Refinement", "Other"};
	private static final int MEASUREMENTS = 5;

	private final QueryCounter queryCounter;
	private final DataWordSUL learningSul;
	private final DataWordSUL testingSul;
	private final Measurements[] phaseMeasurements = new Measurements[MEASUREMENTS];
	private final Measurements measurements;
	private int phase = OTHER;
	private long queryCountLastUpdate = 0;
	private long inputCountLastUpdate = 0;
	public final Map<SymbolicWord, Integer> treeQueryWords = new LinkedHashMap<SymbolicWord, Integer>();
	public final Set<Word<PSymbolInstance>> ces = new LinkedHashSet<Word<PSymbolInstance>>();

	public QueryStatistics(Measurements measurements, QueryCounter queryCounter) {
		this.queryCounter = queryCounter;
		learningSul = null;
		testingSul = null;
		this.measurements = measurements;
		initMeasurements();
	}

	public QueryStatistics(Measurements measurements, DataWordSUL sul) {
		this.queryCounter = null;
		learningSul = sul;
		testingSul = null;
		this.measurements = measurements;
		initMeasurements();
	}

	public QueryStatistics(Measurements measurements, DataWordSUL learningSul, DataWordSUL testingSul) {
		queryCounter = null;
		this.learningSul = learningSul;
		this.testingSul = testingSul;
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
		if (phase == TESTING && testingSul != null) {
			updateTests();
		}
		else if (learningSul != null) {
			phaseMeasurements[phase].inputs = phaseMeasurements[phase].inputs + learningSul.getInputs() - inputCountLastUpdate;
			phaseMeasurements[phase].resets = phaseMeasurements[phase].resets + learningSul.getResets() - queryCountLastUpdate;
			inputCountLastUpdate = learningSul.getInputs();
			queryCountLastUpdate = learningSul.getResets();
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
		if (testingSul != null) {
			phaseMeasurements[phase].inputs = testingSul.getInputs();
			phaseMeasurements[phase].resets = testingSul.getResets();
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
		long totTQNoTesting = totTQ - phaseMeasurements[TESTING].treeQueries;
		long totRNoTesting = totR - phaseMeasurements[TESTING].resets;
		long totINoTesting = totI - phaseMeasurements[TESTING].inputs;
		str = str + "Total excl testing: {TQ: " + totTQNoTesting + ", Resets: " + totRNoTesting + ", Inputs: " + totINoTesting + "}\n";
		return str + "Total: " + "{TQ: " + totTQ + ", Resets: " + totR + ", Inputs: " + totI + "}";
	}
}
