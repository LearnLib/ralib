package de.learnlib.ralib.learning;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import net.automatalib.serialization.xml.ra.RegisterAutomatonImporter;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.words.DataWords;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.symbol.PSymbolInstance;
import net.automatalib.symbol.ParameterizedSymbol;
import net.automatalib.word.Word;

public class SymbolicSuffixTest extends RaLibTestSuite {

  @Test
  public void concatTest() {

      RegisterAutomatonImporter loader = TestUtil.getLoader(
              "/de/learnlib/ralib/automata/xml/fifo7.xml");

      //RegisterAutomaton model = loader.getRegisterAutomaton();
      //logger.log(Level.FINE, "SYS: {0}", model);

      Constants consts = loader.getConstants();

      DataType intType = TestUtil.getType("int", loader.getDataTypes());

      ParameterizedSymbol iput = new InputSymbol(
              "IPut", new DataType[] {intType});

      ParameterizedSymbol iget = new InputSymbol(
              "IGet", new DataType[] {});

      ParameterizedSymbol oget = new OutputSymbol(
              "OGet", new DataType[] {intType});

      ParameterizedSymbol ook = new OutputSymbol(
              "OOK", new DataType[] {});

      DataValue d0 = new DataValue(intType, 0);
      DataValue d1 = new DataValue(intType, 1);
      DataValue d6 = new DataValue(intType, 6);

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

      SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix1, suffix1, consts);
      SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix2, suffix2, consts);

      LinkedHashMap<Integer, SuffixValue> dataValues = new LinkedHashMap<Integer, SuffixValue>();
      for (int i=1; i<=5; i++) {
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
