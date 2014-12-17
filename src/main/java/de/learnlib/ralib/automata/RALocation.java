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
    
    private final Map<ParameterizedSymbol, Collection<Transition>> out = new HashMap<>();
    
    Collection<Transition> getOut(ParameterizedSymbol ps) {
        return out.get(ps);
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
}
