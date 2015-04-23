///*
// * Copyright (C) 2014 falk.
// *
// * This library is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 2.1 of the License, or (at your option) any later version.
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this library; if not, write to the Free Software
// * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
// * MA 02110-1301  USA
// */
//
//package de.learnlib.ralib.learning;
//
//import de.learnlib.ralib.data.Constants;
//import de.learnlib.ralib.data.DataValue;
//import de.learnlib.ralib.learning.sdts.LoginExampleTreeOracle;
//import static org.testng.Assert.*;
//import org.testng.annotations.Test;
//import static de.learnlib.ralib.example.login.LoginAutomatonExample.*;
//import de.learnlib.ralib.words.PSymbolInstance;
//import net.automatalib.words.Word;
//
///**
// *
// * @author falk
// */
//public class ObservationsTest {
//    
//    public ObservationsTest() {
//    }
//
//     @Test
//     public void testLearnLoginExample() {
//     
//         LoggingOracle oracle = new LoggingOracle(new LoginExampleTreeOracle());
//         ObservationTable obs = new ObservationTable(
//                 oracle, false, I_REGISTER, I_LOGIN, I_LOGOUT);
//
//        Word<PSymbolInstance> epsPrefix = Word.epsilon();
//        obs.addPrefix(epsPrefix);
//
//        SymbolicSuffix epsSuffix = new SymbolicSuffix(epsPrefix, epsPrefix);
//        obs.addSuffix(epsSuffix);         
//         
//        while(!(obs.complete())) {};        
//        
//        System.out.println("### complete");
//  
//        final Word<PSymbolInstance> prefix = Word.fromSymbols(
//                new PSymbolInstance(I_REGISTER, 
//                    new DataValue(T_UID, 1),
//                    new DataValue(T_PWD, 1)));          
//          
//        final Word<PSymbolInstance> suffix = Word.fromSymbols(
//                new PSymbolInstance(I_LOGIN, 
//                    new DataValue(T_UID, 2),
//                    new DataValue(T_PWD, 2)));
//        
//        final SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix);  
//        
//        obs.addSuffix(symSuffix);
//        
//        while(!(obs.complete())) {};    
//        
//        AutomatonBuilder ab = new AutomatonBuilder(
//                obs.getComponents(), new Constants());
//        
//        System.out.println(ab.toRegisterAutomaton());
//     }
//}
