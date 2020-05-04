/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.learning;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.POLL;
import static de.learnlib.ralib.example.priority.PriorityQueueOracle.doubleType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.priority.PriorityQueueOracle;
import de.learnlib.ralib.example.sumc.inequality.TwoWayTCPSUL;
import de.learnlib.ralib.example.sumc.inequality.TCPExample.Option;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.DoubleInequalityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.tools.theories.SumCDoubleInequalityTheory;
import de.learnlib.ralib.utils.DataValueConstructor;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class GeneralizedSymbolicSuffixTest {
    
    public void testGeneralizedSymbolicSuffix1() {
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 2),
                    new DataValue(T_PWD, 2)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));        

        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(T_UID, new IntegerEqualityTheory(T_UID));
        theories.put(T_PWD, new IntegerEqualityTheory(T_PWD));
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix, suffix, new Constants(), theories);
        
        System.out.println(symSuffix);
    }
    
    
    public void testGeneralizedSymbolicSuffix2() {
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_PWD, 2),
                    new DataValue(T_PWD, 2)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_PWD, 1),
                    new DataValue(T_PWD, 1)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_PWD, 1),
                    new DataValue(T_PWD, 1)));        

        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(T_PWD, new IntegerEqualityTheory(T_PWD));
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix, suffix, new Constants(), theories);
        
        System.out.println(symSuffix);
    }
    
    

    public void testGeneralizedSymbolicSuffix3() {
    	   Constants consts = new Constants();
           PriorityQueueOracle dwOracle = new PriorityQueueOracle(3);
           
           final Map<DataType, Theory> teachers = new LinkedHashMap<>();
           DoubleInequalityTheory dit = new DoubleInequalityTheory(doubleType);
           dit.setUseSuffixOpt(true);
           teachers.put(doubleType, dit);

           final Word<PSymbolInstance> suffix = Word.fromSymbols(
                   new PSymbolInstance(OFFER,
                           new DataValue(doubleType, 5.0)),
                   new PSymbolInstance(OFFER,
                           new DataValue(doubleType, 5.0)),
                   new PSymbolInstance(POLL,
                           new DataValue(doubleType, 5.0)),
                   new PSymbolInstance(POLL,
                           new DataValue(doubleType, 5.0)));
           final Word<PSymbolInstance> prefix = Word.fromSymbols( new PSymbolInstance(OFFER,
                   new DataValue(doubleType, 10.0)), new PSymbolInstance(OFFER,
                           new DataValue(doubleType, 1.0))); 
        		   
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix, suffix, new Constants(), teachers);
        
        System.out.println("prefix: " + prefix + " suffix: " + suffix);
        System.out.println(symSuffix);
    }
    
    @Test
    public void testGeneralizedSymbolicSuffix4() {
    	Double win = 1000.0;
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(TwoWayTCPSUL.DOUBLE_TYPE, 
                new SumCDoubleInequalityTheory(TwoWayTCPSUL.DOUBLE_TYPE,
                		Arrays.asList(
                				new DataValue<Double>(TwoWayTCPSUL.DOUBLE_TYPE, 1.0), // for successor
                				new DataValue<Double>(TwoWayTCPSUL.DOUBLE_TYPE, win)), // for window size
                		Collections.emptyList()));

        TwoWayTCPSUL sul = new TwoWayTCPSUL(win);
        sul.configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
        JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();        
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(
                sul, TwoWayTCPSUL.ERROR, teachers, 
                new Constants(), jsolv, 
                sul.getInputSymbols());
        DataValueConstructor<Double> b = new DataValueConstructor<>(TwoWayTCPSUL.DOUBLE_TYPE);
                
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSUL.ICONNECT,
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 100.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK));
        
        final Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(TwoWayTCPSUL.ISYN, 
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 100.0),
                		new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 0.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK),
                new PSymbolInstance(TwoWayTCPSUL.ISYNACK,
                        new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 200.0),
                        new DataValue(TwoWayTCPSUL.DOUBLE_TYPE, 101.0)),
                new PSymbolInstance(TwoWayTCPSUL.OK));
        
        
        EnumSet<DataRelation> [] prefRel = new EnumSet [] {
        		EnumSet.of(DataRelation.EQ), EnumSet.noneOf(DataRelation.class),
        		EnumSet.noneOf(DataRelation.class), EnumSet.noneOf(DataRelation.class)
        		
        };
        
        EnumSet<DataRelation> [][] suffRel = new EnumSet [] [] {
        	{}, {EnumSet.noneOf(DataRelation.class)}, {EnumSet.noneOf(DataRelation.class), EnumSet.noneOf(DataRelation.class)},
        	{EnumSet.of(DataRelation.EQ_SUMC1), EnumSet.noneOf(DataRelation.class), EnumSet.noneOf(DataRelation.class)}
        };
        
        Word<ParameterizedSymbol> suffWord = DataWords.actsOf(suffix);
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                suffWord, prefRel, suffRel);
        
        System.out.println(symSuffix);
        
        GeneralizedSymbolicSuffix sSymSuffix = symSuffix.suffix();
        
        Word<ParameterizedSymbol> sSuffWord = suffWord.suffix(-1);
        
        EnumSet<DataRelation> [] sPrefRel = new EnumSet [] { EnumSet.noneOf(DataRelation.class),
        		EnumSet.of(DataRelation.EQ_SUMC1)};
        
        
        EnumSet<DataRelation> [][] sSuffRel = new EnumSet [] [] {
        	{}, {EnumSet.noneOf(DataRelation.class)}
        };
        
        GeneralizedSymbolicSuffix expectedSymSuffix = new GeneralizedSymbolicSuffix(sSuffWord, sPrefRel, sSuffRel);
        Assert.assertEquals(sSymSuffix, expectedSymSuffix);
       
    }
}
