package de.learnlib.ralib.oracles.mto;

import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class SymbolicSuffixRestrictionBuilder {

	private final Map<DataType, Theory> teachers;

	private final Constants consts;

	public SymbolicSuffixRestrictionBuilder(Constants consts) {
		this.consts = consts;
		this.teachers = null;
	}

	public SymbolicSuffixRestrictionBuilder(Constants consts, Map<DataType, Theory> teachers) {
		this.consts = consts;
		this.teachers = teachers;
	}

	public SymbolicSuffixRestrictionBuilder(Map<DataType, Theory> teachers) {
		this(new Constants(), teachers);
	}

	public SymbolicSuffixRestrictionBuilder() {
		this(new Constants());
	}


    public Map<SuffixValue, SuffixValueRestriction> restrictSuffix(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix) {
    	DataType[] types = DataWords.typesOf(DataWords.actsOf(suffix));
    	Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	SuffixValueGenerator svgen = new SuffixValueGenerator();
    	for (DataType t : types) {
    		SuffixValue sv = svgen.next(t);
    		SuffixValueRestriction restr;
    		if (teachers == null) {
    			// use standard restrictions
    			restr = SuffixValueRestriction.generateGenericRestriction(sv, prefix, suffix, consts);
    		} else {
    			// theory-specific restrictions
    			Theory<?> theory = teachers.get(t);
    			restr = theory.restrictSuffixValue(sv, prefix, suffix, consts);
    		}
    		restrictions.put(sv, restr);
    	}
    	return restrictions;
    }
}
