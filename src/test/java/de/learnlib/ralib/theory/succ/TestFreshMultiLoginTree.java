package de.learnlib.ralib.theory.succ;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.example.login.FreshMultiLoginSUL;
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
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.SumCIntegerInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class TestFreshMultiLoginTree extends RaLibTestSuite{

    public void testFreshMultiLoginSystem() {
    	FreshMultiLoginProvider sulProvider = new FreshMultiLoginProvider();
        DataValueConstructor<Integer> b = new DataValueConstructor<>(FreshMultiLoginSUL.INT_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, b.dv(100)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER, b.fv(200)),
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, 
                		b.dv(200)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER,
                		b.fv(300)),
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(200), b.dv(300)),
                new PSymbolInstance(FreshMultiLoginSUL.OK));
        
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(100),
                		b.dv(100)), 
                new PSymbolInstance(FreshMultiLoginSUL.NOK),
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, 
                		b.dv(300)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER,
                		b.fv(400)),
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(300),
                		b.dv(400)), 
                new PSymbolInstance(FreshMultiLoginSUL.NOK));
        
        GeneralizedSymbolicSuffix gsuff = testTreeQuery(sulProvider, prefix, longsuffix);
        
    }
    
    public void testFreshMultiLoginSystem2() {
    	FreshMultiLoginProvider sulProvider = new FreshMultiLoginProvider();
        DataValueConstructor<Integer> b = new DataValueConstructor<>(FreshMultiLoginSUL.INT_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, b.dv(100)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER, b.fv(200)),
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(300), b.dv(400)));
        
        final Word<ParameterizedSymbol> actions = Word.fromSymbols(FreshMultiLoginSUL.OREGISTER, FreshMultiLoginSUL.IREGISTER, FreshMultiLoginSUL.OREGISTER, FreshMultiLoginSUL.IREGISTER, FreshMultiLoginSUL.OREGISTER);
        GeneralizedSymbolicSuffix gsuff = new GeneralizedSymbolicSuffix(actions, new EnumSet[]{EnumSet.of(DataRelation.DEFAULT),
        		EnumSet.of(DataRelation.EQ, DataRelation.DEQ), EnumSet.noneOf(DataRelation.class), EnumSet.of(DataRelation.EQ, DataRelation.DEQ), EnumSet.noneOf(DataRelation.class)		
        }, new EnumSet[][]{{},{EnumSet.noneOf(DataRelation.class)}, {EnumSet.noneOf(DataRelation.class),EnumSet.noneOf(DataRelation.class)}, 
        	{EnumSet.noneOf(DataRelation.class),EnumSet.noneOf(DataRelation.class),EnumSet.noneOf(DataRelation.class)},
        	{EnumSet.noneOf(DataRelation.class),EnumSet.noneOf(DataRelation.class),EnumSet.noneOf(DataRelation.class),EnumSet.noneOf(DataRelation.class)}});
        sulProvider.mto.treeQuery(prefix, gsuff);
    }
    
    public void testSuffixBuilder() {
       	FreshMultiLoginProvider sulProvider = new FreshMultiLoginProvider();
        DataValueConstructor<Integer> b = new DataValueConstructor<>(FreshMultiLoginSUL.INT_TYPE);
        
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, b.dv(100)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER, b.fv(200)),
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, b.dv(200)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER, b.fv(300)),
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(200), b.dv(300)),
                new PSymbolInstance(FreshMultiLoginSUL.OK));
    	
        final Word<PSymbolInstance> suffix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(100), b.dv(100)),
                new PSymbolInstance(FreshMultiLoginSUL.NOK),
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, b.dv(300)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER, b.fv(400)),
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(300), b.dv(400)) );
        SliceBuilder builder = sulProvider.buildSliceBuilder();
        Slice slice = builder.sliceFromWord(prefix, suffix);
        System.out.println(slice);
    }
    
    public void testFreshMultiLoginSystem3() {
    	FreshMultiLoginProvider sulProvider = new FreshMultiLoginProvider();
    	sulProvider.useOptimizedSuffix();
        DataValueConstructor<Integer> b = new DataValueConstructor<>(FreshMultiLoginSUL.INT_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(FreshMultiLoginSUL.IREGISTER, b.dv(100)),
                new PSymbolInstance(FreshMultiLoginSUL.OREGISTER, b.fv(200)),
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(100), b.dv(200)),
                new PSymbolInstance(FreshMultiLoginSUL.OK),
                new PSymbolInstance(FreshMultiLoginSUL.ILOGIN, 
                		b.dv(300), b.dv(400)) );

        
        final Word<ParameterizedSymbol> actions = Word.fromSymbols(FreshMultiLoginSUL.NOK, FreshMultiLoginSUL.IREGISTER, FreshMultiLoginSUL.OREGISTER, FreshMultiLoginSUL.ILOGIN, FreshMultiLoginSUL.NOK);
        GeneralizedSymbolicSuffix gsuff = new GeneralizedSymbolicSuffix(actions, 
        		new EnumSet[]{EnumSet.of(DataRelation.EQ, DataRelation.DEQ), EnumSet.noneOf(DataRelation.class), EnumSet.of(DataRelation.EQ, DataRelation.DEQ), EnumSet.noneOf(DataRelation.class)}, 
        		new EnumSet[][]{{},{EnumSet.noneOf(DataRelation.class)}, {EnumSet.noneOf(DataRelation.class),EnumSet.noneOf(DataRelation.class)}, 
        	{EnumSet.noneOf(DataRelation.class),EnumSet.of(DataRelation.EQ),EnumSet.noneOf(DataRelation.class)}});
        
