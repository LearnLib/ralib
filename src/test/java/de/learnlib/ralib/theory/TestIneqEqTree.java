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

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class TestIneqEqTree extends RaLibTestSuite {

    @Test
    public void testIneqEqTree() {

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(PriorityQueueSUL.DOUBLE_TYPE,
                new DoubleInequalityTheory(PriorityQueueSUL.DOUBLE_TYPE));

        PriorityQueueSUL sul = new PriorityQueueSUL();
        ConstraintSolver solver = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                sul, PriorityQueueSUL.ERROR, teachers,
                new Constants(), solver,
                sul.getInputSymbols());

        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE, BigDecimal.valueOf(4.0))),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE,  BigDecimal.valueOf(5.0))),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.POLL),
                new PSymbolInstance(PriorityQueueSUL.OUTPUT,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE,  BigDecimal.valueOf(6.0))));

        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE,  BigDecimal.valueOf(1.0))),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE,  BigDecimal.valueOf(2.0))),
                new PSymbolInstance(PriorityQueueSUL.OK),
                new PSymbolInstance(PriorityQueueSUL.OFFER,
                        new DataValue(PriorityQueueSUL.DOUBLE_TYPE,  BigDecimal.valueOf(3.0))),
                new PSymbolInstance(PriorityQueueSUL.OK));

        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);

        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SDT sdt = res.sdt();

        final String expectedTree = "[r1, r2]-+\n" +
                "        []-(s1=1.0[DOUBLE])\n" +
                "         |    []-(s2>2.0[DOUBLE])\n" +
                "         |     |    []-(s3=2.0[DOUBLE])\n" +
                "         |     |     |    [Leaf+]\n" +
                "         |     |     +-(s3!=2.0[DOUBLE])\n" +
                "         |     |          [Leaf-]\n" +
                "         |     +-(s2<=2.0[DOUBLE])\n" +
                "         |          []-(s3=s2)\n" +
                "         |           |    [Leaf+]\n" +
                "         |           +-(s3!=s2)\n" +
                "         |                [Leaf-]\n" +
                "         +-(s1!=1.0[DOUBLE])\n" +
                "              []-TRUE: s2\n" +
                "                    []-TRUE: s3\n" +
                "                          [Leaf-]\n";

        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedTree);

        Branching b = mto.getInitialBranching(prefix, PriorityQueueSUL.OFFER, sdt);

        Assert.assertEquals(b.getBranches().size(), 2);
        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }

}
