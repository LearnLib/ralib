package de.learnlib.ralib.theory.inequality;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.AbstractSuffixValueRestriction;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.UnrestrictedSuffixValue;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class IneqTheoryRestrictionsTest extends RaLibTestSuite {

    private final DataType D_TYPE = new DataType("double");

    private final InputSymbol A = new InputSymbol("a", D_TYPE);

    @Test
    public void testOptimizationFromConcreteValues() {
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DoubleInequalityTheory dit = new DoubleInequalityTheory(D_TYPE);
        dit.useSuffixOptimization(true);
        teachers.put(D_TYPE, dit);

        Constants consts = new Constants();
        SymbolicSuffixRestrictionBuilder builder = new SymbolicSuffixRestrictionBuilder(consts, teachers);

        DataValue dv0 = new DataValue(D_TYPE, BigDecimal.ZERO);
        DataValue dv1 = new DataValue(D_TYPE, BigDecimal.ONE);
        DataValue dv2 = new DataValue(D_TYPE, BigDecimal.valueOf(2));
        DataValue dv3 = new DataValue(D_TYPE, BigDecimal.valueOf(3));

        SuffixValueGenerator sgen = new SuffixValueGenerator();
        SuffixValue s1 = sgen.next(D_TYPE);
        SuffixValue s2 = sgen.next(D_TYPE);
        SuffixValue s3 = sgen.next(D_TYPE);
        SuffixValue s4 = sgen.next(D_TYPE);

        Word<PSymbolInstance> prefix1 = Word.fromSymbols(new PSymbolInstance(A, dv0));
        Word<PSymbolInstance> suffix1 = Word.fromSymbols(
        		new PSymbolInstance(A, dv0),
        		new PSymbolInstance(A, dv2),
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv3));

        Map<SuffixValue, AbstractSuffixValueRestriction> restr1 = new LinkedHashMap<>();
        restr1.put(s1, new UnrestrictedSuffixValue(s1));
        restr1.put(s2, new GreaterSuffixValue(s2));
        restr1.put(s3, new UnrestrictedSuffixValue(s3));
        restr1.put(s4, new GreaterSuffixValue(s4));

        SymbolicSuffix expected1 = new SymbolicSuffix(DataWords.actsOf(suffix1), restr1);
        SymbolicSuffix actual1 = new SymbolicSuffix(prefix1, suffix1, builder);

        Assert.assertEquals(actual1, expected1);

        Word<PSymbolInstance> prefix2 = Word.epsilon();
        Word<PSymbolInstance> suffix2 = Word.fromSymbols(
        		new PSymbolInstance(A, dv2),
        		new PSymbolInstance(A, dv0),
        		new PSymbolInstance(A, dv1),
        		new PSymbolInstance(A, dv0));

        Map<SuffixValue, AbstractSuffixValueRestriction> restr2 = new LinkedHashMap<>();
        restr2.put(s1, new UnrestrictedSuffixValue(s1));
        restr2.put(s2, new LesserSuffixValue(s2));
        restr2.put(s3, new UnrestrictedSuffixValue(s3));
        restr2.put(s4, new UnrestrictedSuffixValue(s4));

        SymbolicSuffix expected2 = new SymbolicSuffix(DataWords.actsOf(suffix2), restr2);
        SymbolicSuffix actual2 = new SymbolicSuffix(prefix2, suffix2, builder);

        Assert.assertEquals(actual2, expected2);
    }
}
