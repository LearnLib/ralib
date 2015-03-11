package de.learnlib.ralib.oracles.mto;

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

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoader;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
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
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.words.Word;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class UntypedBranchingTest {
    
    public UntypedBranchingTest() {
    }


    @Test
    public void testBranching() {
    
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.ALL);
        }

        final ParameterizedSymbol ERROR
                = new ParameterizedSymbol("_io_err", new DataType[]{});

        RegisterAutomatonLoader loader = new RegisterAutomatonLoader(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/login.xml"));

        RegisterAutomaton model = loader.getRegisterAutomaton();
        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});
        
        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new HashMap<DataType, Theory>();
        for (final DataType t : loader.getDataTypes()) {
            teachers.put(t, new EqualityTheory() {
                @Override
                public DataValue getFreshValue(List vals) {
                    //System.out.println("GENERATING FRESH: " + vals.size());
                    return new DataValue(t, vals.size());
                }
            });
        }

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts, inputs);

        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioFilter, teachers);
        
        DataType intType = getType("int", loader.getDataTypes());
        
        ParameterizedSymbol reg = new ParameterizedSymbol(
                "IRegister", new DataType[] {intType, intType});

        ParameterizedSymbol log = new ParameterizedSymbol(
                "ILogin", new DataType[] {intType, intType});    
    
        ParameterizedSymbol ok = new ParameterizedSymbol(
                "OOK", new DataType[] {});    

        DataValue u = new DataValue(intType, 0);
        DataValue p = new DataValue(intType, 1);
        
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(reg, new DataValue[] {u, p}),
                new PSymbolInstance(ok, new DataValue[] {}));

        Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(log, new DataValue[] {u, p}),
                new PSymbolInstance(ok, new DataValue[] {}));        
        
        SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);
        
        System.out.println(prefix);
        System.out.println(suffix);
        System.out.println(symSuffix);
 
        System.out.println("MQ: " + ioOracle.trace(prefix.concat(suffix)));
        
        System.out.println("######################################################################");
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);        
        System.out.println(res.getSdt());
       
        PIV piv = res.getPiv();
        SymbolicDecisionTree sdt = res.getSdt();

        // --- comment this out to make it work again
        
        ParameterGenerator pgen = new ParameterGenerator();
        RegisterGenerator rgen = new RegisterGenerator();        
        
        Parameter p1 = pgen.next(intType);
        Parameter p2 = pgen.next(intType);
        Register r1 = rgen.next(intType);
        Register r2 = rgen.next(intType);
        
        VarMapping map = new VarMapping();
        map.put(r1, r2);
        map.put(r2, r1);
        
        piv = new PIV();
        piv.put(p2, r1);
        piv.put(p1, r2);
        
        sdt = sdt.relabel(map);
        
        // --- end of commenting
        
        System.out.println("######################################################################");
        Branching bug2 = mto.getInitialBranching(prefix, log, new PIV());        
        bug2 = mto.updateBranching(prefix, log, bug2, piv, sdt);        
        System.out.println(Arrays.toString(bug2.getBranches().keySet().toArray()));
        System.out.println("This set has only one word, there should be three.");
        
        System.out.println(piv);
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
