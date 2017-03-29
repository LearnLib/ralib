package de.learnlib.ralib.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

//parses exh suffixes from strings
public class SuffixParser {
	private static String SUFACTION_EX = "\\w+\\s*\\[[^\\]]*\\]\\s*";
	private static Pattern SUFACTION_MATCH = Pattern.compile(SUFACTION_EX);
	private static String SUFFIX_EX = "(" + SUFACTION_EX + ")+";
	private List<GeneralizedSymbolicSuffix> suffixes;
	
	public SuffixParser(List<String> suffixStrings,
			List<ParameterizedSymbol> actionSignatures, Map<DataType, Theory> theories) {
		this.suffixes = parseSuffixesFromStrings(suffixStrings, actionSignatures, theories);
	}
	
	
	public List<GeneralizedSymbolicSuffix> getSuffixes() {
		return suffixes;
	}

	private List<GeneralizedSymbolicSuffix> parseSuffixesFromStrings(List<String> suffixStrings,
			List<ParameterizedSymbol> actionSignatures, Map<DataType, Theory> theories) {
		List<GeneralizedSymbolicSuffix> suffixes = new ArrayList<>();
		final Map<String, ParameterizedSymbol> strToSym = new LinkedHashMap<>();
		actionSignatures.stream().forEach(act -> strToSym.put(act.getName(), act));
		for (String suffix : suffixStrings) {
			Word<ParameterizedSymbol> actions = Word.epsilon();
			if (!suffix.matches(SUFFIX_EX)) 
				throw new RuntimeException("Invalid test format, expected: a1(v1,v2) a2(v2,v3) ...");
			Matcher matcher = SUFACTION_MATCH.matcher(suffix);
			while (matcher.find()) {
				String actStr = matcher.group();
				actStr = actStr.replaceAll("\\s", "");
				String[] actSplit = actStr.split("\\[|\\]|\\,");
				String actName = actSplit[0].trim();
				ParameterizedSymbol act = strToSym.get(actName);
				if (act != null)
					actions = actions.append(act);
				else
					throw new RuntimeException("Could not find action " + actName + " in the list of signatures " + strToSym);
			}
			suffixes.add(GeneralizedSymbolicSuffix.fullSuffix(actions, theories));
		}
		return suffixes;
	}

}
