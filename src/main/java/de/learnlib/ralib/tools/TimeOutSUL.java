/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.tools;

import de.learnlib.api.SULException;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public class TimeOutSUL extends DataWordSUL {

    private final DataWordSUL back;
    
    private final long timeoutMillis;
    
    public TimeOutSUL(DataWordSUL back, long timeoutMillis) {
        this.back = back;
        this.timeoutMillis = System.currentTimeMillis() + timeoutMillis;
    }

    @Override
    public void pre() {
        if (System.currentTimeMillis() > timeoutMillis) {
            throw new TimeOutException();
        }
        back.pre();
    }

    @Override
    public void post() {
        back.post();
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        return back.step(i);
    }

    @Override
    public long getResets() {
        return back.getResets(); 
    }

    @Override
    public long getInputs() {
        return back.getInputs(); 
    }

    
    
    
    
}
