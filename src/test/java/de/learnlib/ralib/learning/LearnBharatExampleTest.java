/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.learning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.HypVerifier;
import de.learnlib.ralib.example.ineq.BharatExampleSUL;
import de.learnlib.ralib.example.ineq.BharatExampleSUL.Actions;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author Bharat Garhewal
 */
/*
 * The test involves learning a system with few locations but complex guards.
 */
public class LearnBharatExampleTest extends RaLibLearningTestSuite {

	public static final DataType doubleType = new DataType("DOUBLE", Double.class);

	public LearnBharatExampleTest() {
	}

	@Test
	public void LearnBharatExampleIO() {

		final ParameterizedSymbol ERROR = new OutputSymbol("_io_err", new DataType[] {});
		final ParameterizedSymbol OK = new OutputSymbol("_ok", new DataType[] {});
		final ParameterizedSymbol NOK = new OutputSymbol("_not_ok", new DataType[] {});

		final ParameterizedSymbol PUT = new InputSymbol("put", new DataType[] { doubleType, doubleType, doubleType });

		List<ParameterizedSymbol> inputList = new ArrayList<ParameterizedSymbol>();
		List<ParameterizedSymbol> outputList = new ArrayList<ParameterizedSymbol>();

		inputList.add(PUT);
		outputList.add(OK);
		outputList.add(NOK);

		List<ParameterizedSymbol> actionList = new ArrayList<ParameterizedSymbol>();
		actionList.addAll(inputList);
		actionList.addAll(outputList);
		final ParameterizedSymbol[] actionArray = actionList.toArray(new ParameterizedSymbol[actionList.size()]);

		Map<BharatExampleSUL.Actions, ParameterizedSymbol> inputs = new LinkedHashMap<BharatExampleSUL.Actions, ParameterizedSymbol>();
		inputs.put(Actions.PUT, PUT);

		Map<BharatExampleSUL.Actions, ParameterizedSymbol> outputs = new LinkedHashMap<BharatExampleSUL.Actions, ParameterizedSymbol>();
		outputs.put(Actions.OK, OK);
		outputs.put(Actions.NOK, NOK);

		final Constants consts = new Constants();

		long seed = -4750580074638681533L;
		// long seed = (new Random()).nextLong();
		System.out.println("SEED=" + seed);
		final Random random = new Random(seed);

		final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
		HypVerifier hypVerifier = HypVerifier.getVerifier(true, teachers, consts);
		class Cpr implements Comparator<DataValue<Double>> {

			public int compare(DataValue<Double> one, DataValue<Double> other) {
				return one.getId().compareTo(other.getId());
			}
		}
		teachers.put(doubleType, new DoubleInequalityTheory(doubleType));
		
		DataWordSUL sul = new BharatExampleSUL(teachers, consts, inputs, outputs);
		
		super.setHypValidator((hyp) -> {
			PSymbolInstance[][] tests = {
					{ new PSymbolInstance(PUT, new DataValue(doubleType, 1.0), new DataValue(doubleType, 2.0),
							new DataValue(doubleType, 2.0)), new PSymbolInstance(NOK) },
					{ new PSymbolInstance(PUT, new DataValue(doubleType, 1.0), new DataValue(doubleType, 3.0),
							new DataValue(doubleType, 2.0)), new PSymbolInstance(OK) },
					{ new PSymbolInstance(PUT, new DataValue(doubleType, 1.0), new DataValue(doubleType, 2.0),
							new DataValue(doubleType, 3.0)), new PSymbolInstance(OK) },
					{ new PSymbolInstance(PUT, new DataValue(doubleType, 1.0), new DataValue(doubleType, 2.0),
							new DataValue(doubleType, 3.0)), new PSymbolInstance(OK) },
					{ new PSymbolInstance(PUT, new DataValue(doubleType, 3.0), new DataValue(doubleType, 1.0),
							new DataValue(doubleType, 2.0)), new PSymbolInstance(NOK) } };

			for (PSymbolInstance[] test : tests) {
				DefaultQuery<PSymbolInstance, Boolean> ceQuery = new DefaultQuery<PSymbolInstance, Boolean>(
						Word.fromSymbols(test), Boolean.TRUE);
				boolean isCE = hypVerifier.isCEForHyp(ceQuery, hyp);
				Assert.assertFalse(isCE);
			}
		});
		super.runIOLearningExperiments(sul, teachers, consts, false, TestUtil.getZ3Solver(), actionArray, ERROR);
	}
}
