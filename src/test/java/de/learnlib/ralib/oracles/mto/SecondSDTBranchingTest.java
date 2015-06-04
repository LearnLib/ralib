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
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.xml.*;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class SecondSDTBranchingTest {
    
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
            teachers.put(t, new IntegerEqualityTheory(t));
        }

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);

        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers, consts);
        
        DataType intType = getType("int", loader.getDataTypes());
  
        
        ParameterizedSymbol ipr = new InputSymbol(
                "IPRACK", new DataType[] {intType});

        ParameterizedSymbol inv = new InputSymbol(
                "IINVITE", new DataType[] {intType});

        ParameterizedSymbol o100 = new OutputSymbol(
                "O100", new DataType[] {intType});    

        ParameterizedSymbol o200 = new OutputSymbol(
                "O200", new DataType[] {intType});    

        ParameterizedSymbol o481 = new OutputSymbol(
                "O481", new DataType[] {intType});         

        DataValue d0 = new DataValue(intType, 0);
        DataValue d1 = new DataValue(intType, 1);

        //****** ROW:  IINVITE[0[int]] O100[0[int]] IPRACK[1[int]]
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(inv, d0),
                new PSymbolInstance(o100, d0),
                new PSymbolInstance(ipr, d1));
        
        //**** [s1]((O481[s1]))
        Word<PSymbolInstance> suffix1 =  Word.fromSymbols(
                new PSymbolInstance(o481, d0));
        SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix, suffix1);
        
        //[s1, s2, s3]((O481[s1] IPRACK[s2] O200[s3]))
        Word<PSymbolInstance> suffix2 =  Word.fromSymbols(
                new PSymbolInstance(o481, d0),
                new PSymbolInstance(ipr, d0),
                new PSymbolInstance(o200, d0));
        SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix, suffix2);
        
        System.out.println(prefix);
        System.out.println(symSuffix1);
        System.out.println(symSuffix2);
        
        TreeQueryResult tqr1 = mto.treeQuery(prefix, symSuffix1);
        TreeQueryResult tqr2 = mto.treeQuery(prefix, symSuffix2);

        RegisterGenerator rgen = new RegisterGenerator();
        Register r1 = rgen.next(intType);
        Register r2 = rgen.next(intType);
        VarMapping remap = new VarMapping();
        remap.put(r1, r2);
        remap.put(r2, r1);
        
        PIV piv = tqr2.getPiv();
        SymbolicDecisionTree sdt1 = tqr1.getSdt().relabel(remap);
        SymbolicDecisionTree sdt2 = tqr2.getSdt();
       
        System.out.println(piv);
        System.out.println(sdt1);
        System.out.println(sdt2);
     
        Branching b = mto.getInitialBranching(prefix, o100, piv, sdt1);
        
        b = mto.updateBranching(prefix, o100, b, piv, sdt1, sdt2);
   
                
        System.out.println("combined branching 1+2: ");
        for (Entry<Word<PSymbolInstance>, TransitionGuard> e : b.getBranches().entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
        
        System.out.println("The two guards shoud be x1 = y1 and x1 != y1.");
        
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
