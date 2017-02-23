package de.learnlib.ralib.theory.succ;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.example.succ.ModerateFreshTCPSUL;
import de.learnlib.ralib.example.succ.AbstractTCPExample.Option;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DeterminedDataWordSUL;
import de.learnlib.ralib.sul.ValueCanonizer;
import de.learnlib.ralib.theory.DataRelation;
import static de.learnlib.ralib.theory.DataRelation.*;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class TestGeneralizedSuffix extends RaLibTestSuite{
	
	private SDT rejSdt(DataType type, int depth, int maxDepth) {
		if (depth > maxDepth) {
			return null;
		}
		if (depth == maxDepth) {
			return SDTLeaf.REJECTING;
		} else {
			LinkedHashMap<SDTGuard, SDT> children = new LinkedHashMap<SDTGuard, SDT>();
			children.put(new SDTTrueGuard(new SymbolicDataValue.SuffixValue(type, depth)), rejSdt(type, depth+1, maxDepth));
			return new SDT(children);
		}
	}
	
    @Test
    public void testModerateFreshTCPSymbolicSuffix2() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue<Double> inc = new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, 1.0);
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, win)};
        SumCDoubleInequalityTheory sumCTheory = new SumCDoubleInequalityTheory(ModerateFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(ModerateFreshTCPSUL.DOUBLE_TYPE, 
               sumCTheory);

        ModerateFreshTCPSUL sul = new ModerateFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers, consts), sul), ModerateFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(ModerateFreshTCPSUL.DOUBLE_TYPE);
        //I_IConnect[] O_IConnect[100001.0[DOUBLE]] I_ISYN[201002.0[DOUBLE], 302003.0[DOUBLE]] FALSE[] I_IACK[403004.0[DOUBLE], 302003.0[DOUBLE] + 1.0[DOUBLE]] 
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
                		b.fv(100001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
                		b.fv(201002.0),
                		b.fv(302003.0))
        				);
        
        
        final Word<PSymbolInstance> longSuffix = Word.fromSymbols(
                new PSymbolInstance(ModerateFreshTCPSUL.NOK),
                new PSymbolInstance(ModerateFreshTCPSUL.IACK, 
                		b.dv(100001.0),
                		b.dv(100001.0)), 
                new PSymbolInstance(ModerateFreshTCPSUL.NOK),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
                		b.fv(201002.0),
                		b.fv(302003.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.OK),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYNACK, 
                		b.dv(201002.0),
                		b.dv(100001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.OK),
                new PSymbolInstance(ModerateFreshTCPSUL.IACK, 
                		b.dv(100001.0),
                		b.dv(100001.0)), 
                new PSymbolInstance(ModerateFreshTCPSUL.OK));
        
        EnumSet<DataRelation> na = EnumSet.noneOf(DataRelation.class);
        EnumSet<DataRelation> [] prefRel = new EnumSet [] {
        		EnumSet.noneOf(DataRelation.class),
        		EnumSet.noneOf(DataRelation.class),//EnumSet.of(EQ_SUMC1),
        		EnumSet.of(EQ, DEQ),
        		EnumSet.noneOf(DataRelation.class),
        		EnumSet.noneOf(DataRelation.class),//EnumSet.of(EQ),
        		EnumSet.noneOf(DataRelation.class),//EnumSet.of(EQ_SUMC1, DEQ_SUMC1),
        		EnumSet.noneOf(DataRelation.class),
        		EnumSet.noneOf(DataRelation.class),
        }; 
        
        EnumSet<DataRelation> [][] suffRel = new EnumSet [][]{
        	new EnumSet []{},
        	new EnumSet []{na},
        	new EnumSet []{na, na},
        	new EnumSet []{na, na, na},
        	new EnumSet []{na, na, na, na},
        	new EnumSet []{na, na, EnumSet.of(EQ_SUMC1), na, na},
        	new EnumSet []{na, na, na, na, na, EnumSet.of(EQ)},
        	new EnumSet []{na, EnumSet.of(EQ), na, na, EnumSet.of(EQ_SUMC1), na, na}
        };
        
        // create a symbolic suffix from the concrete suffix
        final GeneralizedSymbolicSuffix symSuffix =// GeneralizedSymbolicSuffix.fullSuffix(prefix, longsuffix, consts, teachers); 
        		new GeneralizedSymbolicSuffix(DataWords.actsOf(longSuffix), prefRel, suffRel, null);
        		//GeneralizedSymbolicSuffix.fullSuffix(prefix, longSuffix, consts, teachers);

        
        		//GeneralizedSymbolicSuffix.fullSuffix(prefix, longsuffix, consts, teachers);
        System.out.println(symSuffix);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        System.out.println(sdt);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
    }
	
