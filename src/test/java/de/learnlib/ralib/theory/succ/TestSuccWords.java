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
package de.learnlib.ralib.theory.succ;

import de.learnlib.ralib.data.DataType;
import static de.learnlib.ralib.theory.succ.SuccessorMinterms.*;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import java.util.Arrays;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class TestSuccWords {
    
    @Test
    public void testIntantiation() {
        
        DataType type = new DataType("int", Integer.class);
        
        SuccessorDataValue[] dvs = new SuccessorDataValue[3];
        dvs[0] = new SuccessorDataValue(type, 1, new SuccessorMinterms[] {});
        dvs[1] = new SuccessorDataValue(type, 2, new SuccessorMinterms[] {
            SUCC
        });
        dvs[2] = new SuccessorDataValue(type, 3, new SuccessorMinterms[] {
            IN_HALFSPACE, IN_WINDOW
        });
//        dvs[3] = new SuccessorDataValue(type, 1, new SuccessorMinterms[] {
//            OTHER, OTHER, OTHER
//        });
        
        
        ConstraintSolverFactory fact = new ConstraintSolverFactory();
        ConstraintSolver solver = fact.createSolver("z3");
        
        WordUtil util = new WordUtil(solver);        
        int[] result = util.instantiate(dvs);
        
        System.out.println(Arrays.toString(dvs));
        System.out.println(Arrays.toString(result));
    }
}
