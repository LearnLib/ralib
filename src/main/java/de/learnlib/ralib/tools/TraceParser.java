package de.learnlib.ralib.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

// Parses tests from strings
public class TraceParser {
	private static String INPUT_EX = "\\w+\\s*\\[[0-9\\.\\(\\)\\+\\:\\s\\,]*\\]\\s*";
	private static Pattern SYMINST_MATCH = Pattern.compile(INPUT_EX);
	private static String TRACE_EX =  "(" +  INPUT_EX + ")+";
	private List<Word<PSymbolInstance>> traces; 
	
	
	public TraceParser(List<String> testStrings,
			List<ParameterizedSymbol> actionSignatures) {
		this.traces = this.parseTestsFromStrings(testStrings, actionSignatures);
	}
	
	public List<Word<PSymbolInstance>> getTraces() {
		return this.traces;
	}
	
	public List<List<PSymbolInstance>> getInputSequencesForTraces() {
		List<List<PSymbolInstance>> inputSequences = new ArrayList<>(this.traces.size());
		for (Word<PSymbolInstance> trace : traces) {
    		List<PSymbolInstance> inputs = trace.stream().filter(s -> 
    		(s.getBaseSymbol() instanceof InputSymbol)).collect(Collectors.toList());
    		inputSequences.add(inputs);
		}
		
		return inputSequences;
	}
	
	private List<Word<PSymbolInstance>> parseTestsFromStrings(List<String> traceStrings,
			List<ParameterizedSymbol> actionSignatures) {
		List<Word<PSymbolInstance>> tests = new ArrayList<>();
		final Map<String, ParameterizedSymbol> strToSym = new LinkedHashMap<>();
		actionSignatures.stream().forEach(act -> strToSym.put(act.getName(), act));
		for (String trace : traceStrings) {
			trace = trace.replaceAll("\\[[a-zA-Z]+\\]", "");
			if (!trace.matches(TRACE_EX)) 
				throw new RuntimeException("Invalid test format, expected: a1[v1,v2] a2[v2,v3] ...");
			Matcher matcher = SYMINST_MATCH.matcher(trace);
			Word<PSymbolInstance> testConcActions =Word.epsilon();
			while (matcher.find()) {
				String inpStr = matcher.group();
				inpStr = inpStr.replaceAll("\\s", "");
				PSymbolInstance action = parseSymInst(inpStr, strToSym);
				testConcActions = testConcActions.append(action);
			}
			tests.add(testConcActions);
		}
		return tests;
	}
	
	private PSymbolInstance parseSymInst(String inpString,  Map<String, ParameterizedSymbol> strToSym) {
		DataValue dv;
		String[] inpSplit = inpString.split("\\[|\\]|\\,");
		String actName = inpSplit[0];
		ParameterizedSymbol matchingSig = strToSym.get(actName);
		if (matchingSig == null) 
			throw new RuntimeException("Could not find action " + actName + " in the list of signatures " + strToSym);
		if (matchingSig.getArity() != inpSplit.length-1)
			throw new RuntimeException("Arity mismatch for " + actName);
		DataType [] pTypes = matchingSig.getPtypes();
		DataValue [] dvs = new DataValue [inpSplit.length-1];
		
		for (int i=1; i < inpSplit.length; i++) 
			dvs[i-1] = parseInput(inpSplit[i], pTypes[i-1]);
		
		return new PSymbolInstance(matchingSig, dvs);
	} 
	
	public DataValue parseInput(String inpString, DataType type) {
		DataValue dv;
		
		if (inpString.contains("(")) {
			String beforeParan = inpString.substring(0, inpString.indexOf("("));
			DataValue actDv = parseInput(beforeParan, type);
			String betweenParan =  inpString.substring(inpString.indexOf("(")+1, inpString.lastIndexOf(")"));
			char[] chars = betweenParan.toCharArray();
			int lvl = 0, intSplit;
			for (intSplit =0; intSplit < chars.length; intSplit++) {
				char c = chars[intSplit];
				if (c == '(')
					lvl ++;
				else if (c==')')
					lvl --;
				else if (c== ':' && lvl == 0) 
					break;
			}
			String smDvString = betweenParan.substring(0, intSplit);
			String bgDvString = betweenParan.substring(intSplit+1);
			DataValue smDv = null;
			DataValue bgDv = null;
			if (!smDvString.isEmpty())
				smDv = parseInput(smDvString, type);
			if (!bgDvString.isEmpty())
				bgDv = parseInput(bgDvString, type);
			
			dv = new IntervalDataValue(actDv, smDv, bgDv);
		} else if (inpString.contains("+")) {
			String[] terms = inpString.split("\\+");
			dv = parseInput(terms[0], type);
			for (int j=1; j < terms.length; j++) 
				dv = new SumCDataValue(dv, DataValue.valueOf(terms[j], type));
		} else {
			dv = DataValue.valueOf(inpString, type);
		}
		
		return dv;
	}
	
}
