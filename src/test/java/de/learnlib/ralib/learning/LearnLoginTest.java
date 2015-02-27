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

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.login.LoginAutomatonExample;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.testng.Assert.*;
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
    
        Constants consts = new Constants();
        
        RegisterAutomaton sul = LoginAutomatonExample.AUTOMATON;
        DataWordOracle dwOracle = new SimulatorOracle(sul);
        
        Map<DataType, Theory> teachers = new HashMap<>();
        
        teachers.put(LoginAutomatonExample.T_UID, new EqualityTheory() {
            @Override
            public DataValue getFreshValue(List vals) {
                return new DataValue(LoginAutomatonExample.T_UID, vals.size());
            }
        });
        
        teachers.put(LoginAutomatonExample.T_PWD, new EqualityTheory() {
            @Override
            public DataValue getFreshValue(List vals) {
                return new DataValue(LoginAutomatonExample.T_PWD, vals.size());
            }
        });
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, teachers);
        
        RaStar rastar = new RaStar(mto, consts, 
            LoginAutomatonExample.I_LOGIN, 
                LoginAutomatonExample.I_LOGOUT, 
                LoginAutomatonExample.I_REGISTER);
        
        rastar.learn();
        
        RegisterAutomaton hyp = rastar.getHypothesis();
        
        System.out.println(hyp);
        
    
    
    }
}
