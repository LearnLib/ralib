package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.oracles.TraceCanonizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public abstract class BoundedIOEquivalenceOracle implements IOEquivalenceOracle {
	
	private long maxRuns;
	private boolean resetRuns;
	private int runs;
	private int batchSize = 1;
	private TraceGenerator traceGenerator;
	private IOOracle target;
	private TraceCanonizer traceCanonizer;

	public BoundedIOEquivalenceOracle(IOOracle target,  TraceCanonizer traceCanonizer, long maxRuns, boolean resetRuns) {
	
		this.maxRuns = maxRuns;
		this.resetRuns = resetRuns;
		this.target = target;
		this.runs = 0;
		this.traceCanonizer = traceCanonizer;
	}
	
	public BoundedIOEquivalenceOracle(DataWordSUL target,  TraceCanonizer traceCanonizer, long maxRuns, boolean resetRuns) {
		
		this.maxRuns = maxRuns;
		this.resetRuns = resetRuns;
		this.target = new BasicSULOracle(target, SpecialSymbols.ERROR);
		this.runs = 0;
		this.traceCanonizer = traceCanonizer;
		
	}
	
	/**
	 * Execute in batches instead of one trace at a time. Can harness concurrent implementations of the IO Oracle.
	 */
	public void setBatchExecution(int batchSize) {
		this.batchSize = batchSize;
	}
	
	/**
	 * Sets the hypothesis trace generator. The trace generator should be set during initialization/before 
	 * the findCE functionality is used.
	 */
	protected void setTraceGenerator(TraceGenerator traceGenerator) {
		this.traceGenerator = traceGenerator;
	}

	@Override
	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hyp,
			Collection<? extends PSymbolInstance> clctn) {

		// reset the counter for number of runs after every equiv query?
		if (resetRuns) {
			runs = 0;
		}
		int tens = 1;
		// find counterexample ...
		while (runs < maxRuns) {
			List<Word<PSymbolInstance>> hypTraces = new ArrayList<Word<PSymbolInstance>>(batchSize); 
			for (int i=0; i<batchSize; i++) {
				Word<PSymbolInstance> hypTrace = this.traceGenerator.generateTrace(hyp);
				if (hypTrace.toString().contains("IConnect") && !hypTrace.getSymbol(0).getBaseSymbol().getName().contains("IConnect")) {
					i--;
					continue;
				}
				hypTrace = traceCanonizer.canonizeTrace(hypTrace);
				hypTraces.add(hypTrace);
			}
			List<Word<PSymbolInstance>> sulTraces = this.target.traces(hypTraces);
			//sulTraces.forEach( tr -> System.out.println(tr));
			runs = runs+batchSize;
			for (int i=0; i<batchSize; i++) {
				Word<PSymbolInstance> hypTrace = hypTraces.get(i);
				Word<PSymbolInstance> sulTrace = sulTraces.get(i);
				if (hypTrace == null || sulTrace == null)
					continue;
				if (!hypTrace.equals(sulTrace)) {
					System.out.println("SUL Trace " + sulTrace.toString());
					System.out.println("HYP Trace " + hypTrace.toString());
					Word<PSymbolInstance> newSulTrace = this.target.trace(sulTrace);
					assert newSulTrace.equals(sulTrace);
					int j;
					for(j=0; hypTrace.getSymbol(j).equals(sulTrace.getSymbol(j)); j++);
					return new DefaultQuery<>(sulTrace.prefix(j+1));
				}
			}
			
			if (maxRuns / 100 * tens < runs) {
				System.out.println(tens + "% percent completed");
				tens++;
			}
		}
		return null;
	}
	
	

}
