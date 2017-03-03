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
package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.theory.DataRelation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author falk
 */
public class Slice {

    public static class SlicePredicate {
        private final SymbolicDataValue left;
        private final DataRelation relation;
        private final SuffixValue right;

        public SlicePredicate(SymbolicDataValue left, DataRelation relation, SuffixValue right) {
            this.left = left;
            this.relation = relation;
            this.right = right;
        }
        
        @Override
        public String toString() {
            return "(" + left + " " + relation + " " + right + ")";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SlicePredicate other = (SlicePredicate) obj;
            if (!Objects.equals(this.left, other.left)) {
                return false;
            }
            if (this.relation != other.relation) {
                return false;
            }
            if (!Objects.equals(this.right, other.right)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.left);
            hash = 71 * hash + Objects.hashCode(this.relation);
            hash = 71 * hash + Objects.hashCode(this.right);
            return hash;
        }
        
        
    }
    
    private final List<SlicePredicate> constraints;

    public Slice() {
        this.constraints = new ArrayList<>();
        
    }
    
    @Override
    public String toString() {
        List<String> s = new ArrayList<>();
        constraints.stream().forEach((g) -> {
            s.add(g.toString());
        });
        return String.join(" /\\ ", s);                
    }
            
    void addPredicate(SymbolicDataValue left, DataRelation relation, SuffixValue right ) {
        SlicePredicate p = new SlicePredicate(left, relation, right);
        if (!constraints.contains(p)) {
            constraints.add(p);
        }
    }
 
    public Set<Register> getRegisters() {
        throw new IllegalStateException("not implemented yet.");
    }
    
    public EnumSet<DataRelation> getPrefixRelationssFor(SuffixValue sv) {
        EnumSet<DataRelation> ret = EnumSet.noneOf(DataRelation.class);
        for (SlicePredicate p : constraints) {
            if (p.right.equals(sv) && p.left instanceof Register) {
                ret.add(p.relation);
            }
        }        
        return ret;
    }

    public EnumSet<DataRelation> getSuffixRelationsFor(SuffixValue left, SuffixValue right) {
        EnumSet<DataRelation> ret = EnumSet.noneOf(DataRelation.class);       
        for (SlicePredicate p : constraints) {
            if (p.left.equals(left) && p.right.equals(right)) {
                ret.add(p.relation);
            }
        }        
        return ret;
    } 
    
    public Slice suffix(int shift) {        
        Slice ret = new Slice();
        
        Map<SymbolicDataValue, Register> rMap = new HashMap<>(); 
        RegisterGenerator rgen = new RegisterGenerator();
        
        for (SlicePredicate p : constraints) {            
            if (p.right.getId() <= shift) {
                continue;
            }
            
            SuffixValue right = new SuffixValue(
                    p.right.getType(), p.right.getId() - shift);
            
            SymbolicDataValue left;
            if (p.left instanceof SuffixValue && p.left.getId() > shift) {
                left = new SuffixValue(
                    p.left.getType(), p.left.getId() - shift);
            }
            else {
                Register r = rMap.get(p.left);
                if (r == null) {
                    r = rgen.next(p.left.getType());
                    rMap.put(p.left, r);
                }
                left = r;
            }
            ret.addPredicate(left, p.relation, right);
        }
        
        return ret;
    }    
}
