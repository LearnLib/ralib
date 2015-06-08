///*
// * Copyright (C) 2014-2015 The LearnLib Contributors
// * This file is part of LearnLib, http://www.learnlib.de/.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
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
