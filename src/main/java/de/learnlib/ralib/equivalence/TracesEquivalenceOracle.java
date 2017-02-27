package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.learnlib.api.EquivalenceOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class TracesEquivalenceOracle implements EquivalenceOracle<RegisterAutomaton, PSymbolInstance, Boolean> {

	private List<List<PSymbolInstance>> testTraces;
	private IOHypVerifier hypVerifier;
	private DataWordSUL target;

	public TracesEquivalenceOracle(DataWordSUL target, Map<DataType, Theory> teachers, Constants constants,
			List<List<PSymbolInstance>> tests) {
		this.hypVerifier = new IOHypVerifier(teachers, constants);
		this.testTraces = tests;
		this.target = target;
	}
	
	public TracesEquivalenceOracle(DataWordSUL target, Map<DataType, Theory> teachers, Constants constants,
			List<String> tests, List<ParameterizedSymbol> actionSignatures) {
		this.hypVerifier = new IOHypVerifier(teachers, constants);
		this.testTraces = parseTestsFromStrings(tests, actionSignatures);
		this.target = target;
	}
	
	private static String INPUT_EX = "\\w+\\s*\\([0-9\\.\\s\\,]*\\)\\s*";
	private static String TEST_EX =  "(" +  INPUT_EX + ")+"; 
	private static Pattern INPUT_MATCH = Pattern.compile(INPUT_EX);

	private List<List<PSymbolInstance>> parseTestsFromStrings(List<String> testStrings,
			List<ParameterizedSymbol> actionSignatures) {
		List<List<PSymbolInstance>> tests = new ArrayList<>();
		List<String> actionNames = actionSignatures.stream().map(actSig -> actSig.getName()).collect(Collectors.toList());
		for (String test : testStrings) {
			if (!test.matches(TEST_EX)) 
				throw new RuntimeException("Invalid test format, expected: a1(v1,v2) a2(v2,v3) ...");
			Matcher matcher = INPUT_MATCH.matcher(test);
			List<PSymbolInstance> testInputs = new ArrayList<>();
			while (matcher.find()) {
				String inpStr = matcher.group();
				inpStr = inpStr.replaceAll("\\s", "");
				PSymbolInstance input = parseSymInst(inpStr, actionSignatures);
				testInputs.add(input);
			}
			tests.add(testInputs);
		}
		return tests;
	}
	
	private PSymbolInstance parseSymInst(String inpString,  List<ParameterizedSymbol> actSignature) {
		DataValue dv;
		String[] inpSplit = inpString.split("\\(|\\)|\\,");
		String actName = inpSplit[0].trim();
		Optional<ParameterizedSymbol> matchingSig = actSignature.stream().filter(actSig -> actSig.getName().equals(actName)).findAny();
		if (!matchingSig.isPresent()) 
			throw new RuntimeException("Could not find action " + actName + " in the list of signatures " + actSignature);
		if (matchingSig.get().getArity() != inpSplit.length-1)
			throw new RuntimeException("Arity mismatch for " + actName);
		DataType [] pTypes = matchingSig.get().getPtypes();
		DataValue [] dvs = new DataValue [inpSplit.length-1];
		for (int i=1; i < inpSplit.length; i++) {
			dvs[i-1] = DataValue.valueOf(inpSplit[i], pTypes[i-1]);
		}
		return new PSymbolInstance(matchingSig.get(), dvs);
	} 

	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
			Collection<? extends PSymbolInstance> inputs) {
		System.out.println("Executing conformance tests:");
		for (List<PSymbolInstance> test : testTraces) {
			target.pre();
			Word<PSymbolInstance> run = Word.epsilon();
			for (PSymbolInstance input : test) {
				run = run.append(input);
				PSymbolInstance out = target.step(input);
				run = run.append(out);
				if (this.hypVerifier.isCEForHyp(run, hypothesis)) {
					//return new DefaultQuery<>(run, true);
					return null;
				}
			}
			target.post();
		}
		
		return null;
	}

}
