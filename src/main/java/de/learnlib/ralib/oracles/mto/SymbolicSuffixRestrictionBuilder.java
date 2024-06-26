package de.learnlib.ralib.oracles.mto;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

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
    			restr = SuffixValueRestriction.genericRestriction(sv, prefix, suffix, consts);
    		} else {
    			// theory-specific restrictions
                Theory<?> theory = teachers.get(t);
    			restr = theory.restrictSuffixValue(sv, prefix, suffix, consts);
    		}
    		restrictions.put(sv, restr);
    	}
    	return restrictions;
    }

    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
    	if (teachers == null)
    		return SuffixValueRestriction.genericRestriction(guard, prior);
        Theory<?> theory = teachers.get(guard.getParameter().getType());
    	return theory.restrictSuffixValue(guard, prior);
    }

    public boolean sdtPathRevealsRegister(List<SDTGuard> path, SymbolicDataValue[] registers) {
    	if (teachers == null)
    		return false;
    	Set<SymbolicDataValue> revealedRegisters = new LinkedHashSet<>();
    	for (SDTGuard guard : path) {
            Theory<?> theory = teachers.get(guard.getParameter().getType());
    		for (SymbolicDataValue r : registers) {
    			if (theory.guardRevealsRegister(guard, r)) {
    				revealedRegisters.add(r);
    			}
    		}
    	}
    	return revealedRegisters.size() == registers.length;
    }
}
