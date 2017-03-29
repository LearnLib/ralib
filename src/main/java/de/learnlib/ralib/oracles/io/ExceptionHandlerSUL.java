package de.learnlib.ralib.oracles.io;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.api.SULException;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.exceptions.NonDeterminismException;
import de.learnlib.ralib.exceptions.SULRestartException;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;

public class ExceptionHandlerSUL extends DataWordSUL implements ExceptionHandler {
	private DataWordSUL sul;
	private List<PSymbolInstance> inputs;

	public ExceptionHandlerSUL(DataWordSUL dwSul) {
		this.sul = dwSul;
		this.inputs = new ArrayList<>();
	}

	public void pre() {
		this.sul.pre();
		this.inputs = new ArrayList<>();
	}

	@Override
	public void post() {
		this.sul.post();
	}

	@Override
	public PSymbolInstance step(PSymbolInstance in) throws SULException {
		int nonDet = 0, sulRest = 0;
		DecoratedRuntimeException lastExc = null;

		while (nonDet < NON_DET_ATTEMPTS && sulRest < SUL_RESTART_ATTEMPTS) {
			try {
				if (lastExc != null) 
					recover();
				PSymbolInstance out = this.sul.step(in);
				this.inputs.add(in);
				return out;
			} catch(NonDeterminismException exc) {
				nonDet ++;
				lastExc = exc;
			} catch(SULRestartException exc) {
				sulRest ++;
				System.out.println("SUL issued restart");
				lastExc = exc;
			}
		}
		throw lastExc;
	}
	
	private void recover() {
		this.sul.post();
		this.sul.pre();
		this.inputs.forEach(in -> this.sul.step(in));
	}
}
