package de.learnlib.ralib.theory.succ;

import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.testng.Assert;

import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.succ.IntAbstractTCPExample.Option;
import de.learnlib.ralib.example.succ.IntHardFreshTCPSUL;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.mapper.MultiTheoryDeterminizer;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.Slice;
import de.learnlib.ralib.oracles.mto.SliceBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixBuilder;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCIntegerInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TestIntHardTCPTree {

	public void testHardFreshTCPTree() {
        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntHardFreshTCPSUL.INT_TYPE);
		 final Word<PSymbolInstance> prefix = Word.fromSymbols(
	                new PSymbolInstance(IntHardFreshTCPSUL.ISYN, 
	                		b.fv(100),
	                		b.fv(200)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OSYNACK,
	                		b.fv(300),
	                		b.sumcv(100, 1)),
	                new PSymbolInstance(IntHardFreshTCPSUL.IACK, 
	                		b.sumcv(100, 1),
	                		b.sumcv(300, 1))
				 );
	        
	        
	        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
	                new PSymbolInstance(IntHardFreshTCPSUL.OTIMEOUT),
	                new PSymbolInstance(IntHardFreshTCPSUL.IACKPSH, 
	                		b.sumcv(100, 1),
	                		b.sumcv(300, 1)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OACK,
	                		b.sumcv(300, 1),
	                		b.sumcv(100, 1, 1)));
	       
	        testTreeQuery(prefix, longsuffix);
//	        testTreeQuery(prefix, (teach, cons) ->  GeneralizedSymbolicSuffix.fullSuffix(prefix, longsuffix, cons, teach)
//	        		, sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 7) ); 
	        		//sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6));
	  //      Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6);
	    }
	
	public void testHardFreshTCPTree2() {
        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntHardFreshTCPSUL.INT_TYPE);
		 final Word<PSymbolInstance> prefix = Word.fromSymbols(
	                new PSymbolInstance(IntHardFreshTCPSUL.ISYN, 
	                		b.fv(100),
	                		b.fv(200)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OSYNACK,
	                		b.fv(300),
	                		b.sumcv(100, 1)),
	                new PSymbolInstance(IntHardFreshTCPSUL.IACK, 
	                		b.sumcv(100, 1),
	                		b.sumcv(300, 1))
				 );
	        
	        
	        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
	        		new PSymbolInstance(IntHardFreshTCPSUL.OTIMEOUT),
	        		new PSymbolInstance(IntHardFreshTCPSUL.IACK, 
	                		b.sumcv(100, 1),
	                		b.sumcv(300, 1)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OTIMEOUT),
	                new PSymbolInstance(IntHardFreshTCPSUL.IACKPSH, 
	                		b.sumcv(100, 1),
	                		b.sumcv(300, 1)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OACK,
	                		b.sumcv(300, 1),
	                		b.sumcv(100, 1, 1)));
	       
	        testTreeQuery(prefix, longsuffix);
