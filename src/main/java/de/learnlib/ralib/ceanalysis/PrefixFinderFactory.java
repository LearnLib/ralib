package de.learnlib.ralib.ceanalysis;

import java.util.Map;

import de.learnlib.ralib.ct.CTHypothesis;
import de.learnlib.ralib.ct.ClassificationTree;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;

public class PrefixFinderFactory {
	public enum PrefixFinderType {
		Default,
		Eq
	};

	private PrefixFinderType type = PrefixFinderType.Default;

	private final TreeOracle sulOracle;
	private final Map<DataType, Theory> teachers;
//	private final SymbolicSuffixRestrictionBuilder restrBuilder;
	private final SymbolicSuffixRestrictionBuilder restrBuilder;
	private final ConstraintSolver solver;
	private final Constants consts;

	public PrefixFinderFactory(TreeOracle sulOracle, Map<DataType, Theory> teachers,
			SymbolicSuffixRestrictionBuilder restrBuilder, ConstraintSolver solver, Constants consts) {
		this.sulOracle = sulOracle;
		this.teachers = teachers;
		this.restrBuilder = restrBuilder;
		this.solver = solver;
		this.consts = consts;
	}

	public void setPrefixFinderType(PrefixFinderType type) {
		this.type = type;
	}

	public PrefixFinder create(CTHypothesis hyp, ClassificationTree ct) {
		if (type == PrefixFinderType.Eq) {
			assert restrBuilder instanceof SLLambdaRestrictionBuilder;
			return new PrefixFinderEq(sulOracle, hyp, ct, teachers, (SLLambdaRestrictionBuilder) restrBuilder, solver, consts);
		}
		return new PrefixFinder(sulOracle, hyp, ct, teachers, restrBuilder, solver, consts);
	}
}
