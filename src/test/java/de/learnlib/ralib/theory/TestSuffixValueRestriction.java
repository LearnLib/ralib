package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class TestSuffixValueRestriction extends RaLibTestSuite {

	private static final DataType T = new DataType("t");
	private static final InputSymbol A = new InputSymbol("α", T);

    @Test
    public void testCEAnalysisRestrictions() {
    	Theory theory = new IntegerEqualityTheory(T);
    	Map<DataType, Theory> teachers = Map.of(T, theory);

    	final DataValue dv1 = new DataValue(T, BigDecimal.ONE);
    	final DataValue dv2 = new DataValue(T, BigDecimal.valueOf(2));

    	Register r1 = new Register(T, 1);
    	Constant c1 = new Constant(T, 1);
    	SuffixValue s1 = new SuffixValue(T, 1);
    	SuffixValue s2 = new SuffixValue(T, 2);
    	SuffixValue s3 = new SuffixValue(T, 3);

    	// fresh
    	Word<PSymbolInstance> prefix1 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1));
    	Word<PSymbolInstance> suffix1 = Word.fromSymbols(
    			new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> u1 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1));
    	RegisterValuation val1 = new RegisterValuation();
    	RegisterValuation uval1 = new RegisterValuation();
    	Constants consts1 = new Constants();
    	SLLambdaRestrictionBuilder builder1 = new SLLambdaRestrictionBuilder(consts1, teachers);
    	AbstractSuffixValueRestriction restr1 = builder1.constructRestrictedSuffix(prefix1, suffix1, u1, val1, uval1).getRestriction(s1);
//    	AbstractSuffixValueRestriction restr1 = theory.restrictSuffixValue(s1, prefix1, suffix1, val1, consts1);
    	Assert.assertEquals(restr1.toString(), "Fresh(s1)");

    	// equal register, constant
    	Word<PSymbolInstance> prefix2 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> suffix2 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> u2 = prefix2;
    	RegisterValuation val2 = new RegisterValuation();
    	val2.put(r1, dv1);
    	RegisterValuation uval2 = val2;
    	Constants consts2 = new Constants();
    	consts2.put(c1, dv2);
    	SLLambdaRestrictionBuilder builder2 = new SLLambdaRestrictionBuilder(consts2, teachers);
    	AbstractSuffixValueRestriction restr2Reg = builder2.constructRestrictedSuffix(prefix2, suffix2, u2, val2, uval2).getRestriction(s1);
    	AbstractSuffixValueRestriction restr2Con = builder2.constructRestrictedSuffix(prefix2, suffix2, u2, val2, uval2).getRestriction(s2);
//    	AbstractSuffixValueRestriction restr2Reg = theory.restrictSuffixValue(s1, prefix2, suffix2, val2, consts2);
//    	AbstractSuffixValueRestriction restr2Con = theory.restrictSuffixValue(s2, prefix2, suffix2, val2, consts2);
    	Assert.assertEquals(restr2Reg.toString(), "((s1 == r1) OR Fresh(s1))");
    	Assert.assertEquals(restr2Con.toString(), "(s2 == c1)");

    	// equal suffix
    	Word<PSymbolInstance> prefix3 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1));
    	Word<PSymbolInstance> suffix3 = Word.fromSymbols(
    			new PSymbolInstance(A, dv2),
    			new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> u3 = prefix3;
    	RegisterValuation val3 = new RegisterValuation();
    	RegisterValuation uval3 = val3;
    	Constants consts3 = new Constants();
    	SLLambdaRestrictionBuilder builder3 = new SLLambdaRestrictionBuilder(consts3, teachers);
    	AbstractSuffixValueRestriction restr3 = builder3.constructRestrictedSuffix(prefix3, suffix3, u3, val3, uval3).getRestriction(s2);
//    	AbstractSuffixValueRestriction restr3 = theory.restrictSuffixValue(s2, prefix3, suffix3, val3, consts3);
    	Assert.assertEquals(restr3.toString(), "(s2 == s1)");

    	// equal mapped
    	Word<PSymbolInstance> prefix4 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> suffix4 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv1));
    	Word<PSymbolInstance> u4 = prefix4;
    	RegisterValuation val4 = new RegisterValuation();
    	val4.put(r1, dv1);
    	RegisterValuation uval4 = val4;
    	Constants consts4 = new Constants();
    	consts4.put(c1, dv1);
    	SLLambdaRestrictionBuilder builder4 = new SLLambdaRestrictionBuilder(consts4, teachers);
    	AbstractSuffixValueRestriction restr4_1 = builder4.constructRestrictedSuffix(prefix4, suffix4, u4, val4, uval4).getRestriction(s1);
    	AbstractSuffixValueRestriction restr4_2 = builder4.constructRestrictedSuffix(prefix4, suffix4, u4, val4, uval4).getRestriction(s2);
