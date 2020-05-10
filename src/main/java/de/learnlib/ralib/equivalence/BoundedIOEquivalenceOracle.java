package de.learnlib.ralib.equivalence;

import java.util.Collection;
import java.util.logging.Level;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public abstract class BoundedIOEquivalenceOracle implements IOEquivalenceOracle {
	private static final LearnLogger log = LearnLogger.getLogger(BoundedIOEquivalenceOracle.class);
	private long maxRuns;
	private boolean resetRuns;
	private int runs;
	private TraceGenerator hypTraceGenerator;
	private IOOracle target;
	private TraceCanonizer traceCanonizer;
	private TestPurpose testPurpose = (w) -> true;

	public BoundedIOEquivalenceOracle(IOOracle target,  TraceCanonizer traceCanonizer, long maxRuns, boolean resetRuns) {
	
		this.maxRuns = maxRuns;
		this.resetRuns = resetRuns;
		this.target = target;
		this.runs = 0;
		this.traceCanonizer = traceCanonizer;
	}
	
	/**
	 * Only traces which satisfy the test purpose will be run on the system.
	 */
	public void setTestPurpose(TestPurpose testPurpose) {
		this.testPurpose = testPurpose;
	}
	
	/**
	 * Sets the hypothesis trace generator. The trace generator should be set during initialization/before 
	 * the findCE functionality is used.
	 */
	protected void setTraceGenerator(TraceGenerator hypTraceGenerator) {
		this.hypTraceGenerator = hypTraceGenerator;
	}

	@Override
	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hyp,
			Collection<? extends PSymbolInstance> clctn) {

		// reset the counter for number of runs after every equiv query?
		if (resetRuns) {
			runs = 0;
		}

		// find counterexample ...
		while (runs < maxRuns) {
			Word<PSymbolInstance> hypTrace = hypTraceGenerator.generateTrace(hyp);
			if (!testPurpose.isSatisfied(hypTrace)) {
				continue;
			} 
			hypTrace = traceCanonizer.canonize(hypTrace);
			Word<PSymbolInstance> sulTrace = target.trace(hypTrace);
			if (!hypTrace.equals(sulTrace)) {
				log.log(Level.INFO, "SUL Trace {0}", sulTrace.toString());
				log.log(Level.INFO, "HYP Trace {0}", hypTrace.toString());
				System.out.println("HYP Trace " + hypTrace.toString());
				Word<PSymbolInstance> newSulTrace = target.trace(sulTrace);
				assert newSulTrace.equals(sulTrace);
				int j;
				for(j=0; hypTrace.getSymbol(j).equals(sulTrace.getSymbol(j)); j++);
				log.log(Level.FINE, "HYP Run: {0}", hypRun(hyp, sulTrace.prefix(j+1)));
				return new DefaultQuery<>(sulTrace.prefix(j+1), Boolean.TRUE);
			}
			runs ++;
			
		}
		return null;
	}
	
	private String hypRun(RegisterAutomaton hyp, Word<PSymbolInstance> trace) {
		StringBuilder builder = new StringBuilder();
		Word<PSymbolInstance> crtTrace = Word.epsilon();
		for (PSymbolInstance sym : trace) {
			builder.append(hyp.getLocation(crtTrace))
			.append(" ").append(sym).append(" ");
			crtTrace = crtTrace.append(sym);
		}
		return builder.toString();
	}
	

}
