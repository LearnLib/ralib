/*
 * Copyright (C) 2014 falk.
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

import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.Arrays;

import net.automatalib.words.Word;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.sdts.LoginExampleTreeOracle;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public class CellTest {
    
    @Test
    public void testCellCreation() {
           
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 2),
                    new DataValue(T_PWD, 2)));           
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGOUT));
        
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)
        
        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, longsuffix);
        System.out.println("Prefix: " + prefix);
        System.out.println("Suffix: " + symSuffix);        
        
        LoggingOracle oracle = new LoggingOracle(new LoginExampleTreeOracle());
        
        Cell c = Cell.computeCell(oracle, prefix, symSuffix);
        
        System.out.println("Memorable: " + Arrays.toString(c.getMemorable().toArray()));
        
        System.out.println(c.toString());
        
        Assert.assertTrue(c.couldBeEquivalentTo(c));
        Assert.assertTrue(c.isEquivalentTo(c, new VarMapping()));
        
    }
    
}
