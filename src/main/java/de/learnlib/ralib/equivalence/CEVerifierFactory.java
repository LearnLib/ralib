package de.learnlib.ralib.equivalence;

import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;

public class CEVerifierFactory {
	private SimulatorSUL hypSim;
	private SULOracle sul;
	private Map<DataType, Theory> teachers;
	private Constants constants;

	public CEVerifierFactory(SULOracle sul, Map<DataType, Theory> teachers, Constants constants) {
		this.sul = sul;
		this.teachers = teachers;
		this.constants = constants;
	}
	
	public ConcreteCEVerifier getCEVerifier(Hypothesis hyp) {
		return new ConcreteCEVerifier(this.sul, hyp, teachers, constants);
	}
}
