package de.learnlib.ralib.sul.examples;

import de.learnlib.api.SULException;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteInput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteOutput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteSUL;

public class TwoTheoriesMingle extends ConcreteSUL{

	
	
	public void pre() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void post() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ConcreteOutput step(ConcreteInput in) throws SULException {
		if (in.getMethodName().equals("IMSG")) {
			Integer seq = (Integer) in.getParameterValues()[0];
			return new ConcreteOutput("OMSG", seq, seq);
		}
		
		return null;
	}

}
