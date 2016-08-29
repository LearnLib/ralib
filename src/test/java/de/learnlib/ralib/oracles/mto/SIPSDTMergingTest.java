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
package de.learnlib.ralib.oracles.mto;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class SIPSDTMergingTest {
    
    @Test
    public void testModelswithOutput() {
 
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINEST);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINEST);
            h.setFilter(new CategoryFilter(EnumSet.of(
                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
        }

        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});

        RegisterAutomatonImporter loader = new RegisterAutomatonImporter(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/sip.xml"));

        de.learnlib.ralib.automata.RegisterAutomaton model = loader.getRegisterAutomaton();
        System.out.println("SYS:------------------------------------------------");
        System.out.println(model);
        System.out.println("----------------------------------------------------");

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        for (final DataType t : loader.getDataTypes()) {
            teachers.put(t, new EqualityTheory() {
                @Override
                public DataValue getFreshValue(List vals) {
                    //System.out.println("GENERATING FRESH: " + vals.size());
                    return new DataValue(t, vals.size());
                }
            });
        }

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);

        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, 
                consts, new SimpleConstraintSolver());
        
        DataType intType = getType("int", loader.getDataTypes());
  
        
        ParameterizedSymbol ipr = new InputSymbol(
                "IPRACK", new DataType[] {intType});

        ParameterizedSymbol inv = new InputSymbol(
                "IINVITE", new DataType[] {intType});

         ParameterizedSymbol inil = new InputSymbol(
                "Inil", new DataType[] {}); 
         
         ParameterizedSymbol o100 = new OutputSymbol(
                "O100", new DataType[] {intType});    

        ParameterizedSymbol o486 = new OutputSymbol(
                "O486", new DataType[] {intType});    

        ParameterizedSymbol o481 = new OutputSymbol(
                "O481", new DataType[] {intType});         
         
        DataValue d0 = new DataValue(intType, 0);
        DataValue d1 = new DataValue(intType, 1);

        
        //****** ROW:  IINVITE[0[int]] O100[0[int]] IINVITE[1[int]] O100[1[int]]
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(inv, d0),
                new PSymbolInstance(o100, d0),
                new PSymbolInstance(inv, d1),
                new PSymbolInstance(o100, d1));
        
        //**** [s1, s2, s3]((Inil[] O486[s1] IPRACK[s2] O481[s3]))
        Word<PSymbolInstance> suffix =  Word.fromSymbols(
                new PSymbolInstance(inil),
                new PSymbolInstance(o486, d0),
                new PSymbolInstance(ipr, d0),
                new PSymbolInstance(o481, d0));
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        
        System.out.println(prefix);
        System.out.println(symSuffix);
        
        TreeQueryResult tqr = mto.treeQuery(prefix, symSuffix);       
        
        System.out.println(tqr.getPiv());
        
        System.out.println(tqr.getSdt());
    }

    private DataType getType(String name, Collection<DataType> dataTypes) {
        for (DataType t : dataTypes) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }        
        
}
