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

package de.learnlib.ralib.learning;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
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
public class LearnLoginTest {
    
    public LearnLoginTest() {
    }

    @Test
    public void learnLoginExample() {
        
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINEST);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINEST);
            h.setFilter(new CategoryFilter(EnumSet.of(
//                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
                    Category.EVENT, Category.PHASE, Category.MODEL)));
        }
        
        Constants consts = new Constants();
        
        RegisterAutomaton sul = AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        System.out.println("SYS:------------------------------------------------");
        System.out.println(sul);
        System.out.println("----------------------------------------------------");

        final Map<DataType, Theory> teachers = new LinkedHashMap<DataType, Theory>();
        
        teachers.put(T_UID, new IntegerEqualityTheory(T_UID));        
        teachers.put(T_PWD, new IntegerEqualityTheory(T_PWD));
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers, new Constants());
        SDTLogicOracle slo = new MultiTheorySDTLogicOracle(consts);

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, new Constants());
            }
        };
        
        RaStar rastar = new RaStar(mto, hypFactory, slo, 
                consts, I_LOGIN, I_LOGOUT, I_REGISTER);
        
        rastar.learn();        
        RegisterAutomaton hyp = rastar.getHypothesis();        
        System.out.println("HYP:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

        Word<PSymbolInstance> ce = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                        new DataValue(T_UID, 1), new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                        new DataValue(T_UID, 1), new DataValue(T_PWD, 1)));
    
        rastar.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, sul.accepts(ce)));
    
        rastar.learn();        
        hyp = rastar.getHypothesis();        
        System.out.println("HYP:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

    }
}
