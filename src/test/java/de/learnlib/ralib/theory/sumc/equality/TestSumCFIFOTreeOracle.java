package de.learnlib.ralib.theory.sumc.equality;

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
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.sumc.equality.SumCFIFOSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerSumCEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TestSumCFIFOTreeOracle extends RaLibTestSuite{
	

    @Test
    public void testSumCFIFOTree() {
    	int capacity = 3;
    	int sumc = 1;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Integer>(SumCFIFOSUL.INT_TYPE, sumc), 
				};
        
        IntegerSumCEqualityTheory theory = new IntegerSumCEqualityTheory();
        teachers.put(SumCFIFOSUL.INT_TYPE, theory);
        theory.setCheckForFreshOutputs(true);
        theory.setType(SumCFIFOSUL.INT_TYPE);
        SumCFIFOSUL sul = new SumCFIFOSUL(3, sumc);
        
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        theory.setConstants(consts);
        MultiTheoryTreeOracle mto = TestUtil.createBasicMTO(
                sul, SumCFIFOSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
             new PSymbolInstance(SumCFIFOSUL.OFFER,
                     new DataValue(SumCFIFOSUL.INT_TYPE, 1)
            		 ), 
             new PSymbolInstance(SumCFIFOSUL.OK),
             new PSymbolInstance(SumCFIFOSUL.OFFER,
                     new DataValue(SumCFIFOSUL.INT_TYPE, 2)
            		 ), 
             new PSymbolInstance(SumCFIFOSUL.OK)
        		);
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
        		 new PSymbolInstance(SumCFIFOSUL.OFFER,
        				 new DataValue(SumCFIFOSUL.INT_TYPE, 3)
        				 ),
        		 new PSymbolInstance(SumCFIFOSUL.OK),
        		 new PSymbolInstance(SumCFIFOSUL.POLL),
        		 new PSymbolInstance(SumCFIFOSUL.OUTPUT,
        				 new DataValue(SumCFIFOSUL.INT_TYPE, 1)
        				 ),
        		 new PSymbolInstance(SumCFIFOSUL.POLL),
        		 new PSymbolInstance(SumCFIFOSUL.OUTPUT,
        				 new DataValue(SumCFIFOSUL.INT_TYPE, 2)
        				 )
        		);
        
        final GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix( prefix, suffix, consts, teachers);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();
        
        String expectedTree = "[r1]-+\n" +
        		"    []-TRUE: s1\n" +
        		"          []-(s2=r1 + 1)\n" +
        		"           |    []-(s3=r1)\n" +
        		"           |     |    [Leaf+]\n" +
        		"           |     +-(s3!=r1)\n" +
        		"           |          [Leaf-]\n" +
        		"           +-(s2!=r1 + 1)\n" +
        		"                []-TRUE: s3\n" +
        		"                      [Leaf-]\n";
        
        Assert.assertEquals(sdt.toString(), expectedTree);
    }

}