//	        testTreeQuery(prefix, (teach, cons) ->  GeneralizedSymbolicSuffix.fullSuffix(prefix, longsuffix, cons, teach)
//	        		, sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 7) ); 
	        		//sdt -> Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6));
	  //      Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 6);
	    }
	

	public void testHardFreshTCPTree3() {
        DataValueConstructor<Integer> b = new DataValueConstructor<>(IntHardFreshTCPSUL.INT_TYPE);
		 final Word<PSymbolInstance> prefix = Word.fromSymbols(
	                new PSymbolInstance(IntHardFreshTCPSUL.ISYN, 
	                		b.fv(100),
	                		b.fv(200)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OSYNACK,
	                		b.fv(300),
	                		b.sumcv(100, 1))
				 );
	        
	        //IACK[10001[int], 30000[int] + 1[int]] OTIMEOUT[] IFINACK[10001[int], 30001[int]] OACK[30000[int], 10001[int]]
	        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
	        		new PSymbolInstance(IntHardFreshTCPSUL.IACK, 
	                		b.sumcv(100, 1),
	                		b.sumcv(300, 1)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OTIMEOUT),
	                new PSymbolInstance(IntHardFreshTCPSUL.IFINACK, 
	                		b.sumcv(100, 1),
	                		b.sumcv(300, 1)),
	                new PSymbolInstance(IntHardFreshTCPSUL.OACK,
	                		b.dv(300),
	                		b.sumcv(100, 1)));
	       
	        testTreeQuery(prefix, longsuffix);
	    }
	
	
	@SafeVarargs
	public final void testTreeQuery(Word<PSymbolInstance> prefix, BiFunction<Map<DataType, Theory>, Constants, GeneralizedSymbolicSuffix> symSuffProvider, Consumer<SDT>... assertions) {
		Integer win = 100;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Integer>(IntHardFreshTCPSUL.INT_TYPE, 1)};
        SumCIntegerInequalityTheory sumCTheory = new SumCIntegerInequalityTheory(IntHardFreshTCPSUL.INT_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(IntHardFreshTCPSUL.INT_TYPE, 
               sumCTheory);

        IntHardFreshTCPSUL sul = new IntHardFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                new DeterminedDataWordSUL(() -> MultiTheoryDeterminizer.buildNew(teachers, consts), sul), IntHardFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        GeneralizedSymbolicSuffix symSuffix = symSuffProvider.apply(teachers, consts);
        System.out.println(symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

       // System.out.println(((SDT)sdt).getVariables());
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        System.out.println("Result: " + sdt.toString());
        
        for (Consumer<SDT> assertion : assertions) {
        	assertion.accept((SDT)sdt);
        }
	}
	

	@SafeVarargs
	public final void testTreeQuery(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Consumer<SDT>... assertions) {
		Integer win = 100;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Integer>(IntHardFreshTCPSUL.INT_TYPE, 1)};
        SumCIntegerInequalityTheory sumCTheory = new SumCIntegerInequalityTheory(IntHardFreshTCPSUL.INT_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(IntHardFreshTCPSUL.INT_TYPE, 
               sumCTheory);

        IntHardFreshTCPSUL sul = new IntHardFreshTCPSUL(win);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        
        DeterminedDataWordSUL detSul = new DeterminedDataWordSUL(() -> MultiTheoryDeterminizer.buildNew(teachers, consts), sul);
        detSul.pre();
        Word<PSymbolInstance> run = Word.epsilon();
        for (PSymbolInstance sym : prefix.concat(suffix).stream().filter(s -> (s.getBaseSymbol() instanceof InputSymbol)).collect(Collectors.toList())) {
        	run = run.append(sym).append(detSul.step(sym));
        }
        detSul.post();
        System.out.println("Concrete run of inputs : " + run);
        
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                new DeterminedDataWordSUL(() -> MultiTheoryDeterminizer.buildNew(teachers, consts), sul), IntHardFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        SliceBuilder sb = new SliceBuilder(teachers, consts, jsolv);
        // create a symbolic suffix from the concrete suffix
        Slice slice = sb.sliceFromWord(prefix, suffix);
        System.out.println("Slice from word: " + slice);
        GeneralizedSymbolicSuffix symSuffix = SymbolicSuffixBuilder.suffixFromSlice(DataWords.actsOf(suffix), slice);
//        GeneralizedSymbolicSuffix symSuffix = 
//                SymbolicSuffixBuilder.suffixFromSlice(DataWords.actsOf(suffix), slice);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

       // System.out.println(((SDT)sdt).getVariables());
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        System.out.println("Result: " + sdt.toString());
        
        for (Consumer<SDT> assertion : assertions) {
        	assertion.accept((SDT)sdt);
        }
	}
	

    public static void main(String args []) {
    	TestIntHardTCPTree testSuite = new TestIntHardTCPTree();
    	testSuite.testHardFreshTCPTree3();
    }
}
