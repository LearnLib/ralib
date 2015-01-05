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

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarMapping;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
import de.learnlib.ralib.learning.sdts.LoginExampleTreeOracle;
import de.learnlib.ralib.trees.SymbolicSuffix;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.ArrayList;
import java.util.Arrays;
import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class RowTest {
    
    @Test
    public void testRowEquivalence() {
    
        final Word<PSymbolInstance> prefix1 = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 2),
                    new DataValue(T_PWD, 2)));
        
        final Word<PSymbolInstance> prefix2 = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 2),
                    new DataValue(T_PWD, 2)));          
    
        final Word<PSymbolInstance> suffix1 = Word.epsilon();        
        final Word<PSymbolInstance> suffix2 = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));
        
        final SymbolicSuffix symSuffix1 = new SymbolicSuffix(prefix1, suffix1);
        final SymbolicSuffix symSuffix2 = new SymbolicSuffix(prefix1, suffix2);
        
        SymbolicSuffix[] suffixes = new SymbolicSuffix[] {symSuffix1, symSuffix2};
        System.out.println("Suffixes: " + Arrays.toString(suffixes));
        
        LoggingOracle oracle = new LoggingOracle(new LoginExampleTreeOracle());
        
        Row r1 = Row.computeRow(oracle, prefix1, Arrays.asList(suffixes));
        Row r2 = Row.computeRow(oracle, prefix2, Arrays.asList(suffixes));
        
        VarMapping renaming = null;
        for (VarMapping map : new PIVRemappingIterator(r1.getParsInVars(), r2.getParsInVars())) {
            if (r1.isEquivalentTo(r2, map)) {
                renaming = map;
                break;
            }
        }

        Assert.assertNotNull(renaming);
        Assert.assertTrue(r1.couldBeEquivalentTo(r2));
        Assert.assertTrue(r1.isEquivalentTo(r2, renaming));
        
    }
    
}
