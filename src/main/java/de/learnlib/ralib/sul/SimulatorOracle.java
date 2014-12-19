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

package de.learnlib.ralib.sul;

import de.learnlib.api.Query;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collection;

/**
 * Uses a Register Automaton to simulate a SUL. 
 * 
 * @author falk
 */
public class SimulatorOracle implements DataWordOracle {

    private final RegisterAutomaton target;

    public SimulatorOracle(RegisterAutomaton target) {
        this.target = target;
    }
    
    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {
        for (Query<PSymbolInstance, Boolean> q : clctn) {
            boolean inLang = target.hasTrace(q.getInput());
            q.answer(inLang);
        }
    }
    
}