//    	AbstractSuffixValueRestriction restr4_1 = theory.restrictSuffixValue(s1, prefix4, suffix4, val4, consts4);
//    	AbstractSuffixValueRestriction restr4_2 = theory.restrictSuffixValue(s2, prefix4, suffix4, val4, consts4);
    	Assert.assertEquals(restr4_1.toString(), "(Fresh(s1) OR (s1 == r1) OR (s1 == c1))");
    	Assert.assertEquals(restr4_2.toString(), "(Fresh(s2) OR (s2 == s1) OR (s2 == r1) OR (s2 == c1))");

    	// equal unmapped
    	Word<PSymbolInstance> prefix5 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2),
    			new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> suffix5 = Word.fromSymbols(
    			new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> u5 = prefix5;
    	RegisterValuation val5 = new RegisterValuation();
    	val5.put(r1, dv1);
    	RegisterValuation uval5 = val5;
    	Constants consts5 = new Constants();
    	SLLambdaRestrictionBuilder builder5 = new SLLambdaRestrictionBuilder(consts5, teachers);
    	AbstractSuffixValueRestriction restr5 = builder5.constructRestrictedSuffix(prefix5, suffix5, u5, val5, uval5).getRestriction(s1);
//    	AbstractSuffixValueRestriction restr5 = theory.restrictSuffixValue(s1, prefix5, suffix5, val5, consts5);
    	Assert.assertEquals(restr5.toString(), "(Unmapped(s1) OR Fresh(s1))");

    	// equal multiple suffix values
    	Word<PSymbolInstance> prefix6 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1));
    	Word<PSymbolInstance> suffix6 = Word.fromSymbols(
    			 new PSymbolInstance(A, dv2),
    			 new PSymbolInstance(A, dv2),
    			 new PSymbolInstance(A, dv2));
    	Word<PSymbolInstance> u6 = prefix6;
    	RegisterValuation val6 = new RegisterValuation();
    	val6.put(r1, dv1);
    	RegisterValuation uval6 = val6;
    	Constants consts6 = new Constants();
    	SLLambdaRestrictionBuilder builder6 = new SLLambdaRestrictionBuilder(consts6, teachers);
    	AbstractSuffixValueRestriction restr6 = builder6.constructRestrictedSuffix(prefix6, suffix6, u6, val6, uval6).getRestriction(s3);
//    	AbstractSuffixValueRestriction restr6 = theory.restrictSuffixValue(s3, prefix6, suffix6, val6, consts6);
    	Assert.assertEquals(restr6.toString(), "(s3 == s1)");

    	// repeat occurrence of mapped
    	Word<PSymbolInstance> prefix7 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv1));
    	Word<PSymbolInstance> suffix7 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1));
    	Word<PSymbolInstance> u7 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2));
    	RegisterValuation val7 = new RegisterValuation();
    	val7.put(r1, dv1);
    	RegisterValuation uval7 = val7;
    	Constants consts7 = new Constants();
    	SLLambdaRestrictionBuilder builder7 = new SLLambdaRestrictionBuilder(consts7, teachers);
    	AbstractSuffixValueRestriction restr7 = builder7.constructRestrictedSuffix(prefix7, suffix7, u7, val7, uval7).getRestriction(s1);
    	Assert.assertEquals(restr7.toString(), "true");
    }

    @Test
    public void testConcretize() {
    	Theory theory = new IntegerEqualityTheory(T);
    	Map<DataType, Theory> teachers = new LinkedHashMap<>();
    	teachers.put(T, theory);

    	final DataValue dv1 = new DataValue(T, BigDecimal.ONE);
    	final DataValue dv2 = new DataValue(T, BigDecimal.valueOf(2));
    	final DataValue dv3 = new DataValue(T, BigDecimal.valueOf(3));
    	final DataValue dv4 = new DataValue(T, BigDecimal.valueOf(4));
    	final DataValue dv5 = new DataValue(T, BigDecimal.valueOf(5));

    	Register r1 = new Register(T, 1);
    	Constant c1 = new Constant(T, 1);
    	SuffixValue s1 = new SuffixValue(T, 1);
    	SuffixValue s2 = new SuffixValue(T, 2);
    	SuffixValue s3 = new SuffixValue(T, 3);
    	SuffixValue s4 = new SuffixValue(T, 4);
    	SuffixValue s5 = new SuffixValue(T, 5);
    	SuffixValue[] svs = {s1, s2, s3, s4, s5};

    	Word<PSymbolInstance> prefix1 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2),
    			new PSymbolInstance(A, dv3),
    			new PSymbolInstance(A, dv4));
    	Word<PSymbolInstance> suffix1 = Word.fromSymbols(
    			new PSymbolInstance(A, dv1),
    			new PSymbolInstance(A, dv2),
    			new PSymbolInstance(A, dv3),
    			new PSymbolInstance(A, dv5),
    			new PSymbolInstance(A, dv5));
    	RegisterValuation val1 = new RegisterValuation();
    	val1.put(r1, dv1);
    	Constants consts1 = new Constants();
    	consts1.put(c1, dv2);
    	Map<SuffixValue, AbstractSuffixValueRestriction> restrs = new LinkedHashMap<>();
    	for (SuffixValue s : svs) {
    		restrs.put(s, theory.restrictSuffixValue(s, prefix1, suffix1, val1, consts1));
    	}
    	SymbolicSuffix symSuff1 = new SymbolicSuffix(DataWords.actsOf(suffix1), restrs);

    	Assert.assertEquals(symSuff1.toString(), "((?α[t] ?α[t] ?α[t] ?α[t] ?α[t]))[(s1 == r1), (s2 == c1), Unmapped(s3), Fresh(s4), (s5 == s4)]");

    	SLLambdaRestrictionBuilder restrBuilder = new SLLambdaRestrictionBuilder(consts1, teachers);
    	SymbolicSuffix symSuff1Conc = restrBuilder.concretize(symSuff1,
    			val1,
    			consts1,
    			ParameterValuation.fromPSymbolWord(prefix1));

    	Assert.assertEquals(symSuff1Conc.toString(), "((?α[t] ?α[t] ?α[t] ?α[t] ?α[t]))[(s1 == 1[t]), (s2 == 2[t]), (s3 == 3[t]) OR (s3 == 4[t]), Fresh(s4), (s5 == s4)]");
    }
}
