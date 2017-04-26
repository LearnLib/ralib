package de.learnlib.ralib.sul;

import de.learnlib.api.SULException;
import de.learnlib.ralib.words.PSymbolInstance;

public class CountingDataWordSUL extends DataWordSUL{
	private DataWordSUL sul;
	private InputCounter inputCounter;

	public CountingDataWordSUL(DataWordSUL dwSUL, InputCounter inputCounter) {
		this.sul = dwSUL;
		this.inputCounter = inputCounter;
	}

	@Override
	public void pre() {
		this.sul.pre();
		this.inputCounter.countResets(1);
	}

	@Override
	public void post() {
		this.sul.post();
	}

	@Override
	public PSymbolInstance step(PSymbolInstance in) throws SULException {
		this.inputCounter.countInputs(1);
		PSymbolInstance out = this.sul.step(in);
		return out;
	}
}
