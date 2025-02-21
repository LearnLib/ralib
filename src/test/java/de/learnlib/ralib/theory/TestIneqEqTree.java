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
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.IntervalGuard;
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

        SuffixValueGenerator sgen = new SuffixValueGenerator();
        RegisterGenerator rgen = new RegisterGenerator();
        ParameterGenerator pgen = new ParameterGenerator();
        SuffixValue s1 = sgen.next(PriorityQueueSUL.DOUBLE_TYPE);
        SuffixValue s2 = sgen.next(PriorityQueueSUL.DOUBLE_TYPE);
        SuffixValue s3 = sgen.next(PriorityQueueSUL.DOUBLE_TYPE);
        Register r1 = rgen.next(PriorityQueueSUL.DOUBLE_TYPE);
        Register r2 = rgen.next(PriorityQueueSUL.DOUBLE_TYPE);
        Parameter p1 = pgen.next(PriorityQueueSUL.DOUBLE_TYPE);
        Parameter p2 = pgen.next(PriorityQueueSUL.DOUBLE_TYPE);
        PIV piv = new PIV();
        piv.put(p1, r1);
        piv.put(p2, r2);
        VarMapping<Register, Register> renaming = new VarMapping<>();
        renaming.put(r1, res.getPiv().get(p1));
        renaming.put(r2, res.getPiv().get(p2));

        SDT expected = new SDT(Map.of(
        		new EqualityGuard(s1,r1), new SDT(Map.of(
        				IntervalGuard.greaterGuard(s2, r2), new SDT(Map.of(
        						new EqualityGuard(s3, r2), SDTLeaf.ACCEPTING,
        						new DisequalityGuard(s3, r2), SDTLeaf.REJECTING)),
        				IntervalGuard.lessOrEqualGuard(s2, r2), new SDT(Map.of(
        						new EqualityGuard(s3, s2), SDTLeaf.ACCEPTING,
        						new DisequalityGuard(s3, s2), SDTLeaf.REJECTING)))),
        		new DisequalityGuard(s1, r1), new SDT(Map.of(
        				new SDTTrueGuard(s2), new SDT(Map.of(
        						new SDTTrueGuard(s3), SDTLeaf.REJECTING))))));

        Assert.assertTrue(sdt.isEquivalent(expected, renaming));

        p1 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 1);
        p2 = new Parameter(PriorityQueueSUL.DOUBLE_TYPE, 2);
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
