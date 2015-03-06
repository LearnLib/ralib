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

package de.learnlib.ralib.automata;

import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author falk
 */
public class RALocation {
        
    private final int id;
    
    private boolean accepting;
    
    private final Map<ParameterizedSymbol, Collection<Transition>> out = new HashMap<>();

    public RALocation(int id, boolean accepting) {
        this.id = id;
        this.accepting = accepting;
    }
        
    public RALocation(int id) {
        this(id, true);
    }

    public Collection<Transition> getOut(ParameterizedSymbol ps) {
        return out.get(ps);
    }

    public Collection<Transition> getOut() {
        ArrayList<Transition> ret = new ArrayList<>();
        for (Collection<Transition> col : out.values()) {
            ret.addAll(col);
        }
        return ret;
    }
    
    void addOut(Transition t) {
        Collection<Transition> c = out.get(t.getLabel());
        if (c == null) {
            c = new ArrayList<>();
            out.put(t.getLabel(), c);
        }
        c.add(t);
    }
    
    void clear() {
        this.out.clear();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RALocation other = (RALocation) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "l" + id + " (" + (this.accepting ? "+" : "-") +")";
    }
    
    public boolean isAccepting() {
        return this.accepting;
    }
    
    public void setAccepting(boolean accepting) {
        this.accepting = accepting;
    }
}
