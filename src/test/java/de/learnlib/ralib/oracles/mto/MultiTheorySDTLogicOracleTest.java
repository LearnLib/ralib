package de.learnlib.ralib.oracles.mto;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.smt.ConstraintSolverFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

public class MultiTheorySDTLogicOracleTest {

  @Test
  public void acceptsTest() {
      Constants consts = new Constants();
      RegisterAutomaton sul = AUTOMATON;

      final Map<DataType, Theory> teachers = new LinkedHashMap<>();
      teachers.put(T_UID, new IntegerEqualityTheory(T_UID));
      teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));

      ConstraintSolver solver = ConstraintSolverFactory.createZ3ConstraintSolver();

      SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts, solver);

      TreeOracleFactory hypFactory = (RegisterAutomaton hyp) ->
              new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers,
                      new Constants(), solver);
      TreeOracle sulTreeOracle = hypFactory.createTreeOracle(sul);
      Word<PSymbolInstance> acceptedWord = Word.fromSymbols(
              new PSymbolInstance(I_REGISTER, new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)),
              new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)),
              new PSymbolInstance(I_LOGOUT),
              new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)));
      Word<PSymbolInstance> prefix = acceptedWord.prefix(1);
      Word<PSymbolInstance> suffix = acceptedWord.suffix(acceptedWord.length() - prefix.length());
      SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
      TreeQueryResult query = sulTreeOracle.treeQuery(prefix, symSuffix);
      Assert.assertTrue(slo.accepts(acceptedWord, prefix, query.getSdt(), query.getPiv()));
      List<Word<PSymbolInstance>> rejectedSuffixes = Arrays.asList(
              Word.fromSymbols(
                      new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ONE), new DataValue(T_PWD, BigDecimal.ZERO)),
                      new PSymbolInstance(I_LOGOUT),
                      new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ONE))),
              Word.fromSymbols(
                      new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ZERO)),
                      new PSymbolInstance(I_LOGOUT),
                      new PSymbolInstance(I_LOGIN, new DataValue(T_UID, BigDecimal.ZERO), new DataValue(T_PWD, BigDecimal.ONE))));

      for (Word<PSymbolInstance> rejectedSuffix : rejectedSuffixes) {
          Word<PSymbolInstance> rejectedWord = prefix.concat(rejectedSuffix);
          Assert.assertFalse(slo.accepts(rejectedWord, prefix, query.getSdt(), query.getPiv()));
      }
  }
}
