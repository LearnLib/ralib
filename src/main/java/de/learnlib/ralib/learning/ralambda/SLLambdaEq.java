package de.learnlib.ralib.learning.ralambda;

import java.util.Map;

import de.learnlib.ralib.ceanalysis.PrefixFinderFactory;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.SLLambdaEqRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class SLLambdaEq extends SLLambda {

	public SLLambdaEq(TreeOracle sulOracle, Map<DataType, Theory> teachers, Constants consts, boolean ioMode,
			ConstraintSolver solver, ParameterizedSymbol ... inputs) {
//		super(sulOracle, teachers, consts, ioMode, solver, SymbolicSuffixRestrictionBuilder.Version.V3, inputs);
		super(sulOracle, teachers, consts, ioMode, solver, new SLLambdaEqRestrictionBuilder(consts, teachers, solver), inputs);
		prefixFinderFactory.setPrefixFinderType(PrefixFinderFactory.PrefixFinderType.Eq);
	}
}
