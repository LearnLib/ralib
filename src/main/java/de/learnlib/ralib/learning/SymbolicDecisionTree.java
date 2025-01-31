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
package de.learnlib.ralib.learning;


import java.util.Set;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.theory.SDTGuard;

/**
 * This interface describes the methods that are needed in a symbolic decision
 * tree during learning.
 *
 * @author falk
 */
public interface SymbolicDecisionTree {

    /**
     * checks if the tree (under renaming) is equivalent to other tree
     *
     * @param other
     * @param renaming
     * @return
     */
    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming);

    public boolean isEquivalentUnderId(SymbolicDecisionTree other, PIV piv, PIV otherPiv);

    /**
     * apply relabeling to tree and return a renamed tree.
     *
     * @param relabeling
     * @return
     */
    public SymbolicDecisionTree relabel(VarMapping relabeling);


    /**
     *
     * @return
     */
    //public Set<SymbolicDataValue.Register> getRegisters();

    /**
     * true if all paths in this tree are accepting
     *
     * @return
     */
    public boolean isAccepting();

//    public GuardExpression getGuardExpression(SuffixValue sv);
    public Set<SDTGuard> getSDTGuards(SuffixValue sv);

    public SymbolicDecisionTree copy();
}
