package de.learnlib.ralib.oracles.mto;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class SymbolicSuffixRestrictionBuilder {

	public enum Version {
		V1,
		V2,
		V3
	};

	public final static Version DEFAULT_VERSION = Version.V3;

    protected final Map<DataType, Theory> teachers;

    protected final Constants consts;

    private final Version version;

    public SymbolicSuffixRestrictionBuilder(Constants consts, Map<DataType, Theory> teachers, Version version) {
    	this.consts = consts;
    	this.teachers = teachers;
    	this.version = version;
    }

    public SymbolicSuffixRestrictionBuilder(Constants consts) {
    	this(consts, null, DEFAULT_VERSION);
    }

    public SymbolicSuffixRestrictionBuilder(Constants consts, Map<DataType, Theory> teachers) {
    	this(consts, teachers, DEFAULT_VERSION);
    }

    public SymbolicSuffixRestrictionBuilder(Map<DataType, Theory> teachers) {
        this(new Constants(), teachers, DEFAULT_VERSION);
    }

    public SymbolicSuffixRestrictionBuilder() {
        this(new Constants(), null, DEFAULT_VERSION);
    }

    public Map<SuffixValue, AbstractSuffixValueRestriction> restrictSuffix(Word<PSymbolInstance> prefix,
    		Word<PSymbolInstance> suffix,
    		Word<PSymbolInstance> u,
    		RegisterValuation wValuation,
    		RegisterValuation uValuation) {
    	DataType[] types = DataWords.typesOf(DataWords.actsOf(suffix));
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
    	SuffixValueGenerator svgen = new SuffixValueGenerator();
    	for (DataType t : types) {
    		SuffixValue sv = svgen.next(t);
    		AbstractSuffixValueRestriction restr;
    		if (teachers == null) {
    			restr = AbstractSuffixValueRestriction.genericRestriction(sv, prefix, suffix, consts);
    		} else {
    			Theory theory = teachers.get(t);
    			restr = theory.restrictSuffixValue(sv, prefix, suffix, u, wValuation, uValuation, consts);
    		}
    		restrictions.put(sv, restr);
    	}
    	return restrictions;
    }

    public Map<SuffixValue, AbstractSuffixValueRestriction> restrictSuffix(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix) {
        DataType[] types = DataWords.typesOf(DataWords.actsOf(suffix));
        Map<SuffixValue, AbstractSuffixValueRestriction> restrictions = new LinkedHashMap<>();
        SuffixValueGenerator svgen = new SuffixValueGenerator();
        for (DataType t : types) {
            SuffixValue sv = svgen.next(t);
            AbstractSuffixValueRestriction restr;
            if (teachers == null) {
                // use standard restrictions
                restr = AbstractSuffixValueRestriction.genericRestriction(sv, prefix, suffix, consts);
            } else {
                // theory-specific restrictions
                Theory theory = teachers.get(t);
                restr = theory.restrictSuffixValue(sv, prefix, suffix, consts, version);
            }
            restrictions.put(sv, restr);
        }
        return restrictions;
    }

    public AbstractSuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, AbstractSuffixValueRestriction> prior) {
        if (teachers == null)
            return AbstractSuffixValueRestriction.genericRestriction(guard, prior);
        Theory theory = teachers.get(guard.getParameter().getDataType());
        return theory.restrictSuffixValue(guard, prior, version);
    }

    public boolean sdtPathRevealsRegister(List<SDTGuard> path, SymbolicDataValue[] registers) {
        if (teachers == null)
            return false;
        Set<SymbolicDataValue> revealedRegisters = new LinkedHashSet<>();
        for (SDTGuard guard : path) {
            Theory theory = teachers.get(guard.getParameter().getDataType());
            for (SymbolicDataValue r : registers) {
                if (theory.guardRevealsRegister(guard, r)) {
                    revealedRegisters.add(r);
                }
            }
        }
        return revealedRegisters.size() == registers.length;
    }
}
