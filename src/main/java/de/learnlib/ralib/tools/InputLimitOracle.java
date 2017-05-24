package de.learnlib.ralib.tools;

import de.learnlib.api.SULException;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.ToolTemplate.Counters;
import de.learnlib.ralib.words.PSymbolInstance;

public class InputLimitOracle extends DataWordSUL{
	
	private Counters counters;
	private DataWordSUL sul;
	private long limit;

	public InputLimitOracle(DataWordSUL sul, Counters counters, long limit) {
		this.counters = counters;
		this.sul = sul;
		this.limit = limit;
	}

	public void pre() {
		this.sul.pre();
	}

	@Override
	public void post() {
		this.sul.post();
	}

	@Override
	public PSymbolInstance step(PSymbolInstance in) throws SULException {
		if (this.counters.getTotalNumInputs() > limit) {
			this.sul.post();
			System.err.println("Input limit reached: " + limit);
			this.counters.print(System.err);
			System.exit(0);
		}
		PSymbolInstance out = this.sul.step(in);
		return out;
	}

}
