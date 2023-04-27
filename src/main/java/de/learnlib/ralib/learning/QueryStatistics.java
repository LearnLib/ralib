package de.learnlib.ralib.learning;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class QueryStatistics {
	public static final int TESTING = 0;
	public static final int CE_ANALYSIS = 1;
	public static final int CE_PROCESSING = 2;
	public static final int OTHER = 3;
	public static final String[] PHASES = {"Testing", "CE Analysis", "Processing", "Other"};
	private static final int MEASUREMENTS = 4;

	private final QueryCounter queryCounter;
	private final Measurements[] phaseMeasurements = new Measurements[MEASUREMENTS];
	private final Measurements measurements;
	private int phase = OTHER;
	private long countLastUpdate = 0;
	public final Map<SymbolicWord, Integer> treeQueryWords = new LinkedHashMap<SymbolicWord, Integer>();
	public final Collection<Word<PSymbolInstance>> ces = new LinkedHashSet<Word<PSymbolInstance>>();

	public QueryStatistics(Measurements measurements, QueryCounter queryCounter) {
		this.queryCounter = queryCounter;
		this.measurements = measurements;
		for (int i = 0; i < MEASUREMENTS; i++)
			phaseMeasurements[i] = new Measurements();
	}

	public QueryStatistics(Measurements measurements, QueryCounter queryCounter, int phase) {
		this(measurements, queryCounter);
		this.phase = phase;
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
		phaseMeasurements[phase].memQueries = phaseMeasurements[phase].memQueries + queryCounter.getQueryCount() - countLastUpdate;
		countLastUpdate = queryCounter.getQueryCount();
		ces.addAll(measurements.ces);
		treeQueryWords.putAll(measurements.treeQueryWords);
		measurements.reset();
	}

	public Measurements getMeasurements(int phase) {
		return phaseMeasurements[phase];
	}

	public long getMemQueries() {
		long queries = measurements.memQueries;
		for (int i = 0; i < MEASUREMENTS; i++) {
			queries = queries + phaseMeasurements[i].memQueries;
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
	public void analyzingCounterExample() {
		setPhase(CE_ANALYSIS);
	}

	public void processingCounterExample() {
		setPhase(CE_PROCESSING);
	}

	public void hypothesisConstructed() {
		setPhase(TESTING);
	}

	public String toString() {
		String str = "";
		for (int i = 0; i < MEASUREMENTS; i++) {
			str = str + phaseMeasurements[i].toString() + " (" + PHASES[i] + ")\n";
		}
		return str;
	}
}
