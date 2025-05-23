package de.learnlib.ralib.learning;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class SymbolicSuffixTest extends RaLibTestSuite {

  @Test
  public void testConcat() {

      RegisterAutomatonImporter loader = TestUtil.getLoader(
              "/de/learnlib/ralib/automata/xml/fifo7.xml");

      //RegisterAutomaton model = loader.getRegisterAutomaton();
      //logger.log(Level.FINE, "SYS: {0}", model);

      Constants consts = loader.getConstants();

      final Map<DataType, Theory> teachers = new LinkedHashMap<>();
      loader.getDataTypes().stream().forEach((t) -> {
          TypedTheory theory = new IntegerEqualityTheory(t);
          theory.setUseSuffixOpt(true);
          teachers.put(t, theory);
      });

      SymbolicSuffixRestrictionBuilder restrictionBuilder = new SymbolicSuffixRestrictionBuilder(consts, teachers);

      DataType intType = TestUtil.getType("int", loader.getDataTypes());

      ParameterizedSymbol iput = new InputSymbol("IPut", intType);
      ParameterizedSymbol iget = new InputSymbol("IGet");
      ParameterizedSymbol oget = new OutputSymbol("OGet", intType);
      ParameterizedSymbol ook = new OutputSymbol("OOK");

      DataValue d0 = new DataValue(intType, BigDecimal.ZERO);
      DataValue d1 = new DataValue(intType, BigDecimal.ONE);
      DataValue d6 = new DataValue(intType, new BigDecimal( 6));

      //****** IPut[0[int]] OOK[] IPut[1[int]] OOK[]
      Word<PSymbolInstance> prefix1 = Word.fromSymbols(
              new PSymbolInstance(iput,d0),
              new PSymbolInstance(ook),
              new PSymbolInstance(iput,d1),
              new PSymbolInstance(ook));

      //**** [s2, s3, s4, s5]((IPut[s1] OOK[] IPut[s2] OOK[] IGet[] OGet[s3] IGet[] OGet[s4] IGet[] OGet[s1] IGet[] OGet[s5]))
      Word<PSymbolInstance> suffix1 =  Word.fromSymbols(
              new PSymbolInstance(iput, d6),
              new PSymbolInstance(ook),
              new PSymbolInstance(iput,d0),
              new PSymbolInstance(ook));

      Word<PSymbolInstance> prefix2 = Word.fromSymbols(new PSymbolInstance(iget),
      new PSymbolInstance(oget,d0),
      new PSymbolInstance(iget));

      Word<PSymbolInstance> suffix2 =  Word.fromSymbols(
      new PSymbolInstance(oget,d0),
      new PSymbolInstance(iget),
      new PSymbolInstance(oget, d6),
      new PSymbolInstance(iget),
      new PSymbolInstance(oget,d0));

      SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix1, suffix1, restrictionBuilder);
      SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix2, suffix2, restrictionBuilder);

      LinkedHashMap<Integer, SuffixValue> dataValues = new LinkedHashMap<Integer, SuffixValue>();
      for (int i = 1; i <= 5; i++) {
    	  dataValues.put(i, new SuffixValue(intType, i));
      }

      LinkedHashSet<SuffixValue> freeValues = new LinkedHashSet<>();
      freeValues.add(new SuffixValue(intType, 2));
      freeValues.add(new SuffixValue(intType, 3));
      freeValues.add(new SuffixValue(intType, 5));

      Word<ParameterizedSymbol> actions = DataWords.actsOf(suffix1.concat(suffix2));

      SymbolicSuffix symSuffix = symSuffix1.concat(symSuffix2);

      SymbolicSuffix expectedSymSuffix = new SymbolicSuffix(actions, dataValues, freeValues);

      Assert.assertEquals(symSuffix, expectedSymSuffix, "Concatenation result is wrong");
  }
}
