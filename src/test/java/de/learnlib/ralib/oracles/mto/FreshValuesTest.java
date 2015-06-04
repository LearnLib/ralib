/*
 * Copyright (C) 2015 falk.
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
package de.learnlib.ralib.oracles.mto;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.ralib.automata.xml.*;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class FreshValuesTest {
    
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
                        "/de/learnlib/ralib/automata/xml/keygen.xml"));

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
            TypedTheory<Integer> theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);            
            teachers.put(t, theory);
        }

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);

        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts);
                
        for (Theory t : teachers.values()) {
            ((EqualityTheory)t).setFreshValues(true, ioCache);
        }
        
        DataType intType = getType("int", loader.getDataTypes());
  
        
        ParameterizedSymbol iput = new InputSymbol(
                "IPut", new DataType[] {intType});

        ParameterizedSymbol iget = new InputSymbol(
                "IGet", new DataType[] {intType});

         ParameterizedSymbol oput = new OutputSymbol(
                "OPut", new DataType[] {intType}); 
         
         ParameterizedSymbol oget = new OutputSymbol(
                "OGet", new DataType[] {intType});    
         
         ParameterizedSymbol onok = new OutputSymbol(
                "ONOK", new DataType[] {});   
         
         DataValue d0 = new DataValue(intType, 0);
         DataValue d1 = new DataValue(intType, 1);
         DataValue d2 = new DataValue(intType, 2);

        // IPut[0[int]] OPut[1[int]] IGet[2[int]] ONOK[] [p2>r1,p1>r2,p3>r3,] []  
        Word<PSymbolInstance> prefix1 = Word.fromSymbols(
                new PSymbolInstance(iput, d0),
                new PSymbolInstance(oput, d1),
                new PSymbolInstance(iget, d2),
                new PSymbolInstance(onok));

         DataValue d3 = new DataValue(intType, 3);
         DataValue d4 = new DataValue(intType, 4);
         DataValue d5 = new DataValue(intType, 5);
        
        // [s2, s4]((IGet[s1] ONOK[] IPut[s2] OPut[s3] IGet[s4] ONOK[] IPut[s5] ONOK[]))
        Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(iget, d3),
                new PSymbolInstance(onok),
                new PSymbolInstance(iput, d0),
                new PSymbolInstance(oput, d4),
                new PSymbolInstance(iget, d0),
                new PSymbolInstance(onok),
                new PSymbolInstance(iput, d5),
                new PSymbolInstance(onok));
        
        
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix1, suffix);
        
        System.out.println(prefix1);
        System.out.println(symSuffix);
        
        TreeQueryResult tqr1 = mto.treeQuery(prefix1, symSuffix);

        System.out.println(tqr1.getSdt());
        
        
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
