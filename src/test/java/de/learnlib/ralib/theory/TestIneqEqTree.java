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

import de.learnlib.ralib.smt.ConstraintSolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
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
        ConstraintSolver jsolv = TestUtil.getZ3Solver();
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                sul, PriorityQueueSUL.ERROR, teachers,
                new Constants(), jsolv,
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
        SymbolicDecisionTree sdt = res.getSdt();

        final String expectedTree = "[r2, r1]-+\n" +
"        []-(s1!=r2)\n" +
"         |    []-TRUE: s2\n" +
"         |          []-TRUE: s3\n" +
"         |                [Leaf-]\n" +
"         +-(s1=r2)\n" +
"              []-(s2=r1)\n" +
"               |    []-(s3!=s2)\n" +
"               |     |    [Leaf-]\n" +
"               |     +-(s3=s2)\n" +
"               |          [Leaf+]\n" +
"               +-(s2<r1)\n" +
"               |    []-(s3!=s2)\n" +
"               |     |    [Leaf-]\n" +
"               |     +-(s3=s2)\n" +
"               |          [Leaf+]\n" +
"               +-(s2>r1)\n" +
"                    []-(s3!=r1)\n" +
"                     |    [Leaf-]\n" +
"                     +-(s3=r1)\n" +
"                          [Leaf+]\n";

        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedTree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);

        Parameter p1 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 1);
        Parameter p2 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 2);
        Parameter p3 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 3);

        PIV testPiv = new PIV();
        testPiv.put(p1, new Register(PriorityQueueSUL.DOUBLE_TYPE, 1));
        testPiv.put(p2, new Register(PriorityQueueSUL.DOUBLE_TYPE, 2));
        testPiv.put(p3, new Register(PriorityQueueSUL.DOUBLE_TYPE, 3));

        Branching b = mto.getInitialBranching(
                prefix, PriorityQueueSUL.OFFER, testPiv, sdt);

        Assert.assertEquals(b.getBranches().size(), 2);
        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }


}