//        GeneralizedSymbolicSuffix gsuff = new GeneralizedSymbolicSuffix(actions, 
//        		new EnumSet[]{EnumSet.of(DataRelation.EQ, DataRelation.DEQ), EnumSet.noneOf(DataRelation.class), EnumSet.noneOf(DataRelation.class), EnumSet.noneOf(DataRelation.class)}, 
//        		new EnumSet[][]{{},{EnumSet.noneOf(DataRelation.class)}, {EnumSet.of(DataRelation.EQ, DataRelation.DEQ),EnumSet.noneOf(DataRelation.class)}, 
//        	{EnumSet.noneOf(DataRelation.class),EnumSet.of(DataRelation.EQ),EnumSet.noneOf(DataRelation.class)}});
        
        TreeQueryResult tree = sulProvider.mto.treeQuery(prefix, gsuff);
        System.out.println(tree.getSdt());
    }
    
    
    @SafeVarargs
	public final GeneralizedSymbolicSuffix testTreeQuery(FreshMultiLoginProvider sulProvider, Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, Consumer<SDT>... assertions) {
        MultiTheoryTreeOracle mto = sulProvider.mto;
        GeneralizedSymbolicSuffix symSuffix = sulProvider.buildExhaustiveSuffix(prefix, suffix);
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

       // System.out.println(((SDT)sdt).getVariables());
        System.out.println("inputs: " + sulProvider.sul.getInputs() + " resets: " + sulProvider.sul.getResets());
        System.out.println("Result: " + sdt.toString());
        
        for (Consumer<SDT> assertion : assertions) {
        	assertion.accept((SDT)sdt);
        }
        return symSuffix;
	}
    
    public final void testTreeQuery(FreshMultiLoginProvider sulProvider, Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix symSuffix, Consumer<SDT>... assertions) {
    	  TreeQueryResult res = sulProvider.mto.treeQuery(prefix, symSuffix);
          SymbolicDecisionTree sdt = res.getSdt();

         // System.out.println(((SDT)sdt).getVariables());
          System.out.println("inputs: " + sulProvider.sul.getInputs() + " resets: " + sulProvider.sul.getResets());
          System.out.println("Result: " + sdt.toString());
          
          for (Consumer<SDT> assertion : assertions) {
          	assertion.accept((SDT)sdt);
          }
    }
    
    public static void main(String args []) {
    	new TestFreshMultiLoginTree().testSuffixBuilder();
    }
    
    class FreshMultiLoginProvider {
    	private final DataWordSUL sul;
    	private final MultiTheoryTreeOracle mto;
    	private final Map<DataType, Theory> teachers;
		private final Constants consts;
		private final JConstraintsConstraintSolver consSolver;
    	public FreshMultiLoginProvider() {
    		this.teachers = new LinkedHashMap<>();
	        DataValue [] sumConsts = new DataValue [] {};
	        SumCIntegerInequalityTheory sumCTheory = new SumCIntegerInequalityTheory(FreshMultiLoginSUL.INT_TYPE,
	        		Arrays.asList(sumConsts), // for window size
	        		Collections.emptyList());
	        sumCTheory.setCheckForFreshOutputs(true);
	        teachers.put(FreshMultiLoginSUL.INT_TYPE, 
	               sumCTheory);

	        FreshMultiLoginSUL sul = new FreshMultiLoginSUL();
	        this.consSolver= TestUtil.getZ3Solver();  
	        this.consts = new Constants(new SumConstants(sumConsts));
	        
	        this.sul= new DeterminedDataWordSUL(() -> MultiTheoryDeterminizer.buildNew(teachers, consts), sul);
	        
	        this.mto = TestUtil.createMTOWithFreshValueSupport(
	                new DeterminedDataWordSUL(() -> MultiTheoryDeterminizer.buildNew(teachers, consts), sul), FreshMultiLoginSUL.ERROR, teachers, 
	                consts, consSolver, 
	                sul.getInputSymbols());
    	}
    	
    	private void useOptimizedSuffix() {
    		((TypedTheory)this.teachers.get(FreshMultiLoginSUL.INT_TYPE)).setUseSuffixOpt(true);
    	}
    	
    	private DataWordSUL getSUL() {
    		return this.sul;
    	}
    	
    	private MultiTheoryTreeOracle getMTO() {
    		return this.mto;
    	}
    	
    	private  Map<DataType, Theory> getTeachers() {
    		return this.teachers;
    	}
    	
    	private SliceBuilder buildSliceBuilder() {
    		return new SliceBuilder(this.teachers, this.consts, this.consSolver);
    	}
    	
    	private GeneralizedSymbolicSuffix buildExhaustiveSuffix(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix) {
            SliceBuilder sb = this.buildSliceBuilder();
            // create a symbolic suffix from the concrete suffix
            Slice slice = sb.sliceFromWord(prefix, suffix);
            System.out.println("Slice from word: " + slice);
            GeneralizedSymbolicSuffix symSuffix = SymbolicSuffixBuilder.suffixFromSlice(DataWords.actsOf(suffix), slice);
            return  symSuffix;
    	}
    }
    
}
