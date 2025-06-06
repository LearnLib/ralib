/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.theory;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.exception.SULException;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class TestIneqOutputTree extends RaLibTestSuite {

    private final static DataType TYPE = PriorityQueueSUL.DOUBLE_TYPE;
    private final static InputSymbol IN = new InputSymbol("in", TYPE);
    private final static OutputSymbol OUT = new OutputSymbol("out", TYPE);

    private static class BiggerSUL extends DataWordSUL {

        @Override
        public void pre() {
        }

        @Override
        public void post() {
        }

        @Override
        public PSymbolInstance step(PSymbolInstance i) throws SULException {
            return new PSymbolInstance(OUT, new DataValue(TYPE,
                    i.getParameterValues()[0].getValue().add(BigDecimal.ONE)));
        }

    }

    @Test(enabled = false)
    public void testIneqEqTree() {
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(TYPE, new DoubleInequalityTheory(TYPE));

        BiggerSUL sul = new BiggerSUL();
        ConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                sul, PriorityQueueSUL.ERROR, teachers,
                new Constants(), solver, IN);

        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(IN, new DataValue(TYPE,  BigDecimal.valueOf(1.0))));

        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(OUT, new DataValue(TYPE,  BigDecimal.valueOf(1.0))));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        SDT res = mto.treeQuery(prefix, symSuffix);
        SDT sdt = res;

        final String expectedTree = "[r1]-+\n" +
"    []-(s1<r1)\n" +
"     |    [Leaf-]\n" +
"     +-(s1=r1)\n" +
"     |    [Leaf-]\n" +
"     +-(s1>r1)\n" +
"          [Leaf+]\n";

        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedTree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);
    }

}