//    @Test
    public void testModerateFreshTCPSymbolicSuffix() {
    	
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        DataValue [] sumConsts = new DataValue [] {
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, 1.0), // for successor
				new DataValue<Double>(ModerateFreshTCPSUL.DOUBLE_TYPE, win)};
        SumCDoubleInequalityTheory sumCTheory = new SumCDoubleInequalityTheory(ModerateFreshTCPSUL.DOUBLE_TYPE,
        		Arrays.asList(sumConsts), // for window size
        		Collections.emptyList());
        sumCTheory.setCheckForFreshOutputs(true);
        teachers.put(ModerateFreshTCPSUL.DOUBLE_TYPE, 
               sumCTheory);

        ModerateFreshTCPSUL sul = new ModerateFreshTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();  
        Constants consts = new Constants(new SumConstants(sumConsts));
        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                new DeterminedDataWordSUL(() -> ValueCanonizer.buildNew(teachers, consts), sul), ModerateFreshTCPSUL.ERROR, teachers, 
                consts, jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(ModerateFreshTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> prefix = //Word.epsilon(); 
        		Word.fromSymbols(
                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
                		b.fv(100001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
                		b.dv(100001.0),
                		b.fv(201002.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.OK)
//                ,
//                new PSymbolInstance(ModerateFreshTCPSUL.IACK, 
//                		b.dv(100001.0),
//                		b.dv(100001.0)), 
//                new PSymbolInstance(ModerateFreshTCPSUL.NOK),
//                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
//                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
//                		b.fv(302003.0))
        				);
        
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(ModerateFreshTCPSUL.IACK, 
                		b.dv(100001.0),
                		b.dv(100001.0)), 
                new PSymbolInstance(ModerateFreshTCPSUL.NOK),
                new PSymbolInstance(ModerateFreshTCPSUL.ICONNECT),
                new PSymbolInstance(ModerateFreshTCPSUL.OCONNECT,
                		b.fv(302003.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYNACK, 
                		b.dv(201002.0),
                		b.dv(100001.0)),
                new PSymbolInstance(ModerateFreshTCPSUL.NOK),
                new PSymbolInstance(ModerateFreshTCPSUL.ISYN, 
                		b.dv(302003.0),
                		b.sumcv(201002.0, win)),
                new PSymbolInstance(ModerateFreshTCPSUL.NOK));
        
        
        
        // create a symbolic suffix from the concrete suffix
        final GeneralizedSymbolicSuffix symSuffix =// GeneralizedSymbolicSuffix.fullSuffix(prefix, longsuffix, consts, teachers); 
        		new GeneralizedSymbolicSuffix(prefix, longsuffix, consts, teachers); 
//        symSuffix.getPrefixRelations(1).clear();
//        symSuffix.getPrefixRelations(1).addAll(EnumSet.of(DataRelation.EQ_SUMC1, DataRelation.DEQ_SUMC1));
//        symSuffix.getPrefixRelations(2).clear();
//        symSuffix.getPrefixRelations(2).addAll(EnumSet.of(DataRelation.EQ_SUMC1, DataRelation.DEQ_SUMC1));
//        symSuffix.getPrefixRelations(3).clear();
//        symSuffix.getPrefixRelations(4).clear();
//        symSuffix.getPrefixRelations(5).clear();
        //symSuffix.getPrefixRelations(5).addAll(EnumSet.of(DataRelation.EQ_SUMC1, DataRelation.DEQ_SUMC1));

        
        		//GeneralizedSymbolicSuffix.fullSuffix(prefix, longsuffix, consts, teachers);
        System.out.println(symSuffix);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        System.out.println(sdt);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());
        
        
        
        SDT hypSdt = rejSdt(ModerateFreshTCPSUL.DOUBLE_TYPE, 1, 8);
        MultiTheorySDTLogicOracle oracle = new MultiTheorySDTLogicOracle(consts, jsolv);
        GeneralizedSymbolicSuffix reducedSuff = oracle.suffixForCounterexample(prefix, hypSdt, new PIV(), sdt, res.getPiv(), new TransitionGuard(new TrueGuardExpression()), DataWords.actsOf(longsuffix));
 //       reducedSuff.getPrefixRelations(1).add(DataRelation.DEQ_SUMC1);
//        reducedSuff.getPrefixRelations(2).add(DataRelation.DEQ_SUMC1);
        reducedSuff.getPrefixRelations(2).add(DataRelation.EQ_SUMC1);
        reducedSuff.getSuffixRelations(1,5).addAll(EnumSet.of(DataRelation.EQ, DataRelation.DEQ));
        reducedSuff.getSuffixRelations(2,5).addAll(EnumSet.of(DataRelation.EQ, DataRelation.DEQ));
        System.out.println(reducedSuff);
        
        TreeQueryResult resRed = mto.treeQuery(prefix, reducedSuff);
        SymbolicDecisionTree sdtRed = resRed.getSdt();

        System.out.println(sdtRed);
        System.out.println("inputs: " + sul.getInputs() + " resets: " + sul.getResets());

        
        //Assert.assertEquals(((SDT)sdt).getNumberOfLeaves() , 4);
    }
}
